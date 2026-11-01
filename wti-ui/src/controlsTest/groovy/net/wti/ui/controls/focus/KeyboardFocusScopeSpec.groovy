package net.wti.ui.controls.focus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Application
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import spock.lang.Specification

class KeyboardFocusScopeSpec extends Specification {
    def previousApp, previousGraphics, previousInput
    Stage stage
    Skin skin
    KeyboardFocusScope scope
    java.util.List<String> actions = []

    def setup() {
        previousApp = Gdx.app; previousGraphics = Gdx.graphics; previousInput = Gdx.input
        Gdx.app = Stub(Application) { getType() >> Application.ApplicationType.HeadlessDesktop }
        Gdx.graphics = Stub(Graphics) { getWidth() >> 800; getHeight() >> 600 }
        Gdx.input = null
        def viewport = new ScreenViewport() {
            @Override void update(int width, int height, boolean center) { setWorldSize(width, height); setScreenBounds(0, 0, width, height) }
        }
        stage = new Stage(viewport, Stub(Batch))
        skin = new Skin()
        def data = new BitmapFont.BitmapFontData(); data.lineHeight = 18; data.capHeight = 14; data.spaceXadvance = 8
        (32..126).each { code ->
            def glyph = new BitmapFont.Glyph(); glyph.id = code; glyph.xadvance = 8; glyph.width = 8; glyph.height = 14
            data.setGlyph(code, glyph)
        }
        def font = new BitmapFont(data, new TextureRegion(), false) {
            @Override protected void load(BitmapFont.BitmapFontData ignored) {}
        }
        skin.add('default-font', font)
        skin.add('default', new Label.LabelStyle(font, Color.WHITE))
        def button = new TextButton.TextButtonStyle(new BaseDrawable(), null, null, font)
        button.over = new BaseDrawable(); button.fontColor = Color.BLUE; button.overFontColor = Color.WHITE
        skin.add('default', button)
        skin.add('default', new CheckBox.CheckBoxStyle(null, null, font, Color.BLUE))
        def scroll = new ScrollPane.ScrollPaneStyle(); skin.add('default', scroll)
        def list = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(font, Color.WHITE, Color.WHITE, new BaseDrawable())
        skin.add('default', list)
        skin.add('default', new SelectBox.SelectBoxStyle(font, Color.WHITE, new BaseDrawable(), scroll, list))
        skin.add('default', new TextField.TextFieldStyle(font, Color.WHITE, new BaseDrawable(), null, new BaseDrawable()))
        skin.add('default', new Window.WindowStyle(font, Color.WHITE, null))
        scope = new KeyboardFocusScope(stage)
    }
    def cleanup() { scope.dispose(); stage.dispose(); skin.dispose(); Gdx.app = previousApp; Gdx.graphics = previousGraphics; Gdx.input = previousInput }
    TextButton button(String name) {
        def value = new TextButton(name, skin); value.name = name
        value.addListener(new ChangeListener() { @Override void changed(ChangeEvent event, Actor actor) { actions << name } })
        value
    }
    void key(int code) { stage.keyDown(code); stage.keyUp(code) }

    def 'Tab and Shift Tab wrap in reading order and skip disabled hidden controls without activation'() {
        given:
        def first = button('first'); def disabled = button('disabled'); def hidden = button('hidden'); def last = new CheckBox('last', skin)
        disabled.disabled = true
        def group = new Group(); group.addActor(hidden); group.visible = false
        [first, disabled, group, last].each { stage.addActor(it) }
        def original = first.style
        when:
        key(Keys.TAB)
        then:
        stage.keyboardFocus.is(first)
        first.style.up.is(original.over)
        !first.style.is(original)
        skin.get(TextButton.TextButtonStyle).is(original)
        when:
        key(Keys.TAB)
        then:
        stage.keyboardFocus.is(last)
        first.style.is(original)
        when:
        stage.keyDown(Keys.SHIFT_LEFT); key(Keys.TAB); stage.keyUp(Keys.SHIFT_LEFT)
        then:
        stage.keyboardFocus.is(first)
        actions.empty
    }
    def 'Enter and Space activate once on release and key repeat does not duplicate actions'() {
        given:
        def target = button('target'); stage.addActor(target); stage.keyboardFocus = target
        when:
        5.times { stage.keyDown(keycode) }
        then:
        actions.empty
        when:
        stage.keyUp(keycode)
        then:
        actions == ['target']
        when:
        target.disabled = true; key(keycode)
        then:
        actions == ['target']
        where:
        keycode << [Keys.ENTER, Keys.SPACE, Keys.NUMPAD_ENTER]
    }
    def 'focus or hierarchy change before release cancels activation instead of redirecting it'() {
        given:
        def first = button('first'); def second = button('second'); stage.addActor(first); stage.addActor(second)
        stage.keyboardFocus = first
        when:
        stage.keyDown(Keys.SPACE); stage.keyboardFocus = second; stage.keyUp(Keys.SPACE)
        stage.keyDown(Keys.ENTER); second.remove(); stage.addActor(button('second')); stage.keyUp(Keys.ENTER)
        then:
        actions.empty
    }
    def 'pointer down gives keyboard focus but never activates the target'() {
        given:
        def target = button('target'); stage.addActor(target)
        def event = new InputEvent(); event.type = InputEvent.Type.touchDown; event.stage = stage; event.button = 0
        when:
        target.label.fire(event)
        then:
        stage.keyboardFocus.is(target)
        actions.empty
    }
    def 'top modal traps traversal and removal returns access to the board'() {
        given:
        def board = button('board'); stage.addActor(board)
        def dialog = new Window('Modal', skin); dialog.modal = true
        def field = new TextField('', skin); def apply = button('apply')
        dialog.add(field); dialog.add(apply); stage.addActor(dialog)
        when:
        key(Keys.TAB)
        then:
        stage.keyboardFocus.is(field)
        when:
        key(Keys.TAB)
        then:
        stage.keyboardFocus.is(apply)
        when:
        key(Keys.TAB)
        then:
        stage.keyboardFocus.is(field)
        when:
        dialog.remove(); key(Keys.TAB)
        then:
        stage.keyboardFocus.is(board)
        actions.empty
    }
    def 'SelectBox uses native Enter navigation and Tab closes popup before leaving'() {
        given:
        Gdx.input = Stub(com.badlogic.gdx.Input)
        def box = new SelectBox<String>(skin); box.setItems('Alex', 'Sam'); box.setSize(120,48)
        def next = button('next'); stage.addActor(box); stage.addActor(next); stage.keyboardFocus = box
        when:
        key(Keys.ENTER)
        then:
        box.list.stage.is(stage)
        when:
        key(Keys.DOWN); key(Keys.ENTER); 3.times { stage.act(0.5f) }
        then:
        box.selected == 'Sam'
        box.scrollPane.stage == null
        when:
        key(Keys.ENTER); key(Keys.TAB); 3.times { stage.act(0.5f) }
        then:
        box.scrollPane.stage == null
        stage.keyboardFocus.is(next)
        actions.empty
    }
    def 'focus traversal scrolls the focused button into view'() {
        given:
        def content = new Table(); (0..11).each { content.add(button('b'+it)).height(48).width(220).row() }
        def pane = new ScrollPane(content, skin); pane.setScrollingDisabled(true,false); pane.setSize(240,160); stage.addActor(pane); pane.validate()
        when:
        12.times { key(Keys.TAB) }
        then:
        stage.keyboardFocus.name == 'b11'
        pane.getScrollY() > 0
        actions.empty
    }
    def 'dispose restores shared styles and removes the stage listeners'() {
        given:
        def target = button('target'); stage.addActor(target); def original = target.style
        stage.keyboardFocus = target
        when:
        scope.dispose(); scope.dispose()
        then:
        target.style.is(original)
        !stage.keyDown(Keys.TAB)
        actions.empty
    }
}
