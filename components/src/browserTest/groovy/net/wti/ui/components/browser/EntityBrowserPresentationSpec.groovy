package net.wti.ui.components.browser

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import net.wti.ui.components.SizingTextTooltip
import spock.lang.Shared
import spock.lang.Specification

class EntityBrowserPresentationSpec extends Specification {

    @Shared
    HeadlessApplication app

    @Shared
    BitmapFont font

    @Shared
    Skin skin

    def setupSpec() {
        app = new HeadlessApplication(
                new ApplicationAdapter() {},
                new HeadlessApplicationConfiguration()
        )
        Gdx.graphics = new MockGraphics()
        final GL20 gl = Mock(GL20)
        gl.glGenTexture() >> 1
        Gdx.gl = gl
        Gdx.gl20 = gl
        font = new BitmapFont()
        skin = new Skin()
        def labelStyle = new Label.LabelStyle(font, Color.WHITE)
        skin.add("default", labelStyle)
        skin.add(
                "tooltip-default",
                new TextTooltip.TextTooltipStyle(labelStyle, null)
        )
    }

    def cleanupSpec() {
        skin?.dispose()
        app?.exit()
    }

    def "card applies normal hover selected and keyboard-current Skin drawables"() {
        given:
        def normal = new BaseDrawable()
        def hovered = new BaseDrawable()
        def selected = new BaseDrawable()
        def keyboard = new BaseDrawable()
        def style = style(normal, hovered, selected, keyboard)
        def card = new EntityBrowserCard(
                skin,
                style,
                new Actor(),
                "Primary",
                "Secondary",
                null,
                new EntityBrowserCellState(false, false)
        )

        expect:
        card.background.is(normal)

        when:
        hoverListener(card).enter(null, 1f, 1f, -1, null)

        then:
        card.hovered
        card.background.is(hovered)

        when:
        card.setBrowserState(new EntityBrowserCellState(true, false))

        then:
        card.background.is(selected)

        when:
        card.setBrowserState(new EntityBrowserCellState(true, true))

        then:
        card.background.is(keyboard)

        when:
        hoverListener(card).exit(null, 1f, 1f, -1, null)

        then:
        !card.hovered
        card.background.is(keyboard)
    }

    def "card renderer supports independent image-heavy and metadata-heavy entity types"() {
        given:
        def cardStyle = style(
                new BaseDrawable(),
                new BaseDrawable(),
                new BaseDrawable(),
                new BaseDrawable()
        )
        def tooltipStyle = new TextTooltip.TextTooltipStyle(
                skin.get(Label.LabelStyle),
                new BaseDrawable()
        )
        cardStyle.tooltipStyle = tooltipStyle
        def imageEntity = new ImageAsset("image-1", "Coastal panorama")
        def imageRenderer = new EntityBrowserCardRenderer<ImageAsset>(
                skin,
                cardStyle,
                new EntityBrowserCardContentProvider<ImageAsset>() {
                    @Override
                    Actor createContent(ImageAsset entity, Skin ignored) {
                        def thumbnail = new Actor()
                        thumbnail.name = "thumbnail-" + entity.key
                        thumbnail.setSize(96f, 72f)
                        thumbnail
                    }

                    @Override
                    String primaryText(ImageAsset entity) {
                        entity.title
                    }

                    @Override
                    String secondaryText(ImageAsset entity) {
                        "1600 x 900"
                    }

                    @Override
                    String tooltipText(ImageAsset entity) {
                        "A deliberately long generic image description used to verify wrapped tooltip presentation."
                    }
                }
        )
        def document = new DocumentRecord("doc-7", "Quarterly notes", "owner: example")
        def documentRenderer = new EntityBrowserCardRenderer<DocumentRecord>(
                skin,
                cardStyle,
                new EntityBrowserCardContentProvider<DocumentRecord>() {
                    @Override
                    Actor createContent(DocumentRecord entity, Skin ignored) {
                        def metadata = new Table()
                        metadata.name = "metadata-" + entity.key
                        metadata.add(new Actor()).size(140f, 20f)
                        metadata
                    }

                    @Override
                    String primaryText(DocumentRecord entity) {
                        entity.title
                    }

                    @Override
                    String secondaryText(DocumentRecord entity) {
                        entity.metadata
                    }
                }
        )

        when:
        def imageCard = imageRenderer.createCell(
                imageEntity,
                imageEntity.key,
                new EntityBrowserCellState(false, false)
        ) as EntityBrowserCard
        def documentCard = documentRenderer.createCell(
                document,
                document.key,
                new EntityBrowserCellState(true, false)
        ) as EntityBrowserCard

        then:
        imageCard.contentActor.name == "thumbnail-image-1"
        imageCard.primaryLabel.text.toString() == "Coastal panorama"
        imageCard.secondaryLabel.text.toString() == "1600 x 900"
        imageCard.listeners.any { it instanceof SizingTextTooltip }
        documentCard.contentActor.name == "metadata-doc-7"
        documentCard.primaryLabel.text.toString() == "Quarterly notes"
        documentCard.secondaryLabel.text.toString() == "owner: example"
        documentCard.browserState.selected

        when: "the long sizing tooltip lays out its wrapped height"
        def tooltip = imageCard.listeners.find {
            it instanceof SizingTextTooltip
        } as SizingTextTooltip
        tooltip.container.pack()
        tooltip.container.validate()
        tooltip.actor.validate()

        then:
        tooltip.style.is(tooltipStyle)
        new EntityBrowserCardStyle(cardStyle).tooltipStyle.is(tooltipStyle)
        tooltip.actor.width > 0f
        tooltip.actor.height > font.lineHeight * 2f
    }

    def "optional detail pane follows selection and refreshes a replaced stable entity"() {
        given:
        def model = model(4)
        def first = new Entry("a", "first")
        def selected = new Entry("b", "before")
        model.setEntries([first, selected])
        def builds = []
        def pane = new EntityBrowserDetailPane<Entry>(
                model,
                { Entry entity, String key ->
                    builds << entity
                    def actor = new Actor()
                    actor.name = key + ":" + entity.label
                    actor
                } as EntityBrowserDetailRenderer<Entry>
        )
        def empty = new Actor()
        pane.setEmptyActor(empty)

        expect:
        pane.currentDetail == null
        pane.children.first().is(empty)

        when:
        model.setSelectedKey("b")

        then:
        pane.currentDetail.name == "b:before"
        builds.last().is(selected)

        when:
        def replacement = new Entry("b", "after")
        model.setEntries([first, replacement])

        then:
        model.selectedKey == "b"
        pane.currentDetail.name == "b:after"
        builds.last().is(replacement)

        when:
        model.clearSelection()

        then:
        pane.currentDetail == null
        pane.children.first().is(empty)

        cleanup:
        pane.dispose()
    }

    def "double-click activation remains opt-in and emits once on the second click"() {
        given:
        def model = model(4)
        model.setEntries([new Entry("a", "alpha")])
        def activations = []
        model.addListener(new EntityBrowserListener<Entry>() {
            @Override
            void activated(
                    EntityBrowserModel<Entry> ignored,
                    String stableKey,
                    Entry entity
            ) {
                activations << [stableKey, entity]
            }
        })
        def actor = new EntityBrowserActor<Entry>(
                model,
                new EntityBrowserCellRenderer<Entry>() {
                    @Override
                    Actor createCell(
                            Entry entity,
                            String stableKey,
                            EntityBrowserCellState state
                    ) {
                        new Actor()
                    }
                },
                1
        )

        when:
        actor.handleCellClick("a", 1)
        actor.handleCellClick("a", 2)

        then:
        activations.empty

        when:
        actor.setDoubleClickActivates(true)
        actor.handleCellClick("a", 1)

        then:
        activations.empty

        when:
        actor.handleCellClick("a", 2)

        then:
        activations == [["a", model.selectedEntity]]

        cleanup:
        actor.dispose()
    }

    def "empty view uses caller Skin and optional text without domain policy"() {
        when:
        def view = new EntityBrowserEmptyView(
                skin,
                "No matches",
                "Change the query to see more generic entities."
        )

        then:
        view.skin.is(skin)
        view.titleLabel.text.toString() == "No matches"
        view.messageLabel.text.toString().startsWith("Change the query")
    }

    private static InputListener hoverListener(EntityBrowserCard card) {
        card.listeners.find {
            it instanceof InputListener && !(it instanceof SizingTextTooltip)
        } as InputListener
    }

    private EntityBrowserCardStyle style(
            BaseDrawable normal,
            BaseDrawable hovered,
            BaseDrawable selected,
            BaseDrawable keyboard
    ) {
        def cardStyle = new EntityBrowserCardStyle()
        cardStyle.normal = normal
        cardStyle.hovered = hovered
        cardStyle.selected = selected
        cardStyle.keyboardCurrent = keyboard
        cardStyle.primaryLabelStyle = skin.get(Label.LabelStyle)
        cardStyle.secondaryLabelStyle = skin.get(Label.LabelStyle)
        cardStyle.minimumWidth = 160f
        cardStyle.minimumHeight = 100f
        cardStyle
    }

    private static EntityBrowserModel<Entry> model(int pageSize) {
        new EntityBrowserModel<Entry>(
                { Entry entity -> entity.key } as EntityBrowserKeyProvider<Entry>,
                { Entry entity, String query ->
                    entity.label.contains(query)
                } as EntityBrowserMatcher<Entry>,
                pageSize
        )
    }

    private static final class Entry {
        final String key
        final String label

        Entry(String key, String label) {
            this.key = key
            this.label = label
        }
    }

    private static final class ImageAsset {
        final String key
        final String title

        ImageAsset(String key, String title) {
            this.key = key
            this.title = title
        }
    }

    private static final class DocumentRecord {
        final String key
        final String title
        final String metadata

        DocumentRecord(String key, String title, String metadata) {
            this.key = key
            this.title = title
            this.metadata = metadata
        }
    }
}
