package net.wti.quest.impl


import net.wti.quest.api.*
import net.wti.time.api.DayIndex
import net.wti.time.api.DurationUnit
import net.wti.time.api.ModelDay
import net.wti.time.api.ModelDuration
import net.wti.time.api.TimeAnchor
import net.wti.time.api.TimeAnchorKind
import net.wti.time.impl.DayIndexService
import net.wti.time.impl.ModelDayService
import spock.lang.Specification
import spock.lang.Unroll
import xapi.model.X_Model
import xapi.model.api.ModelKey
import xapi.time.api.TimeZoneInfo

/// TodayPlannerServiceSpec
///
/// Tests orchestration of ensureToday(user):
///  - Uses DayIndexService + ModelDayService to obtain today's ModelDay.
///  - Iterates active definitions & rules.
///  - Applies ScheduleTemplateService.shouldSkip.
///  - Delegates to PlannerService.ensureLiveQuestForDay.
///  - Is idempotent under repeated calls.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:45
class TodayPlannerServiceSpec extends Specification {

    DayIndexService indexService
    ModelDayService dayService
    TimeZoneInfo utcZone

    InMemoryQuestDefinitionSource definitionSource
    InMemoryScheduleTemplateService scheduleService
    PlannerService plannerService
    InMemoryLiveQuestStore liveStore
    TodayPlannerService todayPlannerService

    ModelKey userKey

    def setup() {
        utcZone = new TimeZoneInfo("UTC", "UTC", 0, false)
        indexService = new DayIndexService(utcZone, 4)
        dayService = new ModelDayService(indexService)

        definitionSource = new InMemoryQuestDefinitionSource()
        scheduleService = new InMemoryScheduleTemplateService()
        liveStore = new InMemoryLiveQuestStore()
        plannerService = new PlannerService(liveStore)

        todayPlannerService = new TodayPlannerService(
                indexService,
                dayService,
                definitionSource,
                scheduleService,
                plannerService
        )

        /// Simple synthetic user key
        userKey = X_Model.newKey("user", "u1")
    }

    private static QuestDefinition newQuestDefinition(final String id, final boolean active = true) {
        final QuestDefinition questDefinition = X_Model.create(QuestDefinition)
        questDefinition.setKey(QuestDefinition.KEY_BUILDER_DEF.buildKey(id))
        questDefinition.setName("Quest " + id)
        questDefinition.setActive(active)
        return questDefinition
    }

    private static RecurrenceRule newRule(final String id, final boolean active = true, final boolean auto = true, final int hour = 10, final int minute = 0) {
        final RecurrenceRule rule = X_Model.create(RecurrenceRule)
        rule.setKey(RecurrenceRule.KEY_BUILDER_RULE.buildKey(id))
        rule.setRuleId(id)
        rule.setActive(active)
        rule.setAutoMaterialize(auto)

        final TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(hour)
        anchor.setMinute(minute)
        rule.setAnchor(anchor)

        final ModelDuration duration = X_Model.create(ModelDuration)
        duration.setAmount(1)
        duration.setUnit(DurationUnit.DAY)
        rule.setCadence(duration)

        return rule
    }

    def "ensureToday: creates LiveQuest for each active auto-mat rule"() {
        given:
        final QuestDefinition questDefinition1 = newQuestDefinition("d1", true)
        final QuestDefinition questDefinition2 = newQuestDefinition("d2", true)
        final QuestDefinition questDefinitionInactive = newQuestDefinition("dInactive", false)

        questDefinition1.setRules([newRule("r1a"), newRule("r1b")] as RecurrenceRule[])
        questDefinition2.setRules([newRule("r2a")] as RecurrenceRule[])
        questDefinitionInactive.setRules([newRule("rX")] as RecurrenceRule[])

        definitionSource.definitions = [questDefinition1, questDefinition2, questDefinitionInactive]

        when:
        final List<LiveQuest> results = todayPlannerService.ensureToday(userKey)

        then:
        results.size() == 3  /// 2 from d1, 1 from d2
        liveStore.all.size() == 3
        liveStore.all*.liveKey as Set == (results*.liveKey as Set)
    }

    def "ensureToday: respects ScheduleTemplateService skip flag"() {
        given:
        final QuestDefinition questDefinition = newQuestDefinition("d1", true)
        final RecurrenceRule rule = newRule("r1", true, true)
        questDefinition.setRules([rule] as RecurrenceRule[])
        definitionSource.definitions = [questDefinition]

        and: "ScheduleTemplateService marks this (definition,rule) as skip"
        final String definitionId = questDefinition.key.id.toString()
        scheduleService.skippedPairs << (definitionId + ":" + rule.ruleId)

        when:
        final List<LiveQuest> results = todayPlannerService.ensureToday(userKey)

        then:
        results.size() == 1
        results[0].skip
    }

    def "ensureToday: does not create LiveQuest for inactive rules or auto=false"() {
        given:
        final QuestDefinition questDefinition = newQuestDefinition("d1", true)
        final RecurrenceRule activeAuto = newRule("r1", true, true)
        final RecurrenceRule inactiveRule = newRule("r2", false, true)
        final RecurrenceRule autoOff = newRule("r3", true, false)
        questDefinition.setRules([activeAuto, inactiveRule, autoOff] as RecurrenceRule[])
        definitionSource.definitions = [questDefinition]

        when:
        final List<LiveQuest> results = todayPlannerService.ensureToday(userKey)

        then:
        results.size() == 1
        results[0].liveKey == QuestKeyUtil.liveKeyFor(questDefinition, activeAuto)
    }

    def "ensureToday is idempotent (second call does not create new instances)"() {
        given:
        final QuestDefinition questDefinition = newQuestDefinition("d1", true)
        final RecurrenceRule rule = newRule("r1", true, true)
        questDefinition.setRules([rule] as RecurrenceRule[])
        definitionSource.definitions = [questDefinition]

        when:
        final List<LiveQuest> first = todayPlannerService.ensureToday(userKey)
        final List<LiveQuest> second = todayPlannerService.ensureToday(userKey)

        then:
        first.size() == 1
        second.size() == 1
        first[0].is(second[0])
        liveStore.all.size() == 1
    }

    def "ensureDayForEpoch uses custom zone and rollover"() {
        given:
        final QuestDefinition questDefinition = newQuestDefinition("d1", true)
        final RecurrenceRule rule = newRule("r1", true, true)
        questDefinition.setRules([rule] as RecurrenceRule[])
        definitionSource.definitions = [questDefinition]

        and:
        final TimeZoneInfo est = new TimeZoneInfo("America/New_York", "Eastern", -5 * 3600000, true)
        final long epochNoon = DayIndex.EPOCH_MILLIS + 12 * 3600 * 1000L

        when:
        final List<LiveQuest> results = todayPlannerService.ensureDayForEpoch(userKey, (double) epochNoon, est, 4)

        then:
        results.size() == 1
        /// We do not assert on exact DayIndex here, just that something got created.
        liveStore.all.size() == 1
    }

    @Unroll
    def "ensureDay can be called explicitly for dayIndex #dayNum"() {
        given:
        final QuestDefinition questDefinition = newQuestDefinition("d" + dayNum, true)
        final RecurrenceRule rule = newRule("r" + dayNum, true, true)
        questDefinition.setRules([rule] as RecurrenceRule[])
        definitionSource.definitions = [questDefinition]

        and:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(dayNum as int))

        when:
        final List<LiveQuest> results = todayPlannerService.ensureDay(userKey, day)

        then:
        results.size() == 1
        results[0].dayIndex == dayNum

        where:
        dayNum << [0, 1, -1, 36525, -36525]
    }

    /// ----------------------------------------------------------------------
    /// Test fakes
    /// ----------------------------------------------------------------------

    static class InMemoryQuestDefinitionSource implements QuestDefinitionSource {
        Iterable<QuestDefinition> definitions = Collections.emptyList()

        @Override
        Iterable<QuestDefinition> findDefinitionsForUser(final ModelKey userKey) {
            return definitions
        }
    }

    static class InMemoryScheduleTemplateService implements ScheduleTemplateService {
        /// key format: defId:ruleId
        final Set<String> skippedPairs = new HashSet<>()

        @Override
        boolean shouldSkip(final ModelDay day, final QuestDefinition questDefinition, final RecurrenceRule rule) {
            if (questDefinition == null || rule == null) {
                return false
            }
            final String defId = questDefinition.key.id.toString()
            final String key = defId + ":" + rule.ruleId
            return skippedPairs.contains(key)
        }
    }

    static class InMemoryLiveQuestStore implements LiveQuestStore {

        final List<LiveQuest> all = new ArrayList<>()

        @Override
        LiveQuest findByDayAndLiveKey(final ModelDay day, final String liveKey) {
            return all.find { it.dayIndex == day.dayNum && liveKey == it.liveKey }
        }

        @Override
        LiveQuest createLiveQuest(final ModelDay day, final QuestDefinition questDefinition, final RecurrenceRule rule, final long deadlineMillis, final boolean skip) {
            final LiveQuest liveQuest = X_Model.create(LiveQuest)
            liveQuest.setParentDayKey(ModelDay.newKey(day.dayNum))
            liveQuest.setDayIndex(day.dayNum)
            liveQuest.setLiveKey(QuestKeyUtil.liveKeyFor(questDefinition, rule))
            liveQuest.setSourceDefinitionKey(questDefinition.key)
            if (rule != null) {
                liveQuest.setSourceRuleKey(rule.key)
            }
            liveQuest.setDeadlineMillis(deadlineMillis)
            liveQuest.setSkip(skip)
            liveQuest.setStatus(QuestStatus.ACTIVE)
            final long now = System.currentTimeMillis()
            liveQuest.setCreatedAtMillis(now)
            liveQuest.setUpdatedAtMillis(now)

            all.add(liveQuest)
            return liveQuest
        }

        @Override
        LiveQuest save(final LiveQuest quest) {
            /// In-memory: already in list
            return quest
        }
    }
}
