package net.wti.model.quest.impl

import net.wti.model.test.AbstractModelLoaderSpec
import net.wti.quest.api.LiveQuest
import net.wti.quest.model.impl.LiveQuestLoaderImpl
import spock.lang.Unroll
import xapi.jre.model.ModelServiceJre
import xapi.model.X_Model
import xapi.model.api.ModelManifest

///
/// LiveQuestLoaderImplSpec:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 12:33
class LiveQuestLoaderImplSpec extends AbstractModelLoaderSpec {

    private static final List<String> QUEST_FIXTURES = [
            "simple-quest.xapi",
            "simple-quest-minimal.xapi",
            "composite-quest.xapi",
            "composite-quest-minimal.xapi",
    ]

    private static final List<String> EXPECTED_PROPERTIES = [
            "parentDayKey",
            "dayIndex",
            "liveKey",
            "sourceDefinitionKey",
            "sourceRuleKey",
            "deadlineMillis",
            "status",
            "alarmDuration",
            "estimatedDuration",
            "snoozeUntilMillis",
            "createdAtMillis",
            "updatedAtMillis",
            "startedAtMillis",
            "finishedAtMillis",
            "effectivePriority",
            "tags",
            "skip",
            "gracePeriodDuration",
            "scheduleTemplateKey",
    ]

    static class ExposedQuestLoader extends LiveQuestLoaderImpl {

        Object manifestFor(final Class type) {
            return findManifest(type)
        }

        String metaInfFor(final String suffix) {
            return metaInfPath(suffix)
        }
    }

    void setupSpec() {
        final ModelServiceJre srv = X_Model.getService() as ModelServiceJre
        srv.register(LiveQuest.class)
        srv.getOrMakeModelManifest(LiveQuest.class)
    }

    def "loader resolves META-INF quests path from suffix"() {
        given:
        def loader = new ExposedQuestLoader()

        expect:
        loader.metaInfFor("models/lv") == "META-INF/models/lv"
    }

    def "loader can resolve LiveQuest manifest via X_Model service"() {
        given:
        def loader = new ExposedQuestLoader()

        expect:
        loader.manifestFor(LiveQuest) != null
    }

    def "LiveQuest manifest has exact expected structure"() {
        given:
        def loader = new ExposedQuestLoader()
        def manifest = loader.manifestFor(LiveQuest) as ModelManifest
        maybePrintManifest(manifest)

        expect: "top-level manifest shape"
        manifest != null
        manifest.type == LiveQuest.MODEL_LIVE_QUEST
        manifest.modelType == LiveQuest
        !manifest.keyOnly

        and: "ordered property list is exact"
        checkLiveQuestStructure(manifest)
    }

    // ... existing code ...

    @Unroll
    def "quest fixture '#fileName' is available on test classpath"() {
        expect:
        fixtureUrl(fileName) != null

        where:
        fileName << QUEST_FIXTURES
    }

    def "fixture names consistently use quest verbiage"() {
        expect:
        QUEST_FIXTURES.every { it.contains("quest") }
        QUEST_FIXTURES.every { !it.contains("task") }
        QUEST_FIXTURES.size() == 4
    }

    def "loads simple-quest.xapi with exact expected quest structure"() {
        given:
        def loaded = loadByFixture()
        def quests = loaded["simple-quest.xapi"] ?: []

        expect:
        quests.size() == 1

        and:
        def q = quests.first()
        q.liveKey == "simple-task"
        q.dayIndex == 1
        q.effectivePriority == 3
        q.deadlineMillis == 1760000400000L
        q.skip == false
        q.scheduleTemplateKey == "daily"
        q.tags != null
        q.tags.toList() == ["health"]
    }

    def "loads simple-quest-minimal.xapi with exact expected minimal structure"() {
        given:
        def loaded = loadByFixture()
        def quests = loaded["simple-quest-minimal.xapi"] ?: []

        expect:
        quests.size() == 1

        and:
        def q = quests.first()
        // minimal fixture contract: exactly one quest, with a non-empty liveKey identity
        q.liveKey != null
        !q.liveKey.trim().isEmpty()

        and: "minimal fixture should not force optional fields"
        q.tags == null || q.tags.length == 0
        q.scheduleTemplateKey == null || q.scheduleTemplateKey.trim().isEmpty()
    }

    def "loads composite-quest.xapi with exact expected hierarchy keys"() {
        given:
        def loaded = loadByFixture()
        def quests = loaded["composite-quest.xapi"] ?: []
        def keys = (quests*.liveKey).findAll { it != null } as Set<String>

        expect:
        quests.size() == 7
        keys == [
                "morning-routine",
                "morning-routine/body-activation",
                "morning-routine/body-activation/stretch",
                "morning-routine/body-activation/breathing",
                "morning-routine/day-planning",
                "morning-routine/day-planning/top-3",
                "morning-routine/day-planning/first-block",
        ] as Set<String>

        and:
        assertQuestPresent(quests, "morning-routine", 1, 6, 1760004000000L, false, "workday", ["daily", "routine"])
        assertQuestPresent(quests, "morning-routine/day-planning/top-3", null, 7, 1760002600000L, null, null, ["planning", "focus"])
    }

    def "loads composite-quest-minimal.xapi with exact expected minimal hierarchy keys"() {
        given:
        def loaded = loadByFixture()
        def quests = loaded["composite-quest-minimal.xapi"] ?: []
        def keys = (quests*.liveKey).findAll { it != null } as Set<String>

        expect:
        quests.size() == 4
        keys == [
                "minimal-composite",
                "minimal-composite/child-a",
                "minimal-composite/child-b",
                "minimal-composite/child-b/grandchild-b1",
        ] as Set<String>
    }

    private static void assertQuestPresent(
            final List<LiveQuest> quests,
            final String liveKey,
            final Integer dayIndex,
            final Integer priority,
            final Long deadlineMillis,
            final Boolean skip,
            final String scheduleTemplateKey,
            final List<String> tags
    ) {
        final LiveQuest q = quests.find { it.liveKey == liveKey }
        assert q != null: "Expected quest not found: ${liveKey}"

        if (dayIndex != null) {
            assert q.dayIndex == dayIndex
        }
        if (priority != null) {
            assert q.effectivePriority == priority
        }
        if (deadlineMillis != null) {
            assert q.deadlineMillis == deadlineMillis
        }
        if (skip != null) {
            assert q.skip == skip
        }
        if (scheduleTemplateKey != null) {
            assert q.scheduleTemplateKey == scheduleTemplateKey
        }
        if (tags != null) {
            assert q.tags != null
            assert q.tags.toList() == tags
        }
    }

    private Map<String, List<LiveQuest>> loadByFixture() {
        def out = [:].withDefault { [] as List<LiveQuest> } as Map<String, List<LiveQuest>>
        def loader = new LiveQuestLoaderImpl()

        loader.loadFromClasspath { String resourceName, LiveQuest quest ->
            out[resourceFileName(resourceName)] << quest
        }

        return out
    }

    private static String resourceFileName(final String resourceName) {
        if (resourceName == null) {
            return null
        }
        int idx = resourceName.lastIndexOf('/')
        return idx < 0 ? resourceName : resourceName.substring(idx + 1)
    }

    private static URL fixtureUrl(final String fileName) {
        final ClassLoader cl = Thread.currentThread().contextClassLoader
        return cl.getResource("META-INF/models/lv/${fileName}")
    }

    private static void maybePrintManifest(final ModelManifest manifest) {
        if (Boolean.getBoolean("test.printManifest")) {
            println manifest.dump()
        }
    }

    private static boolean checkLiveQuestStructure(final ModelManifest manifest) {
        assert manifest != null
        assert manifest.type == LiveQuest.MODEL_LIVE_QUEST
        assert manifest.modelType == LiveQuest
        assert !manifest.keyOnly

        final List<String> actualPropertyNames = safeToStringList(manifest.propertyNames)
        assert actualPropertyNames.size() == EXPECTED_PROPERTIES.size()
        assert actualPropertyNames.containsAll(EXPECTED_PROPERTIES)

        final Set<String> propKeys = safeKeySet(manifest.methodsByPropertyNames)
        assert propKeys.size() == EXPECTED_PROPERTIES.size()
        assert propKeys.containsAll(EXPECTED_PROPERTIES)

        final Set<String> methodKeys = safeKeySet(manifest.methodsByMethodNames)
        for (String prop : EXPECTED_PROPERTIES) {
            final String cap = prop.substring(0, 1).toUpperCase() + prop.substring(1)
            final String getter = ("get" + cap).toString()
            final String setter = ("set" + cap).toString()
            final String booleanGetter = ("is" + cap).toString()

            assert methodKeys.contains(setter)
            assert methodKeys.contains(getter) || methodKeys.contains(booleanGetter)
        }

        return true
    }

    private static List<String> safeToStringList(final Object maybeIterable) {
        final List<String> out = []
        if (maybeIterable == null) {
            return out
        }
        if (maybeIterable instanceof Iterable) {
            for (Object o : (Iterable) maybeIterable) {
                out.add(String.valueOf(o))
            }
            return out
        }
        if (maybeIterable.getClass().isArray()) {
            final int len = xapi.fu.X_Fu.getLength(maybeIterable)
            for (int i = 0; i < len; i++) {
                out.add(String.valueOf(xapi.fu.X_Fu.getValue(maybeIterable, i)))
            }
            return out
        }
        out.add(String.valueOf(maybeIterable))
        return out
    }

    private static Set<String> safeKeySet(final Object maybeMap) {
        assert maybeMap instanceof Map
        return ((Map) maybeMap).keySet().collect { String.valueOf(it) } as Set<String>
    }
}
