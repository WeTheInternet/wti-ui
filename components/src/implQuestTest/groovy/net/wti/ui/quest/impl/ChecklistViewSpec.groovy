package net.wti.ui.quest.impl

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import net.wti.quest.api.ChecklistSnapshot
import net.wti.quest.api.QuestStatus
import net.wti.ui.quest.api.ChecklistActionHandler
import spock.lang.Specification

/// Pure Scene2D tree/event tests; no backend, window, device or texture upload.
class ChecklistViewSpec extends Specification {
    Skin skin
    List<String> intents = []
    ChecklistView view

    def setup() {
        skin = new Skin()
        def font = new BitmapFont(new BitmapFont.BitmapFontData(), new TextureRegion(), false)
        // Real text advances/wrapping without loading a texture or creating a GL context.
        font.data.lineHeight = 18f
        font.data.capHeight = 14f
        font.data.xHeight = 10f
        font.data.ascent = 0f
        font.data.descent = -4f
        font.data.down = -18f
        font.data.spaceXadvance = 8f
        (32..126).each { code ->
            def glyph = new BitmapFont.Glyph()
            glyph.id = code
            glyph.xadvance = 8
            glyph.width = 8
            glyph.height = 14
            font.data.setGlyph(code, glyph)
        }
        skin.add('default', new Label.LabelStyle(font, null))
        skin.add('default', new TextButton.TextButtonStyle(null, null, null, font))
        view = new ChecklistView(skin, new ChecklistActionHandler() {
            void startRequested() { intents << 'start' }
            void completeRequested(String key) { intents << key }
        })
    }

    def 'membership browse renders quantity and does not expose completion or start'() {
        given:
        view.snapshot = snapshot(false, false, false)
        when:
        view.refresh()
        then:
        labels(view).contains('Snack × 2')
        buttons(view).empty
        intents.empty
    }

    def 'initial pending and failure states render without a prior snapshot'() {
        when:
        view.pending = true
        view.refresh()
        then:
        labels(view) == ['Loading…']
        when:
        view.pending = false
        view.error = 'Could not load. Try again.'
        view.refresh()
        then:
        labels(view) == ['Could not load. Try again.']
        buttons(view).empty
    }

    def 'known ineligible start states retain missing rows without impossible start actions'() {
        given:
        def root = new ChecklistSnapshot.Entry('definition', 'encoded-root', 'Packing', null, 1,
                finished ? QuestStatus.FINISHED : null, finished, false)
        def missing = new ChecklistSnapshot.Entry('child-definition', 'encoded-child', 'Snack', null,
                2, null, false, false)
        view.snapshot = new ChecklistSnapshot(root, [missing], false)
        when:
        view.refresh()
        then:
        labels(view).containsAll(['Snack × 2', 'Not started'])
        buttons(view).empty
        labels(view).contains('Checklist complete') == finished
        where:
        finished << [false, true]
    }

    def 'starting and completing are distinct intents with pending double-click suppression'() {
        given:
        view.snapshot = snapshot(false, true, false)
        view.refresh()
        when:
        buttons(view)[0].fire(new ChangeListener.ChangeEvent())
        buttons(view)[0].fire(new ChangeListener.ChangeEvent())
        then:
        intents == ['start']
        when:
        view.snapshot = snapshot(true, false, false)
        view.pending = false
        view.refresh()
        buttons(view).find { it.text.toString() == 'Complete' }.fire(new ChangeListener.ChangeEvent())
        then:
        intents == ['start', 'encoded-child']
        labels(view).contains('Snack × 2')
    }

    def 'failed save retains membership and only replacement acknowledged snapshot shows done'() {
        given:
        view.snapshot = snapshot(true, false, false)
        view.refresh()
        buttons(view)[0].fire(new ChangeListener.ChangeEvent())
        when:
        view.pending = false
        view.error = 'Could not save. Try again.'
        view.refresh()
        then:
        labels(view).containsAll(['Snack × 2', 'Could not save. Try again.'])
        !labels(view).contains('Done')
        buttons(view).any { it.text.toString() == 'Complete' }
        when:
        view.snapshot = snapshot(true, false, true)
        view.error = null
        view.refresh()
        then:
        labels(view).contains('Done')
        !buttons(view).any { it.text.toString() == 'Complete' }
    }

    def 'laid out actions have at least 48 units of hittable width and height'() {
        given:
        def root = new ChecklistSnapshot.Entry('definition', 'encoded-root', 'Packing', null, 1,
                rootStarted ? QuestStatus.ACTIVE : null, rootStarted, rootEligible)
        def child = new ChecklistSnapshot.Entry('child-definition', 'encoded-child', 'Snack', null, 2,
                childEligible ? QuestStatus.ACTIVE : null, childEligible, childEligible)
        view.snapshot = new ChecklistSnapshot(root, [child], canStart)
        view.refresh()

        when: 'Scene2D lays out the view in a compact logical viewport'
        view.setSize(320f, 600f)
        view.validate()
        def button = buttons(view).find { it.text.toString() == actionText }

        then: 'actual bounds, rather than minimum-size configuration, contain the touch target'
        button != null
        button.width >= 48f
        button.height >= 48f
        [[1f, 1f], [button.width - 1f, 1f], [1f, button.height - 1f],
         [button.width - 1f, button.height - 1f]].every { corner ->
            def point = button.localToAscendantCoordinates(view, new Vector2(corner[0] as float, corner[1] as float))
            def hit = view.hit(point.x, point.y, true)
            hit != null && (hit.is(button) || hit.isDescendantOf(button))
        }

        where:
        actionText           | rootStarted | rootEligible | childEligible | canStart
        'Start checklist'    | false       | false        | false         | true
        'Finish starting'    | true        | false        | false         | true
        'Complete'           | true        | false        | true          | false
        'Complete checklist' | true        | true         | false         | false
    }

    def 'long root and member titles wrap inside a 320-unit view while action stays hittable'() {
        given:
        String rootText = 'Get ready for a short outing with everything you need'
        String childText = 'Choose a snack and remember to pack the water bottle for the whole afternoon'
        def root = new ChecklistSnapshot.Entry('definition', 'encoded-root', rootText, null, 1,
                QuestStatus.ACTIVE, true, false)
        def child = new ChecklistSnapshot.Entry('child-definition', 'encoded-child', childText, null, 1,
                QuestStatus.ACTIVE, true, true)
        view.snapshot = new ChecklistSnapshot(root, [child], false)
        view.refresh()

        when:
        view.setSize(320f, 600f)
        view.validate()
        def rootLabel = actors(view).find { it instanceof Label && it.text.toString() == rootText }
        def childLabel = actors(view).find { it instanceof Label && it.text.toString() == childText }
        def button = buttons(view).find { it.text.toString() == 'Complete' }

        then: 'both titles really wrap rather than only declaring wrapping'
        rootLabel.glyphLayout.runs.size > 1
        childLabel.glyphLayout.runs.size > 1
        rootLabel.glyphLayout.width <= rootLabel.width + 0.01f
        childLabel.glyphLayout.width <= childLabel.width + 0.01f
        [rootLabel, childLabel, button].every { actor ->
            def origin = actor.localToAscendantCoordinates(view, new Vector2(0f, 0f))
            origin.x >= -0.01f && origin.x + actor.width <= 320.01f
        }

        and: 'text and touch target do not overlap, and the action still receives edge taps'
        def labelRight = childLabel.localToAscendantCoordinates(view, new Vector2(childLabel.width, 0f)).x
        def buttonLeft = button.localToAscendantCoordinates(view, new Vector2(0f, 0f)).x
        labelRight + 11.99f <= buttonLeft
        button.width >= 48f && button.height >= 48f
        [[1f, 1f], [button.width - 1f, button.height - 1f]].every { corner ->
            def point = button.localToAscendantCoordinates(view, new Vector2(corner[0] as float, corner[1] as float))
            def hit = view.hit(point.x, point.y, true)
            hit != null && (hit.is(button) || hit.isDescendantOf(button))
        }
    }

    static ChecklistSnapshot snapshot(boolean started, boolean canStart, boolean complete) {
        def root = new ChecklistSnapshot.Entry('definition', started ? 'encoded-root' : null,
                'Packing', null, 1, started ? QuestStatus.ACTIVE : null, started, false)
        def child = new ChecklistSnapshot.Entry('child-definition', started ? 'encoded-child' : null,
                'Snack', null, 2, complete ? QuestStatus.FINISHED : started ? QuestStatus.ACTIVE : null,
                started, started && !complete)
        new ChecklistSnapshot(root, [child], canStart)
    }

    static List<TextButton> buttons(Group group) {
        List<TextButton> found = []
        group.children.each {
            if (it instanceof TextButton) found << it
            else if (it instanceof Group) found.addAll(buttons(it))
        }
        found
    }

    static List<String> labels(Group group) {
        List<String> found = []
        group.children.each {
            if (it instanceof Label) found << it.text.toString()
            else if (it instanceof Group) found.addAll(labels(it))
        }
        found
    }

    static List actors(Group group) {
        List found = []
        group.children.each {
            found << it
            if (it instanceof Group) found.addAll(actors(it))
        }
        found
    }
}
