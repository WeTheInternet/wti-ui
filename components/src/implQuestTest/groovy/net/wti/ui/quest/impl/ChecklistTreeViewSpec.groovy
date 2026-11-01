package net.wti.ui.quest.impl

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import net.wti.quest.api.ChecklistSnapshot
import net.wti.quest.api.QuestStatus
import spock.lang.Specification

class ChecklistTreeViewSpec extends Specification {
    com.badlogic.gdx.Application previousApp
    Skin skin
    java.util.List<String> intents = []
    ChecklistTreeView view
    def setup() {
        previousApp = com.badlogic.gdx.Gdx.app
        com.badlogic.gdx.Gdx.app = Stub(com.badlogic.gdx.Application) {
            getType() >> com.badlogic.gdx.Application.ApplicationType.HeadlessDesktop
        }
        skin = new Skin()
        def font = new BitmapFont(new BitmapFont.BitmapFontData(), new TextureRegion(), false)
        font.data.lineHeight = 18f; font.data.capHeight = 14f; font.data.down = -18f; font.data.spaceXadvance = 8f
        (32..126).each { code ->
            def glyph = new BitmapFont.Glyph(); glyph.id = code; glyph.xadvance = 8; glyph.width = 8; glyph.height = 14
            font.data.setGlyph(code, glyph)
        }
        skin.add('default', new Label.LabelStyle(font, null))
        skin.add('default', new TextButton.TextButtonStyle(null, null, null, font))
        skin.add('default', new CheckBox.CheckBoxStyle(null, null, font, null))
        skin.add('default', new ScrollPane.ScrollPaneStyle())
        view = new ChecklistTreeView(skin, { key -> intents << key })
    }
    def cleanup() { com.badlogic.gdx.Gdx.app = previousApp }
    static ChecklistSnapshot snapshot(String id, boolean complete = false, boolean writable = true) {
        def root = new ChecklistSnapshot.Entry(id, writable ? id + '-live' : null, 'Goal ' + id, null, 1, null, false, false)
        def child = new ChecklistSnapshot.Entry(id + '-child', writable ? id + '-task' : null,
                'Brush your teeth and put everything away ready for the next person', null, 1,
                complete ? QuestStatus.FINISHED : null, complete, false)
        new ChecklistSnapshot(root, [child], writable)
    }
    static java.util.List actors(Group group) { group.children.toList().collectMany { [it] + (it instanceof Group ? actors(it) : []) } }
    CheckBox checkbox(String key) { actors(view).find { it instanceof CheckBox && it.name == 'task:' + key + '-child' } }

    def 'configured slots retain failed first root ahead of later arrivals and a successful retry'() {
        when:
        view.setGoalOrder(['morning', 'outing'])
        def morningCard = view.children[0]
        def outingCard = view.children[1]
        view.setLoading(true)
        view.setError('morning', 'Could not load. Refresh to retry.')
        view.setGoal('outing', snapshot('outing'))
        view.setLoading(false)
        actors(view).find { it.name == 'goal:outing' }.fire(new ChangeListener.ChangeEvent())
        then:
        view.goalOrder == ['morning', 'outing']
        view.hasGoal('morning')
        !view.isExpanded('outing')
        intents.empty
        when:
        view.setLoading(true); view.setGoalOrder(['morning', 'outing'])
        view.setGoal('morning', snapshot('morning')); view.setError('morning', null)
        view.setGoal('outing', snapshot('outing')); view.setLoading(false)
        then:
        view.goalOrder == ['morning', 'outing']
        view.children[0].is(morningCard) && view.children[1].is(outingCard)
        !view.isExpanded('outing')
        intents.empty
    }
    def 'reapplying configured order preserves actors scroll and pending state'() {
        given:
        def order = (0..15).collect { 'g' + it }
        view.setGoalOrder(order); order.each { view.setGoal(it, snapshot(it)) }
        def scroll = new ScrollPane(view, skin); scroll.setScrollingDisabled(true, false)
        scroll.setSize(320, 300); scroll.validate(); scroll.setScrollY(200)
        def before = scroll.getScrollY(); def card = view.children[8]
        when:
        view.setPending('g8', true); view.setGoalOrder(order); scroll.validate()
        then:
        view.children[8].is(card)
        checkbox('g8').disabled
        scroll.getScrollY() == before
        intents.empty
    }
    def 'all goals begin expanded; toggling is view state and never a completion intent'() {
        when:
        view.setGoal('a', snapshot('a')); view.setGoal('b', snapshot('b'))
        def toggle = actors(view).find { it.name == 'goal:a' }
        toggle.fire(new ChangeListener.ChangeEvent())
        then:
        !view.isExpanded('a') && view.isExpanded('b')
        intents.empty
        when:
        view.setPending('a', true); view.setGoal('a', snapshot('a', true)); view.setPending('a', false)
        then:
        !view.isExpanded('a') && view.isExpanded('b')
        intents.empty
    }
    def 'checkbox dispatches one explicit intent and waits for acknowledgment without collapsing'() {
        given:
        view.setGoal('a', snapshot('a'))
        when:
        checkbox('a').fire(new ChangeListener.ChangeEvent())
        then:
        intents == ['a-task']
        !checkbox('a').checked
        checkbox('a').disabled
        view.isExpanded('a')
        when:
        view.setGoal('a', snapshot('a', true)); view.setPending('a', false)
        then:
        checkbox('a').checked && checkbox('a').disabled
        view.isExpanded('a')
    }
    def 'first visible goal cannot accept a check until remaining roots finish loading'() {
        when:
        view.setLoading(true); view.setGoal('a', snapshot('a'))
        checkbox('a').fire(new ChangeListener.ChangeEvent())
        then:
        checkbox('a').disabled
        !checkbox('a').checked
        intents.empty
        when:
        view.setGoal('b', snapshot('b')); view.setLoading(false)
        checkbox('a').fire(new ChangeListener.ChangeEvent())
        then:
        intents == ['a-task']
    }
    def 'read only checkbox cannot dispatch writes even when a change event is delivered'() {
        given:
        view.setGoal('a', snapshot('a', false, false))
        when:
        checkbox('a').fire(new ChangeListener.ChangeEvent())
        then:
        checkbox('a').disabled
        intents.empty
    }
    def 'reopened completed children expose an acknowledged root reconciliation action'() {
        given:
        def previous = snapshot('a', true)
        def root = new ChecklistSnapshot.Entry('a', 'a-live', 'Goal a', null, 1, QuestStatus.ACTIVE, true, true)
        view.setGoal('a', new ChecklistSnapshot(root, previous.children, false))
        when:
        actors(view).find { it instanceof TextButton && it.text.toString() == 'Finish goal' }.fire(new ChangeListener.ChangeEvent())
        then:
        intents == ['a-live']
        checkbox('a').checked
    }
    def 'long task titles wrap inside a narrow view with a full 48-unit touch target'() {
        given:
        view.setGoal('a', snapshot('a')); view.setSize(288, 600)
        when:
        view.validate()
        def box = checkbox('a')
        then:
        box.width >= 48 && box.height >= 48
        box.width <= 288
        box.label.height > 18
    }
    def 'actual keyboard task activation preserves programmatic suppression and waits for acknowledgment'() {
        given:
        def previousGraphics = com.badlogic.gdx.Gdx.graphics
        def previousInput = com.badlogic.gdx.Gdx.input
        com.badlogic.gdx.Gdx.graphics = Stub(com.badlogic.gdx.Graphics) { getWidth() >> 320; getHeight() >> 600 }
        com.badlogic.gdx.Gdx.input = null
        def viewport = new com.badlogic.gdx.utils.viewport.ScreenViewport() {
            @Override void update(int width, int height, boolean center) { setWorldSize(width,height); setScreenBounds(0,0,width,height) }
        }
        def stage = new com.badlogic.gdx.scenes.scene2d.Stage(viewport, Stub(com.badlogic.gdx.graphics.g2d.Batch))
        def scope = new net.wti.ui.controls.focus.KeyboardFocusScope(stage)
        view.setGoal('a', snapshot('a')); view.setGoal('b', snapshot('b')); stage.addActor(view)
        when:
        stage.keyDown(com.badlogic.gdx.Input.Keys.TAB); stage.keyUp(com.badlogic.gdx.Input.Keys.TAB)
        stage.keyDown(com.badlogic.gdx.Input.Keys.TAB); stage.keyUp(com.badlogic.gdx.Input.Keys.TAB)
        then:
        stage.keyboardFocus.is(checkbox('a'))
        when:
        3.times { stage.keyDown(keycode) }
        stage.keyUp(keycode)
        then:
        intents == ['a-task']
        !checkbox('a').checked
        checkbox('a').disabled
        stage.keyboardFocus.is(checkbox('a'))
        view.isExpanded('a')
        when:
        view.setGoal('a', snapshot('a',true)); view.setPending('a',false)
        stage.keyDown(keycode); stage.keyUp(keycode)
        then:
        checkbox('a').checked
        intents == ['a-task']
        stage.keyboardFocus.is(checkbox('a'))
        when:
        stage.keyDown(com.badlogic.gdx.Input.Keys.TAB); stage.keyUp(com.badlogic.gdx.Input.Keys.TAB)
        then:
        stage.keyboardFocus.name == 'goal:b'
        cleanup:
        scope.dispose(); stage.dispose()
        com.badlogic.gdx.Gdx.graphics = previousGraphics; com.badlogic.gdx.Gdx.input = previousInput
        where:
        keycode << [com.badlogic.gdx.Input.Keys.ENTER, com.badlogic.gdx.Input.Keys.SPACE]
    }
    def 'pending and acknowledgment preserve scroll offset and expansion through layout'() {
        given:
        (0..15).each { i -> view.setGoal('g' + i, snapshot('g' + i)) }
        def scroll = new ScrollPane(view, skin); scroll.setScrollingDisabled(true, false)
        scroll.setSize(320, 300); scroll.validate(); scroll.scrollY = 200
        def before = scroll.getScrollY()
        when:
        view.setPending('g8', true); scroll.validate()
        view.setGoal('g8', snapshot('g8', true)); view.setPending('g8', false); scroll.validate()
        then:
        scroll.getScrollY() == before
        view.isExpanded('g8')
    }
}
