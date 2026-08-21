package net.wti.ui.components.browser

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import net.wti.ui.gdx.theme.CompositeGdxTheme
import net.wti.ui.gdx.theme.UiDataBundle
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

@IgnoreIf({ System.getenv("WTI_ENTITY_BROWSER_VISUAL") != "1" })
class EntityBrowserPresentationVisualSpec extends Specification {

    def "interactive generic browser presentation playground"() {
        given:
        def failure = new AtomicReference<Throwable>()
        def configuration = new Lwjgl3ApplicationConfiguration()
        configuration.setTitle("Generic entity browser presentation")
        configuration.setWindowedMode(1180, 760)
        configuration.setResizable(true)
        configuration.useVsync(true)

        when:
        new Lwjgl3Application(new PlaygroundApplication(failure), configuration)

        then:
        failure.get() == null
    }

    private static final class PlaygroundApplication extends ApplicationAdapter {

        private final AtomicReference<Throwable> failure
        private Stage stage
        private Skin skin

        PlaygroundApplication(AtomicReference<Throwable> failure) {
            this.failure = failure
        }

        @Override
        void create() {
            try {
                TooltipManager.instance.initialTime = 0.2f
                skin = new CompositeGdxTheme(
                        null,
                        new UiDataBundle(
                                "cc-by-4/wti/common/wti-common-ui.json",
                                "cc-by-4/wti/common/wti-common-ui.atlas"
                        ),
                        new UiDataBundle(
                                "cc-by-4/raeleus/sgx/sgx-ui.json",
                                "cc-by-4/raeleus/sgx/sgx-ui.atlas"
                        )
                ).skin
                stage = new Stage(new ScreenViewport())
                def input = new InputMultiplexer()
                input.addProcessor(new InputAdapter() {
                    @Override
                    boolean keyDown(int keycode) {
                        if (keycode == Input.Keys.ESCAPE) {
                            Gdx.app.exit()
                            return true
                        }
                        false
                    }
                })
                input.addProcessor(stage)
                Gdx.input.inputProcessor = input
                stage.addActor(buildRoot())
            } catch (Throwable error) {
                failure.set(error)
                Gdx.app.exit()
            }
        }

        @Override
        void render() {
            if (failure.get() != null) {
                Gdx.app.exit()
                return
            }
            Gdx.gl.glClearColor(0.045f, 0.055f, 0.075f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            stage.act(Gdx.graphics.deltaTime)
            stage.draw()
        }

        @Override
        void resize(int width, int height) {
            stage?.viewport?.update(width, height, true)
        }

        @Override
        void dispose() {
            stage?.dispose()
            skin?.dispose()
        }

        private Actor buildRoot() {
            def root = new Table(skin)
            root.setFillParent(true)
            root.pad(16f)
            def title = new Label("Generic entity browser presentation", skin)
            def instructions = new Label(
                    "Resize the window; search to no results; hover/click cards; use arrows, Enter, and double-click. Press Escape or close when reviewed.",
                    skin
            )
            instructions.wrap = true
            root.add(title)
                    .colspan(2)
                    .left()
                    .padBottom(6f)
                    .row()
            root.add(instructions).colspan(2).growX().left().padBottom(12f).row()

            def style = cardStyle()
            root.add(buildGallery(style)).grow().padRight(10f)
            root.add(buildDocumentList(new EntityBrowserCardStyle(style))).grow()
            root
        }

        private Table buildGallery(EntityBrowserCardStyle style) {
            style.minimumWidth = 142f
            style.minimumHeight = 126f
            def model = new EntityBrowserModel<GalleryAsset>(
                    { GalleryAsset asset -> asset.key } as EntityBrowserKeyProvider<GalleryAsset>,
                    { GalleryAsset asset, String query ->
                        def needle = query.toLowerCase()
                        asset.key.toLowerCase().contains(needle) ||
                                asset.title.toLowerCase().contains(needle) ||
                                asset.metadata.toLowerCase().contains(needle)
                    } as EntityBrowserMatcher<GalleryAsset>,
                    8
            )
            model.setEntries((1..13).collect { int index ->
                new GalleryAsset(
                        String.format("asset-%02d", index),
                        index == 4
                                ? "A deliberately long generic gallery label that must wrap cleanly"
                                : "Gallery asset " + index,
                        index % 2 == 0 ? "landscape, reviewed" : "portrait, pending"
                )
            })
            def renderer = new EntityBrowserCardRenderer<GalleryAsset>(
                    skin,
                    style,
                    new EntityBrowserCardContentProvider<GalleryAsset>() {
                        @Override
                        Actor createContent(GalleryAsset asset, Skin ignored) {
                            def thumbnail = new Table(skin)
                            thumbnail.background = skin.getDrawable("button")
                            thumbnail.add(new Label(
                                    asset.key.substring(asset.key.length() - 2),
                                    skin
                            )).expand().center()
                            thumbnail
                        }

                        @Override
                        String primaryText(GalleryAsset asset) {
                            asset.title
                        }

                        @Override
                        String secondaryText(GalleryAsset asset) {
                            asset.metadata
                        }

                        @Override
                        String tooltipText(GalleryAsset asset) {
                            "Preview " + asset.key +
                                    ". This deliberately long tooltip verifies wrapping while explaining generic metadata and interaction without domain-specific policy."
                        }
                    }
            )
            def browser = new EntityBrowserActor<GalleryAsset>(model, renderer, 4)
            browser.doubleClickActivates = true
            browser.emptyActor = new EntityBrowserEmptyView(
                    skin,
                    "No gallery matches",
                    "Clear or change the search query."
            )

            def detail = new EntityBrowserDetailPane<GalleryAsset>(
                    model,
                    { GalleryAsset asset, String key ->
                        def table = new Table(skin)
                        table.background = skin.getDrawable("button")
                        table.pad(8f)
                        table.add(new Label("Selected: " + key, skin)).left().row()
                        table.add(new Label(asset.title, skin)).growX().left().row()
                        table.add(new Label("Metadata: " + asset.metadata, skin)).growX().left()
                        table
                    } as EntityBrowserDetailRenderer<GalleryAsset>
            )
            detail.emptyActor = new EntityBrowserEmptyView(
                    skin,
                    "Nothing selected",
                    "Click a gallery card to inspect it."
            )

            def activation = new Label("Activation: none", skin)
            model.addListener(new EntityBrowserListener<GalleryAsset>() {
                @Override
                void activated(
                        EntityBrowserModel<GalleryAsset> ignored,
                        String stableKey,
                        GalleryAsset entity
                ) {
                    activation.setText("Activation: " + stableKey)
                }
            })

            def host = new Table(skin)
            host.background = skin.getDrawable("window")
            host.pad(8f)
            host.add(new Label("Thumbnail-heavy grid + optional details", skin))
                    .growX().left().row()
            host.add(search(model, "Search 13 gallery assets...")).growX().padBottom(6f).row()
            def browserScroll = new ScrollPane(browser, skin)
            browserScroll.setScrollingDisabled(true, false)
            host.add(browserScroll).grow().row()
            host.add(pager(model)).growX().padTop(6f).row()
            host.add(detail).growX().height(112f).padTop(6f).row()
            host.add(activation).growX().left().padTop(4f)
            host
        }

        private Table buildDocumentList(EntityBrowserCardStyle style) {
            style.minimumWidth = 360f
            style.minimumHeight = 64f
            def model = new EntityBrowserModel<DocumentRecord>(
                    { DocumentRecord document -> document.key } as EntityBrowserKeyProvider<DocumentRecord>,
                    { DocumentRecord document, String query ->
                        def needle = query.toLowerCase()
                        document.key.toLowerCase().contains(needle) ||
                                document.title.toLowerCase().contains(needle) ||
                                document.metadata.toLowerCase().contains(needle)
                    } as EntityBrowserMatcher<DocumentRecord>,
                    5
            )
            model.setEntries((1..9).collect { int index ->
                new DocumentRecord(
                        "document-" + index,
                        index == 7
                                ? "Extremely long meeting notes title demonstrating list-mode wrapping and stable vertical layout"
                                : "Document record " + index,
                        "owner: example-" + index + "  |  revision: " + (20 + index)
                )
            })
            def renderer = new EntityBrowserCardRenderer<DocumentRecord>(
                    skin,
                    style,
                    new EntityBrowserCardContentProvider<DocumentRecord>() {
                        @Override
                        String primaryText(DocumentRecord document) {
                            document.title
                        }

                        @Override
                        String secondaryText(DocumentRecord document) {
                            document.metadata
                        }

                        @Override
                        String tooltipText(DocumentRecord document) {
                            "Text-heavy metadata example for " + document.key +
                                    ". Selection remains distinct from activation."
                        }
                    }
            )
            def browser = new EntityBrowserActor<DocumentRecord>(model, renderer, 1)
            browser.doubleClickActivates = true
            browser.emptyActor = new EntityBrowserEmptyView(
                    skin,
                    "No document matches",
                    "This list intentionally has no detail pane."
            )
            def activation = new Label("Activation: none", skin)
            model.addListener(new EntityBrowserListener<DocumentRecord>() {
                @Override
                void activated(
                        EntityBrowserModel<DocumentRecord> ignored,
                        String stableKey,
                        DocumentRecord entity
                ) {
                    activation.setText("Activation: " + stableKey)
                }
            })

            def host = new Table(skin)
            host.background = skin.getDrawable("window")
            host.pad(8f)
            host.add(new Label("Metadata-heavy one-column list", skin))
                    .growX().left().row()
            host.add(search(model, "Search 9 document records...")).growX().padBottom(6f).row()
            def browserScroll = new ScrollPane(browser, skin)
            browserScroll.setScrollingDisabled(true, false)
            host.add(browserScroll).grow().row()
            host.add(pager(model)).growX().padTop(6f).row()
            host.add(activation).growX().left().padTop(4f)
            host
        }

        private TextField search(EntityBrowserModel model, String message) {
            def field = new TextField("", skin)
            field.messageText = message
            field.addListener(new ChangeListener() {
                @Override
                void changed(ChangeEvent event, Actor actor) {
                    model.query = field.text
                }
            })
            field
        }

        private Table pager(EntityBrowserModel model) {
            def previous = new TextButton("Previous", skin)
            def next = new TextButton("Next", skin)
            def page = new Label(pageText(model), skin)
            previous.addListener(new ClickListener() {
                @Override
                void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    model.previousPage()
                }
            })
            next.addListener(new ClickListener() {
                @Override
                void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    model.nextPage()
                }
            })
            model.addListener(new EntityBrowserListener() {
                @Override
                void contentsChanged(EntityBrowserModel ignored) {
                    page.setText(pageText(model))
                }
            })
            def controls = new Table(skin)
            controls.add(previous).left()
            controls.add(page).expandX().center()
            controls.add(next).right()
            controls
        }

        private static String pageText(EntityBrowserModel model) {
            model.pageCount == 0
                    ? "Page 0 / 0"
                    : "Page " + (model.pageIndex + 1) + " / " + model.pageCount
        }

        private EntityBrowserCardStyle cardStyle() {
            def button = skin.get(TextButton.TextButtonStyle)
            def style = new EntityBrowserCardStyle()
            style.normal = button.up
            style.hovered = button.over
            style.selected = button.down
            style.keyboardCurrent = button.checked == null ? button.over : button.checked
            style.primaryLabelStyle = skin.get(Label.LabelStyle)
            style.secondaryLabelStyle = skin.get(Label.LabelStyle)
            def tooltip = skin.get("tooltip-default", TextTooltip.TextTooltipStyle)
            style.tooltipStyle = new TextTooltip.TextTooltipStyle(
                    skin.get(Label.LabelStyle),
                    tooltip.background
            )
            style.padding = 7f
            style.contentTextGap = 5f
            style.textGap = 2f
            style
        }
    }

    private static final class GalleryAsset {
        final String key
        final String title
        final String metadata

        GalleryAsset(String key, String title, String metadata) {
            this.key = key
            this.title = title
            this.metadata = metadata
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
