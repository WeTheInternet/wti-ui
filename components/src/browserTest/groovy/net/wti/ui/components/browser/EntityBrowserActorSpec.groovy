package net.wti.ui.components.browser

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import spock.lang.Shared
import spock.lang.Specification

class EntityBrowserActorSpec extends Specification {

    @Shared
    HeadlessApplication app

    def setupSpec() {
        app = new HeadlessApplication(
                new ApplicationAdapter() {},
                new HeadlessApplicationConfiguration()
        )
        Gdx.graphics = new MockGraphics()
        final GL20 gl = Mock(GL20)
        Gdx.gl = gl
        Gdx.gl20 = gl
    }

    def cleanupSpec() {
        app?.exit()
    }

    def "click selects and focuses without accidental activation"() {
        given:
        def fixture = fixture(3)
        def actor = fixture.actor
        def stage = new Stage(new ScreenViewport(), Mock(Batch))
        stage.addActor(actor)

        when:
        click(actor.getCellActor("b"))

        then:
        fixture.model.selectedKey == "b"
        fixture.activations.empty
        stage.keyboardFocus.is(actor)
        fixture.latestStates["b"].selected
        fixture.latestStates["b"].keyboardCurrent

        cleanup:
        actor.dispose()
        stage?.dispose()
    }

    def "arrow traversal and Enter activation remain separate"() {
        given:
        def fixture = fixture(3)
        def actor = fixture.actor
        fixture.model.setSelectedKey("b")

        when:
        key(actor, Input.Keys.DOWN)

        then:
        fixture.model.selectedKey == "e"
        fixture.activations.empty

        when:
        key(actor, Input.Keys.ENTER)

        then:
        fixture.activations == [["e", fixture.model.selectedEntity]]

        when:
        key(actor, Input.Keys.RIGHT)

        then:
        fixture.model.selectedKey == "f"
        fixture.activations.size() == 1

        cleanup:
        actor.dispose()
    }

    def "page keys rebuild only the bounded page and select its first entry"() {
        given:
        def fixture = fixture(2, 3)
        def actor = fixture.actor

        expect:
        fixture.model.pageEntries*.key == ["a", "b", "c"]
        actor.pageTable.children.size == 3

        when:
        key(actor, Input.Keys.PAGE_DOWN)

        then:
        fixture.model.pageIndex == 1
        fixture.model.selectedKey == "d"
        fixture.model.pageEntries*.key == ["d", "e", "f"]
        actor.getCellActor("a") == null
        actor.getCellActor("d") != null
        actor.pageTable.children.size == 3

        cleanup:
        actor.dispose()
    }

    def "caller-owned empty actor replaces cells for no-results query"() {
        given:
        def fixture = fixture(3)
        def empty = new Actor()
        fixture.actor.setEmptyActor(empty)

        when:
        fixture.model.setQuery("not-present")

        then:
        fixture.model.empty
        fixture.actor.pageTable.children.size == 1
        fixture.actor.pageTable.children.first().is(empty)

        cleanup:
        fixture.actor.dispose()
    }

    def "one-column actor uses list traversal"() {
        given:
        def fixture = fixture(1)

        when:
        key(fixture.actor, Input.Keys.DOWN)
        key(fixture.actor, Input.Keys.DOWN)

        then:
        fixture.model.selectedKey == "b"

        cleanup:
        fixture.actor.dispose()
    }

    private static Fixture fixture(int columns, int pageSize = 6) {
        def model = new EntityBrowserModel<Entry>(
                { Entry entry -> entry.key } as EntityBrowserKeyProvider<Entry>,
                { Entry entry, String query ->
                    entry.label.toLowerCase().contains(query.toLowerCase())
                } as EntityBrowserMatcher<Entry>,
                pageSize
        )
        model.setEntries(["a", "b", "c", "d", "e", "f", "g"].collect {
            new Entry(it, "Entity " + it)
        })
        def latestStates = [:]
        def renderer = new EntityBrowserCellRenderer<Entry>() {
            @Override
            Actor createCell(
                    Entry entity,
                    String stableKey,
                    EntityBrowserCellState state
            ) {
                latestStates[stableKey] = state
                def cell = new Actor()
                cell.setSize(40f, 30f)
                cell
            }

            @Override
            void updateCell(
                    Actor cell,
                    Entry entity,
                    String stableKey,
                    EntityBrowserCellState state
            ) {
                latestStates[stableKey] = state
            }
        }
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
        new Fixture(
                model,
                new EntityBrowserActor<Entry>(model, renderer, columns),
                activations,
                latestStates
        )
    }

    private static void click(Actor cell) {
        assert cell != null
        def listener = cell.listeners.find { it instanceof ClickListener } as ClickListener
        assert listener != null
        listener.clicked(new InputEvent(), 1f, 1f)
    }

    private static boolean key(EntityBrowserActor<Entry> actor, int keycode) {
        def listener = actor.listeners.find { it instanceof InputListener } as InputListener
        assert listener != null
        listener.keyDown(new InputEvent(), keycode)
    }

    private static final class Fixture {
        final EntityBrowserModel<Entry> model
        final EntityBrowserActor<Entry> actor
        final List activations
        final Map latestStates

        Fixture(
                EntityBrowserModel<Entry> model,
                EntityBrowserActor<Entry> actor,
                List activations,
                Map latestStates
        ) {
            this.model = model
            this.actor = actor
            this.activations = activations
            this.latestStates = latestStates
        }
    }

    private static final class Entry {
        final String key
        final String label

        Entry(String key, String label) {
            this.key = key
            this.label = label
        }
    }
}
