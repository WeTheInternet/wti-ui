package net.wti.ui.view.responsive

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import spock.lang.Shared
import spock.lang.Specification

class FloatingPanelSpec extends Specification {

    @Shared HeadlessApplication app
    @Shared BitmapFont font

    def setupSpec() {
        app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration())
        Gdx.graphics = new MockGraphics()
        final GL20 gl = Mock(GL20)
        gl.glGenTexture() >> 1
        gl.glCreateShader(_ as int) >> 1
        gl.glCreateProgram() >> 1
        Gdx.gl = gl
        Gdx.gl20 = gl
        font = new BitmapFont()
    }

    def cleanupSpec() {
        font?.dispose()
        app?.exit()
    }

    def "table layout owns title and content viewport bounds"() {
        given:
        def panel = panelWithContent(new Actor())
        panel.setBounds(10f, 20f, 240f, 180f)

        when:
        validateTree(panel)

        then:
        panel.titleActor.parent.is(panel)
        panel.viewport.parent.is(panel)
        panel.titleActor.x >= panel.floatingStyle.edgePadding
        panel.viewport.x >= panel.floatingStyle.edgePadding
        panel.viewport.y < panel.titleActor.y
        panel.viewport.width > 0f
        panel.viewport.height > 0f
    }

    def "viewport clips and scrolls generic content"() {
        given:
        def content = new Group()
        content.setSize(100f, 600f)
        def panel = panelWithContent(content)
        panel.setBounds(0f, 0f, 240f, 160f)

        when:
        validateTree(panel)

        then:
        !panel.viewport.scrollingDisabledY
        panel.viewport.actor.is(content)
        content.parent.is(panel.viewport)
        content.height > panel.viewport.height
    }

    def "generic content scroll pane retains actor bounds and assigns hover scroll focus"() {
        given:
        def content = new Group()
        content.setSize(120f, 600f)
        def pane = new ResponsiveContentScrollPane(content, new ScrollPane.ScrollPaneStyle())
        def stage = new Stage(new ScreenViewport(), Mock(Batch))
        pane.setBounds(17f, 23f, 220f, 140f)
        stage.addActor(pane)

        when:
        validateTree(pane)
        pane.listeners.findAll { it instanceof InputListener }.each { listener ->
            (listener as InputListener).enter(new InputEvent(), 1f, 1f, -1, null)
        }

        then:
        pane.content.is(content)
        pane.actor.is(content)
        content.parent.is(pane)
        pane.x == 17f
        pane.y == 23f
        pane.width == 220f
        pane.height == 140f
        !pane.scrollingDisabledY
        pane.scrollingDisabledX
        content.height > pane.height
        stage.scrollFocus.is(pane)

        cleanup:
        stage.dispose()
    }

    def "title drag converts stage coordinates through nested parent transforms"() {
        given:
        def outer = new Group()
        outer.setPosition(70f, 55f)
        def layer = new FloatingPanelLayer(style())
        layer.setPosition(23f, 31f)
        layer.setSize(500f, 400f)
        def panel = panelWithContent(new Actor())
        def root = new Group()
        root.addActor(outer)
        outer.addActor(layer)
        layer.addPanel(panel)
        panel.setBounds(40f, 60f, 180f, 120f)
        validateTree(root)
        def listener = panel.titleActor.listeners.find { it instanceof com.badlogic.gdx.scenes.scene2d.InputListener }
        def down = new InputEvent()
        def start = panel.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(12f, 14f))
        down.stageX = start.x
        down.stageY = start.y
        def drag = new InputEvent()
        def target = panel.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(112f, 114f))
        drag.stageX = target.x
        drag.stageY = target.y

        when:
        listener.touchDown(down, 12f, 14f, 0, 0)
        listener.touchDragged(drag, 112f, 114f, 0)

        then:
        Math.abs(panel.x - 140f) < 0.01f
        Math.abs(panel.y - 160f) < 0.01f

    }

    def "resizable panel changes size through its handle and remains locked to layer bounds"() {
        given:
        def layer = new FloatingPanelLayer(style())
        layer.setBounds(0f, 0f, 320f, 240f)
        def panel = panelWithContent(new Actor())
        layer.addPanel(panel)
        panel.setBounds(40f, 50f, 140f, 100f)
        panel.setResizable(true)
        def resizeBackground = new BaseDrawable()
        panel.setResizeHandleBackground(resizeBackground)
        validateTree(layer)
        def listener = panel.resizeHandle.listeners.find { it instanceof InputListener }
        def down = new InputEvent()
        def start = panel.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(130f, 90f))
        down.stageX = start.x
        down.stageY = start.y
        def drag = new InputEvent()
        def target = panel.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(220f, 170f))
        drag.stageX = target.x
        drag.stageY = target.y

        when:
        listener.touchDown(down, 1f, 1f, 0, 0)
        listener.touchDragged(drag, 1f, 1f, 0)

        then:
        panel.resizable
        panel.resizeHandle.visible
        panel.resizeHandle.background.is(resizeBackground)
        panel.resizeHandle.touchable != com.badlogic.gdx.scenes.scene2d.Touchable.disabled
        panel.width > 140f
        panel.height > 100f
        panel.x >= style().edgePadding
        panel.x + panel.width <= layer.width - style().edgePadding + 0.01f
        panel.y + panel.height <= layer.height - style().edgePadding + 0.01f
    }

    def "minimizing hides the viewport and restores the expanded size"() {
        given:
        def panel = panelWithContent(new Actor())
        panel.setBounds(0f, 0f, 220f, 150f)
        panel.setResizable(true)
        validateTree(panel)
        def expandedWidth = panel.width
        def expandedHeight = panel.height

        when:
        panel.setMinimized(true)
        validateTree(panel)

        then:
        panel.minimized
        !panel.viewport.visible
        !panel.resizeHandle.visible
        panel.height < expandedHeight

        when:
        panel.setMinimized(false)
        validateTree(panel)

        then:
        !panel.minimized
        panel.viewport.visible
        panel.resizeHandle.visible
        Math.abs(panel.width - expandedWidth) < 0.01f
        Math.abs(panel.height - expandedHeight) < 0.01f
    }

    def "unlocked panel may leave its layer while locked panels are clamped"() {
        given:
        def layer = new FloatingPanelLayer(style())
        layer.setBounds(0f, 0f, 220f, 180f)
        def panel = panelWithContent(new Actor())
        layer.addPanel(panel)
        panel.setSize(100f, 80f)
        panel.setPosition(180f, 150f)

        when:
        layer.clampPanels()

        then:
        panel.x + panel.width <= layer.width - style().edgePadding + 0.01f
        panel.y + panel.height <= layer.height - style().edgePadding + 0.01f

        when:
        panel.setLockedInsideParent(false)
        panel.setPosition(180f, 150f)
        layer.clampPanels()

        then:
        panel.x == 180f
        panel.y == 150f
    }

    def "layer cascades panels and clamps them after resize"() {
        given:
        def layer = new FloatingPanelLayer(style())
        layer.setBounds(0f, 0f, 420f, 300f)
        def first = panelWithContent(new Actor())
        def second = panelWithContent(new Actor())
        layer.addPanel(first)
        layer.addPanel(second)
        first.setSize(180f, 120f)
        second.setSize(180f, 120f)
        layer.placeCascade(first)
        layer.placeCascade(second)
        validateTree(layer)

        expect:
        second.x > first.x
        second.y < first.y

        when:
        layer.setSize(120f, 90f)
        validateTree(layer)

        then:
        [first, second].every { it.x >= 8f && it.y >= 8f }
        [first, second].every { it.x + it.width <= layer.width - 8f + 0.01f }
        [first, second].every { it.y + it.height <= layer.height - 8f + 0.01f }
    }

    def "layer hit testing ignores empty space and keeps content on top of underlying map"() {
        given:
        def map = new Actor()
        map.setBounds(0f, 0f, 400f, 300f)
        def layer = new FloatingPanelLayer(style())
        layer.setBounds(0f, 0f, 400f, 300f)
        def content = new Actor()
        def panel = panelWithContent(content)
        panel.setBounds(40f, 40f, 160f, 120f)
        def root = new Group()
        root.addActor(map)
        root.addActor(layer)
        layer.addPanel(panel)
        validateTree(root)

        expect:
        root.hit(10f, 10f, true).is(map)
        def titlePoint = panel.titleActor.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(5f, 5f))
        root.hit(titlePoint.x, titlePoint.y, true) != map

        when:
        def original = content
        layer.removePanel(panel)

        then:
        !layer.children.contains(panel, true)
        original.parent.is(panel.viewport)
    }

    def "remove and redock preserve the same panel content actor"() {
        given:
        def layer = new FloatingPanelLayer(style())
        def content = new Actor()
        def panel = panelWithContent(content)
        boolean redocked = false
        panel.setRedockAction({ redocked = true })
        layer.addPanel(panel)

        when:
        panel.redock()
        layer.removePanel(panel)

        then:
        redocked
        content.is(panel.contentActor)
        content.parent.is(panel.viewport)
        panel.parent == null
    }

    private FloatingPanel panelWithContent(Actor content) {
        new FloatingPanel(
                new Label("Palette", new Label.LabelStyle(font, Color.WHITE)),
                content,
                new ScrollPane.ScrollPaneStyle(),
                style()
        )
    }

    private FloatingPanelStyle style() {
        def style = new FloatingPanelStyle()
        style.edgePadding = 8f
        style.cascadeGap = 18f
        style.minimumWidth = 80f
        style.minimumHeight = 60f
        style.titleContentGap = 4f
        style
    }

    private static void validateTree(com.badlogic.gdx.scenes.scene2d.Actor actor) {
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.utils.Layout) {
            (actor as com.badlogic.gdx.scenes.scene2d.utils.Layout).validate()
        }
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group) {
            actor.children.each { child -> validateTree(child) }
        }
    }
}
