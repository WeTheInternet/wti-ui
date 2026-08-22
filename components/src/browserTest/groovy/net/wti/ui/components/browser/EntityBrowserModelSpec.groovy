package net.wti.ui.components.browser

import spock.lang.Specification

class EntityBrowserModelSpec extends Specification {

    def "filtering retains deterministic caller order and caller-owned semantics"() {
        given:
        def seenQueries = []
        def model = model(3) { Entry entry, String query ->
            seenQueries << query
            entry.label.toLowerCase().contains(query.toLowerCase())
        }
        model.setEntries(entries("gamma", "alpha", "alpine", "beta"))

        when:
        model.setQuery("AL")

        then:
        model.filteredEntries*.key == ["alpha", "alpine"]
        seenQueries == ["AL", "AL", "AL", "AL"]
        model.entries*.key == ["gamma", "alpha", "alpine", "beta"]
    }

    def "page count bounds and destination selection are deterministic"() {
        given:
        def model = model(3)
        model.setEntries(entries("a", "b", "c", "d", "e", "f", "g"))

        expect:
        model.pageCount == 3
        model.pageIndex == 0
        model.pageEntries*.key == ["a", "b", "c"]
        model.selectedKey == null

        when:
        model.setPage(99)

        then:
        model.pageIndex == 2
        model.pageEntries*.key == ["g"]
        model.selectedKey == "g"

        when:
        model.setPage(-20)

        then:
        model.pageIndex == 0
        model.pageEntries*.key == ["a", "b", "c"]
        model.selectedKey == "a"
    }

    def "empty and no-results states clear selection and expose zero pages"() {
        given:
        def model = model(4)

        expect:
        model.empty
        model.pageCount == 0
        model.pageEntries.empty
        !model.activateSelection()

        when:
        model.setEntries(entries("a", "b"))
        model.setSelectedKey("b")
        model.setQuery("missing")

        then:
        model.empty
        model.pageCount == 0
        model.pageIndex == 0
        model.selectedKey == null
        model.selectedEntity == null
    }

    def "stable-key selection survives immutable source replacement"() {
        given:
        def model = model(2)
        def original = entries("a", "b", "c")
        model.setEntries(original)
        model.setSelectedKey("b")
        def replacement = [
                new Entry("a", "a updated"),
                new Entry("b", "b updated"),
                new Entry("c", "c updated")
        ]

        when:
        model.setEntries(replacement)

        then:
        model.selectedKey == "b"
        model.selectedEntity.is(replacement[1])
        !model.selectedEntity.is(original[1])
        model.pageIndex == 0
    }

    def "query and source reconciliation retain selection or choose the same result index"() {
        given:
        def model = model(2)
        model.setEntries(entries("a", "b", "c", "d"))
        model.setSelectedKey("c")

        when: "the query keeps the selected stable key"
        model.setQuery("c")

        then:
        model.selectedKey == "c"
        model.pageIndex == 0

        when: "the query removes it and leaves a shorter result"
        model.setQuery("d")

        then:
        model.selectedKey == "d"
        model.pageIndex == 0

        when: "the selected source entry disappears"
        model.setQuery("")
        model.setSelectedKey("c")
        model.setEntries(entries("a", "b", "d"))

        then: "the entry now at the prior filtered index becomes current"
        model.selectedKey == "d"

        when: "the selected final entry disappears"
        model.setEntries(entries("a", "b"))

        then: "the preceding final entry becomes current"
        model.selectedKey == "b"
    }

    def "duplicate stable keys are rejected without corrupting the current snapshot"() {
        given:
        def model = model(4)
        model.setEntries(entries("safe"))

        when:
        model.setEntries([
                new Entry("same", "first"),
                new Entry("same", "second")
        ])

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains("Duplicate entity browser key: same")
        model.entries*.key == ["safe"]
    }

    def "one-column traversal follows ordered results and page boundaries"() {
        given:
        def model = model(2)
        model.setEntries(entries("a", "b", "c", "d"))

        when:
        model.moveSelection(EntityBrowserDirection.DOWN, 1)

        then:
        model.selectedKey == "a"
        model.pageIndex == 0

        when:
        model.moveSelection(EntityBrowserDirection.DOWN, 1)
        model.moveSelection(EntityBrowserDirection.DOWN, 1)

        then:
        model.selectedKey == "c"
        model.pageIndex == 1
        model.pageEntries*.key == ["c", "d"]

        when:
        model.moveSelection(EntityBrowserDirection.UP, 1)

        then:
        model.selectedKey == "b"
        model.pageIndex == 0
    }

    def "multi-column traversal handles incomplete final rows"() {
        given:
        def model = model(9)
        model.setEntries(entries("a", "b", "c", "d", "e", "f", "g", "h"))
        model.setSelectedKey("b")

        when:
        model.moveSelection(EntityBrowserDirection.DOWN, 3)
        model.moveSelection(EntityBrowserDirection.DOWN, 3)

        then:
        model.selectedKey == "h"

        when:
        def moved = model.moveSelection(EntityBrowserDirection.DOWN, 3)

        then:
        !moved
        model.selectedKey == "h"

        when: "a missing final-row column clamps to the row's final entity"
        model.setSelectedKey("f")
        model.moveSelection(EntityBrowserDirection.DOWN, 3)

        then:
        model.selectedKey == "h"

        when:
        model.moveSelection(EntityBrowserDirection.UP, 3)

        then:
        model.selectedKey == "e"
    }

    def "selection never activates and explicit activation emits exactly the stable entity"() {
        given:
        def model = model(4)
        model.setEntries(entries("a", "b"))
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

        when:
        model.setSelectedKey("b")
        model.moveSelection(EntityBrowserDirection.LEFT, 1)

        then:
        activations.empty
        model.selectedKey == "a"

        when:
        model.activateSelection()

        then:
        activations.size() == 1
        activations[0][0] == "a"
        activations[0][1].is(model.selectedEntity)
    }

    private static EntityBrowserModel<Entry> model(
            int pageSize,
            Closure<Boolean> matches = { Entry entry, String query ->
                entry.label.contains(query)
            }
    ) {
        new EntityBrowserModel<Entry>(
                { Entry entry -> entry.key } as EntityBrowserKeyProvider<Entry>,
                matches as EntityBrowserMatcher<Entry>,
                pageSize
        )
    }

    private static List<Entry> entries(String... keys) {
        keys.collect { String key -> new Entry(key, key) }
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
