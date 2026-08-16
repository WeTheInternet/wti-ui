package net.wti.ui.view.responsive

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class ResponsivePanelSpec extends Specification {

    @Shared
    HeadlessApplication app

    @Shared
    BitmapFont font

    def setupSpec() {
        app = new HeadlessApplication(
                new ApplicationAdapter() {},
                new HeadlessApplicationConfiguration()
        )
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

    @Unroll
    def "panel selects #expectedColumns columns at #width x #height"() {
        given:
        def panel = populatedPanel(4, 2)

        when:
        panel.setBounds(0f, 0f, width, height)
        validateTree(panel)

        then:
        panel.columnCount == expectedColumns
        panel.sections.every { section ->
            section.x >= panel.style.panelPadLeft - 0.01f &&
                    section.x + section.width <=
                    panel.width - panel.style.panelPadRight + 0.01f
        }
        if (expectedColumns == 2) {
            def left = panel.sections[0]
            def right = panel.sections[1]
            assert Math.abs(left.width - right.width) < 0.01f
            assert right.x - (left.x + left.width) >= panel.style.columnGap - 0.01f
        }

        where:
        width  | height | expectedColumns
        1280f  | 800f   | 2
        1090f  | 634f   | 2
        800f   | 480f   | 1
    }

    def "production hierarchy applies every spacing boundary and wrapped body grows its row"() {
        given:
        def style = spacing()
        def panel = new ResponsivePanel(style)
        def first = panel.addSection(label("First section"))
        def longRow = first.addWrappedLabelRow(
                label("Ctrl + left drag"),
                "Pan the map without moving selection while a deliberately long description " +
                        "wraps onto several lines and continues explaining how pointer movement, " +
                        "selection state, camera bounds, and editing mode interact without ever " +
                        "overlapping the row which follows it.",
                labelStyle()
        )
        def following = first.addWrappedLabelRow(
                label("Arrows"),
                "Pan the map.",
                labelStyle()
        )
        def second = panel.addSection(label("Second section"))
        second.addWrappedLabelRow(label("Map < >"), "Switch map files.", labelStyle())

        when:
        panel.setBounds(0f, 0f, 800f, 480f)
        validateTree(panel)

        then: "panel and section gaps come from production actor bounds"
        first.x >= style.panelPadLeft - 0.01f
        first.x + first.width <= panel.width - style.panelPadRight + 0.01f
        first.y >= second.y + second.height + style.sectionGap - 0.01f

        and: "heading, row, inner padding, and leading/body gaps are non-overlapping"
        first.headingActor.y >= longRow.y + longRow.height + style.headingGap - 0.01f
        longRow.y >= following.y + following.height + style.rowGap - 0.01f
        longRow.leadingActor.x >= style.rowPadLeft - 0.01f
        longRow.bodyActor.x >= longRow.leadingActor.x +
                longRow.leadingActor.width + style.leadingBodyGap - 0.01f
        longRow.bodyActor.x + longRow.bodyActor.width <=
                longRow.width - style.rowPadRight + 0.01f

        and: "the actual wrapped Label determines row height and pushes its sibling"
        (longRow.bodyActor as Label).wrap
        longRow.bodyActor.height > font.lineHeight * 2f
        longRow.height > following.height
        following.y + following.height <= longRow.y - style.rowGap + 0.01f
    }

    def "replacing page content preserves outer bounds and scroll pane owns overflow focus"() {
        given:
        def original = populatedPanel(2, 8)
        def pane = new ResponsiveScrollPane(original, new ScrollPane.ScrollPaneStyle())
        pane.setBounds(17f, 23f, 420f, 180f)
        final Batch batch = Mock(Batch)
        final Stage stage = new Stage(new ScreenViewport(), batch)
        stage.addActor(pane)

        and:
        def replacement = populatedPanel(3, 10)

        when:
        pane.setContent(replacement)
        validateTree(pane)

        then: "content replacement does not rewrite the consumer-owned viewport"
        pane.x == 17f
        pane.y == 23f
        pane.width == 420f
        pane.height == 180f
        pane.actor.is(replacement)
        replacement.parent.is(pane)

        and: "vertical overflow remains under the standard ScrollPane clipping boundary"
        pane.scrollingDisabledX
        !pane.scrollingDisabledY
        replacement.prefHeight > pane.height

        when: "the attached production hover listener receives pointer entry"
        pane.listeners.findAll { it instanceof InputListener }.each { listener ->
            (listener as InputListener).enter(null, 1f, 1f, -1, null)
        }

        then:
        stage.scrollFocus.is(pane)

        cleanup:
        stage.dispose()
    }

    private ResponsivePanel populatedPanel(int sectionCount, int rowsPerSection) {
        def panel = new ResponsivePanel(spacing())
        sectionCount.times { sectionIndex ->
            def section = panel.addSection(label("Section ${sectionIndex}"))
            rowsPerSection.times { rowIndex ->
                section.addWrappedLabelRow(
                        label("Key ${rowIndex}"),
                        "Description ${rowIndex} with enough content to exercise the production wrapping path.",
                        labelStyle()
                )
            }
        }
        panel
    }

    private ResponsivePanelStyle spacing() {
        def style = new ResponsivePanelStyle()
        style.background = new BaseDrawable()
        style.minimumColumnWidth = 480f
        style.panelPadTop = 18f
        style.panelPadLeft = 20f
        style.panelPadBottom = 16f
        style.panelPadRight = 22f
        style.columnGap = 24f
        style.sectionGap = 17f
        style.headingGap = 9f
        style.rowGap = 7f
        style.rowPadTop = 3f
        style.rowPadLeft = 5f
        style.rowPadBottom = 4f
        style.rowPadRight = 6f
        style.leadingBodyGap = 11f
        style
    }

    private Label label(String text) {
        new Label(text, labelStyle())
    }

    private Label.LabelStyle labelStyle() {
        new Label.LabelStyle(font, Color.WHITE)
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
