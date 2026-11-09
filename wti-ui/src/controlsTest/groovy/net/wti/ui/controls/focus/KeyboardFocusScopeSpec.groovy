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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
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
    void typedKey(int code, char character) { stage.keyDown(code); stage.keyTyped(character); stage.keyUp(code) }

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
    def 'typed Tab moves between adjacent text fields only once in either direction'() {
        given:
        Gdx.input = Stub(com.badlogic.gdx.Input) { isKeyPressed(Keys.SHIFT_LEFT) >> backwards }
        def fields = ['first', 'second', 'third'].collect { new TextField(it, skin) }
        fields.eachWithIndex { field, index -> field.setPosition(index * 200, 100); stage.addActor(field) }
        stage.keyboardFocus = fields[1]
        when:
        typedKey(Keys.TAB, '\t' as char)
        then:
        stage.keyboardFocus.is(fields[backwards ? 0 : 2])
        fields*.text == ['first', 'second', 'third']
        where:
        backwards << [false, true]
    }

    def 'typed Tab wraps inside a modal without native traversal to an outside field'() {
        given:
        Gdx.input = Stub(com.badlogic.gdx.Input) { isKeyPressed(Keys.SHIFT_LEFT) >> backwards }
        def outside = new TextField('outside', skin); outside.setPosition(200, 100); stage.addActor(outside)
        def dialog = new Window('Modal', skin); dialog.modal = true; dialog.setBounds(0, 0, 600, 200)
        def first = new TextField('first', skin); first.setPosition(100, 100)
        def last = new TextField('last', skin); last.setPosition(300, 100)
        [first, last].each { dialog.addActor(it) }
        stage.addActor(dialog)
        stage.keyboardFocus = backwards ? first : last
        when:
        typedKey(Keys.TAB, '\t' as char)
        then:
        stage.keyboardFocus.is(backwards ? last : first)
        [first.text, last.text, outside.text] == ['first', 'last', 'outside']
        where:
        backwards << [false, true]
    }

    def 'Tab capture preserves ordinary typing and TextArea newline handling'() {
        given:
        Gdx.input = Stub(com.badlogic.gdx.Input)
        def field = new TextField('', skin)
        def area = new TextArea('', skin); area.setSize(240, 100)
        [field, area, button('submit')].each { stage.addActor(it) }
        stage.keyboardFocus = field
        when:
        typedKey(Keys.X, 'x' as char)
        typedKey(Keys.SPACE, ' ' as char)
        typedKey(Keys.Y, 'y' as char)
        typedKey(Keys.TAB, '\t' as char)
        typedKey(Keys.A, 'a' as char)
        typedKey(Keys.ENTER, '\n' as char)
        typedKey(Keys.B, 'b' as char)
        then:
        field.text == 'x y'
        area.text == 'a\nb'
        stage.keyboardFocus.is(area)
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
    def 'native popup list can gain focus without treating itself as an ancestor'() {
        given:
        Gdx.input = Stub(com.badlogic.gdx.Input)
        def box = new SelectBox<String>(skin); box.setItems('Example A', 'Example B'); box.setSize(120, 48)
        stage.addActor(box)
        box.showScrollPane()
        assert box.scrollPane.actor.is(box.list)

        when: 'the native List pointer handler gives its whole content widget keyboard focus'
        stage.keyboardFocus = box.list

        then:
        stage.keyboardFocus.is(box.list)
        box.scrollPane.stage.is(stage)
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
    def 'image text focus preserves subtype and image resources and restores shared style'() {
        given:
        def original = new ImageTextButton.ImageTextButtonStyle(skin.get(TextButton.TextButtonStyle))
        original.imageUp = new BaseDrawable(); original.imageDown = new BaseDrawable()
        original.imageOver = new BaseDrawable(); original.imageChecked = new BaseDrawable()
        def target = new ImageTextButton('image', original)
        stage.addActor(target)
        when:
        key(Keys.TAB)
        then:
        stage.keyboardFocus.is(target)
        target.style instanceof ImageTextButton.ImageTextButtonStyle
        !target.style.is(original)
        target.style.imageUp.is(original.imageUp)
        target.style.imageDown.is(original.imageDown)
        target.style.imageOver.is(original.imageOver)
        target.style.imageChecked.is(original.imageChecked)
        when:
        stage.keyboardFocus = null
        then:
        target.style.is(original)
        when:
        stage.keyboardFocus = target; scope.dispose()
        then:
        target.style.is(original)
        actions.empty
    }
    def 'custom button style survives traversal and pointer focus without slicing its subtype'() {
        given:
        def first = button('first'); def last = button('last')
        def firstStyle = first.style; def lastStyle = last.style
        def original = new MenuStyle(skin.get(TextButton.TextButtonStyle))
        def target = new TextButton('menu', original) {
            @Override void setStyle(Button.ButtonStyle value) {
                if (!(value instanceof MenuStyle)) throw new IllegalArgumentException('MenuStyle required')
                super.setStyle(value)
            }
        }
        def menuResource = original.menuResource
        [first, target, last].each { stage.addActor(it) }
        when:
        key(Keys.TAB); key(Keys.TAB)
        then:
        stage.keyboardFocus.is(target)
        target.style.is(original)
        target.style.menuResource.is(menuResource)
        first.style.is(firstStyle)
        when:
        key(Keys.TAB)
        def event = new InputEvent(); event.type = InputEvent.Type.touchDown; event.stage = stage; event.button = 0
        target.label.fire(event)
        then:
        stage.keyboardFocus.is(target)
        target.style.is(original)
        last.style.is(lastStyle)
        actions.empty
        when:
        scope.dispose()
        then:
        target.style.is(original)
    }

    def 'checked button focus draws checked hover state when available and restores its shared style'() {
        given:
        def original = new Button.ButtonStyle()
        original.up = new BaseDrawable(); original.over = new BaseDrawable()
        original.checked = new BaseDrawable()
        original.checkedOver = hoverAvailable ? new BaseDrawable() : null
        def checkedBackground = original.checked
        def target = new Button(original); target.checked = true
        target.addListener(new ChangeListener() {
            @Override void changed(ChangeEvent event, Actor actor) { actions << 'switch' }
        })
        stage.addActor(target)
        when:
        key(Keys.TAB)
        target.draw(stage.batch, 1f)
        then:
        stage.keyboardFocus.is(target)
        !target.style.is(original)
        target.background.is(hoverAvailable ? original.checkedOver : checkedBackground)
        original.checked.is(checkedBackground)
        target.checked
        actions.empty
        when:
        stage.keyboardFocus = null
        target.draw(stage.batch, 1f)
        then:
        target.style.is(original)
        target.background.is(checkedBackground)
        target.checked
        actions.empty
        where:
        hoverAvailable << [true, false]
    }

    private static class MenuStyle extends TextButton.TextButtonStyle {
        final Object menuResource = new Object()
        MenuStyle(TextButton.TextButtonStyle original) { super(original) }
    }

    def 'keyboard release reaches registered click and change listeners exactly once with actor context'() {
        given:
        def target = button('target'); target.setSize(100,48)
        def clicks = []; def right = []
        def listener = new ClickListener() {
            @Override void clicked(InputEvent e, float x, float y) { clicks << [e.target, e.listenerActor, e.stage, e.type, x, y] }
        }
        target.addListener(listener)
        target.addListener(new ClickListener(com.badlogic.gdx.Input.Buttons.RIGHT) {
            @Override void clicked(InputEvent e, float x, float y) { right << true }
        })
        stage.addActor(target); stage.keyboardFocus = target
        when:
        4.times { stage.keyDown(code) }; stage.keyUp(code)
        then:
        actions == ['target']
        clicks.size() == 1
        clicks[0] == [target,target,stage,InputEvent.Type.touchUp,50f,24f]
        right.empty
        !listener.pressed
        listener.pressedPointer == -1
        !target.clickListener.pressed
        target.clickListener.pressedPointer == -1
        where:
        code << [Keys.ENTER, Keys.SPACE, Keys.NUMPAD_ENTER]
    }
    def 'held activation cannot rearm after focus interruption until physical release'() {
        given:
        def first = button('first'); def second = button('second')
        stage.addActor(first); stage.addActor(second); stage.keyboardFocus = first
        when:
        stage.keyDown(Keys.SPACE); stage.keyboardFocus = second
        if (back) stage.keyboardFocus = first
        repeats.times { stage.keyDown(Keys.SPACE) }; stage.keyUp(Keys.SPACE)
        then:
        actions.empty
        when:
        key(Keys.SPACE)
        then:
        actions == [back ? 'first' : 'second']
        where:
        back  | repeats
        true  | 0
        true  | 4
        false | 4
    }
    def 'capture click cancellation prevents activation without leaving a press'() {
        given:
        def target = button('target'); stage.addActor(target); stage.keyboardFocus = target
        def cancellation = new ClickListener() {
            @Override boolean touchDown(InputEvent e,float x,float y,int pointer,int button) {
                super.touchDown(e,x,y,pointer,button); e.cancel(); true
            }
        }
        target.addCaptureListener(cancellation)
        when:
        key(Keys.SPACE)
        then:
        actions.empty
        !target.checked
        !cancellation.pressed
        cancellation.pressedPointer == -1
    }

    def 'packaged verification uses the requested complete owning JAR'() {
        expect:
        !System.getProperty('focus.owner.jar') ||
                new File(KeyboardFocusScope.protectionDomain.codeSource.location.toURI()).canonicalFile ==
                new File(System.getProperty('focus.owner.jar')).canonicalFile
    }

    def 'capture release cancellation suppresses regular click action and clears the pending button press'() {
        given:
        def target = button('target'); stage.addActor(target); stage.keyboardFocus = target
        def clicks = []
        target.addCaptureListener(new ClickListener() {
            @Override void clicked(InputEvent e,float x,float y) { clicks << true; e.cancel() }
        })
        when:
        key(Keys.ENTER)
        then:
        clicks == [true]
        actions.empty
        !target.checked
        target.clickListener.pressedPointer == -1
        !target.clickListener.pressed
    }

}
