package net.wti.ui.view.responsive

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.utils.viewport.ScreenViewport
import spock.lang.Shared
import spock.lang.Specification

class ResponsiveAccordionSpec extends Specification {

    @Shared HeadlessApplication app

    def setupSpec() {
        app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration())
        Gdx.graphics = new MockGraphics()
        final GL20 gl = Mock(GL20)
        gl.glGenTexture() >> 1
        gl.glCreateShader(_ as int) >> 1
        gl.glCreateProgram(_ as int) >> 1
        Gdx.gl = gl
        Gdx.gl20 = gl
    }

    def cleanupSpec() {
        app?.exit()
    }

    def "independent sections can be expanded simultaneously without replacing bodies"() {
        given:
        def accordion = new ResponsiveAccordion()
        def firstHeader = new Actor()
        firstHeader.setSize(100f, 20f)
        def firstBody = body(80f)
        def secondBody = body(120f)
        accordion.addSection("first", firstHeader, firstBody)
        def secondHeader = new Actor()
        secondHeader.setSize(100f, 20f)
        accordion.addSection("second", secondHeader, secondBody)

        when:
        accordion.expand("first")
        accordion.expand("second")
        validateTree(accordion)
        def expandedHeight = accordion.getPrefHeight()

        then:
        accordion.isExpanded("first")
        accordion.isExpanded("second")
        firstBody.parent.is(accordion)
        secondBody.parent.is(accordion)
        firstBody.visible
        secondBody.visible

        when:
        accordion.collapse("first")
        validateTree(accordion)

        then:
        !accordion.isExpanded("first")
        accordion.isExpanded("second")
        firstBody.parent.is(accordion)
        firstBody.height == 0f
        firstBody.getTouchable() == Touchable.disabled
        accordion.getPrefHeight() < expandedHeight
    }

    def "exclusive mode preserves legacy openOnly behavior"() {
        given:
        def accordion = new ResponsiveAccordion()
        def first = body(50f)
        def second = body(60f)
        accordion.addSection("first", new Actor(), first)
        accordion.addSection("second", new Actor(), second)
        accordion.setExclusive(true)

        when:
        accordion.expand("first")
        accordion.expand("second")

        then:
        !accordion.isExpanded("first")
        accordion.isExpanded("second")

        when:
        accordion.openOnly("first")

        then:
        accordion.isExpanded("first")
        !accordion.isExpanded("second")
    }

    def "trailing header action is distinct and does not toggle the section"() {
        given:
        def accordion = new ResponsiveAccordion()
        def header = new Actor()
        def action = new Actor()
        def body = body(80f)
        accordion.addSection("tools", header, body, action)

        expect:
        !accordion.isExpanded("tools")
        !action.listeners.any { it instanceof com.badlogic.gdx.scenes.scene2d.utils.ClickListener }
        header.listeners.any { it instanceof com.badlogic.gdx.scenes.scene2d.utils.ClickListener }
    }

    def "detached body remains caller-owned while later accordion rebuilds"() {
        given:
        def accordion = new ResponsiveAccordion()
        def detached = body(80f)
        def retained = body(120f)
        accordion.addSection("detached", new Actor(), detached)
        accordion.addSection("retained", new Actor(), retained)
        accordion.expand("detached")

        when:
        accordion.detachBody("detached")
        detached.setVisible(true)
        detached.setTouchable(Touchable.enabled)
        accordion.expand("retained")
        validateTree(accordion)

        then:
        accordion.isBodyDetached("detached")
        !accordion.isExpanded("detached")
        detached.parent == null
        detached.visible
        detached.getTouchable() == Touchable.enabled
        retained.parent.is(accordion)
        retained.visible

        when:
        accordion.attachBody("detached")
        accordion.expand("detached")
        validateTree(accordion)

        then:
        !accordion.isBodyDetached("detached")
        detached.parent.is(accordion)
        detached.visible
    }

    def "accordion remains a normal actor inside a floating panel and content viewport"() {
        given:
        def accordion = new ResponsiveAccordion()
        def body = body(140f)
        accordion.addSection("content", new Actor(), body)
        accordion.expand("content")
        def panel = new FloatingPanel(new Actor(), accordion, new ScrollPane.ScrollPaneStyle(), floatingStyle())
        panel.setBounds(0f, 0f, 240f, 180f)
        def viewport = new ResponsiveContentScrollPane(panel, new ScrollPane.ScrollPaneStyle())
        viewport.setBounds(11f, 13f, 260f, 200f)

        when:
        validateTree(viewport)

        then:
        viewport.actor.is(panel)
        panel.contentActor.is(accordion)
        accordion.parent.is(panel.viewport)
        accordion.width > 0f
        body.visible
        body.parent.is(accordion)
    }

    def "scroll focus follows the hovered viewport rather than an unrelated viewport"() {
        given:
        def first = new ResponsiveContentScrollPane(body(300f), new ScrollPane.ScrollPaneStyle())
        def second = new ResponsiveContentScrollPane(body(300f), new ScrollPane.ScrollPaneStyle())
        first.setBounds(0f, 0f, 120f, 100f)
        second.setBounds(140f, 0f, 120f, 100f)
        def stage = new Stage(new ScreenViewport(), Mock(Batch))
        stage.addActor(first)
        stage.addActor(second)
        validateTree(stage.root)

        when:
        hover(first)

        then:
        stage.scrollFocus.is(first)

        when:
        hover(second)

        then:
        stage.scrollFocus.is(second)

        cleanup:
        stage.dispose()
    }

    def "resize invalidation updates body bounds through Table layout"() {
        given:
        def accordion = new ResponsiveAccordion()
        def body = body(80f)
        accordion.addSection("resize", new Actor(), body)
        accordion.expand("resize")
        accordion.setBounds(0f, 0f, 180f, 160f)
        validateTree(accordion)
        def originalWidth = body.width

        when:
        accordion.setWidth(320f)
        accordion.invalidateHierarchy()
        validateTree(accordion)

        then:
        body.width > originalWidth
        body.width <= accordion.width + 0.01f
    }

    private static Actor body(final float height) {
        def body = new com.badlogic.gdx.scenes.scene2d.ui.Table()
        body.add(new Actor()).size(100f, height)
        body
    }

    private static FloatingPanelStyle floatingStyle() {
        def style = new FloatingPanelStyle()
        style.edgePadding = 8f
        style.minimumWidth = 80f
        style.minimumHeight = 60f
        style
    }

    private static void hover(final ResponsiveContentScrollPane pane) {
        pane.listeners.findAll { it instanceof InputListener }.each { listener ->
            (listener as InputListener).enter(new InputEvent(), 1f, 1f, -1, null)
        }
    }

    private static void validateTree(final Actor actor) {
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.utils.Layout) {
            (actor as com.badlogic.gdx.scenes.scene2d.utils.Layout).validate()
        }
        if (actor instanceof Group) {
            actor.children.each { child -> validateTree(child) }
        }
    }
}
