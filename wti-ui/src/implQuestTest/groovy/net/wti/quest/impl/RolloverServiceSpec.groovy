package net.wti.quest.impl

import net.wti.quest.api.*
import net.wti.time.api.*
import net.wti.time.impl.DayIndexService
import net.wti.time.impl.ModelDayService
import spock.lang.Specification
import spock.lang.Unroll
import xapi.model.X_Model
import xapi.model.api.ModelKey
import xapi.time.api.TimeZoneInfo

/// RolloverServiceSpec
///
/// Tests for RolloverService:
///  - Fails overdue LiveQuest (deadline + grace < now, skip==false).
///  - Does not fail when skip==true.
///  - Does not fail when no deadline (0 or null).
///  - Deletes LiveQuest after writing QuestFailed.
///  - Calls TodayPlannerService to materialize next day.
///  - Supports helper runRolloverForYesterday.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:50
class RolloverServiceSpec extends Specification {

    DayIndexService indexService
    ModelDayService dayService
    TimeZoneInfo utcZone

    InMemoryRolloverStore rolloverStore
    InMemoryQuestDefinitionSource definitionSource
    InMemoryScheduleTemplateService scheduleService
    PlannerService plannerService
    TodayPlannerService todayPlannerService
    RolloverService rolloverService

    InMemoryLiveQuestStore liveStore

    ModelKey userKey

    def setup() {
        utcZone = new TimeZoneInfo("UTC", "UTC", 0, false)
        indexService = new DayIndexService(utcZone, 4)
        dayService = new ModelDayService(indexService)

        rolloverStore = new InMemoryRolloverStore()
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

        rolloverService = new RolloverService(
                indexService,
                dayService,
                rolloverStore,
                todayPlannerService,
                definitionSource
        )

        userKey = X_Model.newKey("user", "u1")
    }

    private static QuestDefinition newQuestDefinition(final String id) {
        final QuestDefinition questDefinition = X_Model.create(QuestDefinition)
        questDefinition.setKey(QuestDefinition.KEY_BUILDER_DEF.buildKey(id))
        questDefinition.setName("Quest " + id)
        questDefinition.setActive(true)
        return questDefinition
    }

    private static RecurrenceRule newRule(final String id, final int hour, final int minute) {
        final RecurrenceRule rule = X_Model.create(RecurrenceRule)
        rule.setKey(RecurrenceRule.KEY_BUILDER_RULE.buildKey(id))
        rule.setRuleId(id)
        rule.setActive(true)
        rule.setAutoMaterialize(true)

        final TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(hour)
        anchor.setMinute(minute)
        rule.setAnchor(anchor)

        final ModelDuration duration = X_Model.create(ModelDuration)
        duration.setAmount(1)
        duration.setUnit(net.wti.time.api.DurationUnit.DAY)
        rule.setCadence(duration)

        return rule
    }

    private static LiveQuest newLiveQuest(final ModelDay day, final long deadlineMillis, final boolean skip, final Integer graceMinutes) {
        final LiveQuest liveQuest = X_Model.create(LiveQuest)
        final ModelKey parentKey = ModelDay.newKey(day.dayNum)
        final String liveId = "test/quest"

        liveQuest.setParentDayKey(parentKey)
        liveQuest.setDayIndex(day.dayNum)
        liveQuest.setLiveKey(liveId)
        /// Ensure the LiveQuest has a proper ModelKey so QuestFailed.instanceKey can be set.
        liveQuest.setKey(LiveQuest.newKey(parentKey, liveId))

        liveQuest.setDeadlineMillis(deadlineMillis)
        liveQuest.setSkip(skip)
        liveQuest.setGracePeriodMinutes(graceMinutes)
        liveQuest.setStatus(QuestStatus.ACTIVE)
        final long now = System.currentTimeMillis()
        liveQuest.setCreatedAtMillis(now)
        liveQuest.setUpdatedAtMillis(now)
        liveQuest.setTags(new String[0])
        return liveQuest
    }


    def "fails overdue LiveQuest when deadline + grace < now and skip==false"() {
        given:
        final DayIndex fromIndex = DayIndex.of(0)
        final ModelDay fromDay = dayService.getOrCreateModelDay(fromIndex)
        final long deadline = fromDay.endTimestamp() - 60_000L
        final LiveQuest liveQuest = newLiveQuest(fromDay, deadline, false, 1)  /// 1 minute grace
        rolloverStore.liveQuests.add(liveQuest)

        and:
        final long nowMillis = fromDay.endTimestamp() + 2 * 60_000L

        when:
        final List<QuestFailed> failures = rolloverService.runRollover(userKey, fromDay, nowMillis)

        then:
        failures.size() == 1
        rolloverStore.liveQuests.isEmpty()
        rolloverStore.failures.size() == 1
        rolloverStore.failures[0].instanceKey != null
        rolloverStore.failures[0].failureReason.contains("deadline+grace")
    }

    def "does not fail when skip==true even if overdue"() {
        given:
        final DayIndex fromIndex = DayIndex.of(0)
        final ModelDay fromDay = dayService.getOrCreateModelDay(fromIndex)
        final long deadline = fromDay.endTimestamp() - 60_000L
        final LiveQuest liveQuest = newLiveQuest(fromDay, deadline, true, 0)
        rolloverStore.liveQuests.add(liveQuest)

        and:
        final long nowMillis = fromDay.endTimestamp() + 60_000L

        when:
        final List<QuestFailed> failures = rolloverService.runRollover(userKey, fromDay, nowMillis)

        then:
        failures.isEmpty()
        rolloverStore.liveQuests.size() == 1
        rolloverStore.failures.isEmpty()
    }

    def "does not fail when deadlineMillis==0 (no deadline)"() {
        given:
        final DayIndex fromIndex = DayIndex.of(0)
        final ModelDay fromDay = dayService.getOrCreateModelDay(fromIndex)
        final LiveQuest liveQuest = newLiveQuest(fromDay, 0L, false, 0)
        rolloverStore.liveQuests.add(liveQuest)

        and:
        final long nowMillis = fromDay.endTimestamp() + 60_000L

        when:
        final List<QuestFailed> failures = rolloverService.runRollover(userKey, fromDay, nowMillis)

        then:
        failures.isEmpty()
        rolloverStore.liveQuests.size() == 1
        rolloverStore.failures.isEmpty()
    }

    def "does not fail when within deadline + grace window"() {
        given:
        final DayIndex fromIndex = DayIndex.of(0)
        final ModelDay fromDay = dayService.getOrCreateModelDay(fromIndex)
        final long deadline = fromDay.endTimestamp() - 60_000L
        final LiveQuest liveQuest = newLiveQuest(fromDay, deadline, false, 5)  /// 5 minute grace
        rolloverStore.liveQuests.add(liveQuest)

        and:
        final long nowMillis = deadline + 2 * 60_000L

        when:
        final List<QuestFailed> failures = rolloverService.runRollover(userKey, fromDay, nowMillis)

        then:
        failures.isEmpty()
        rolloverStore.liveQuests.size() == 1
        rolloverStore.failures.isEmpty()
    }

    def "runRollover materializes next day via TodayPlannerService"() {
        given:
        final DayIndex fromIndex = DayIndex.of(0)
        final ModelDay fromDay = dayService.getOrCreateModelDay(fromIndex)

        final QuestDefinition questDefinition = newQuestDefinition("d1")
        final RecurrenceRule rule = newRule("r1", 10, 0)
        questDefinition.setRules([rule] as RecurrenceRule[])
        definitionSource.definitions = [questDefinition]

        and:
        final long nowMillis = fromDay.endTimestamp() + 1L

        when:
        final List<QuestFailed> failures = rolloverService.runRollover(userKey, fromDay, nowMillis)

        then:
        failures.isEmpty()
        final ModelDay toDay = dayService.getOrCreateModelDay(DayIndex.of(1))
        /// There should be at least one LiveQuest for day 1 in the planner's store
        liveStore.all.size() >= 1
        liveStore.all*.dayIndex.contains(toDay.dayNum)
    }

    def "runRolloverForYesterday uses DayIndexService.today()"() {
        given:
        /// Compute "yesterday" using same logic as RolloverService.runRolloverForYesterday
        final DayIndex todayIndex = indexService.today()
        final DayIndex fromIndex = DayIndex.of(todayIndex.dayNum - 1)
        final ModelDay yesterday = dayService.getOrCreateModelDay(fromIndex)

        final long deadline = yesterday.endTimestamp() - 60_000L
        final LiveQuest liveQuest = newLiveQuest(yesterday, deadline, false, 0)
        rolloverStore.liveQuests.add(liveQuest)

        and:
        final long nowMillis = yesterday.endTimestamp() + 60_000L

        when:
        final List<QuestFailed> failures = rolloverService.runRolloverForYesterday(userKey, nowMillis)

        then:
        failures.size() == 1
        rolloverStore.liveQuests.isEmpty()
        rolloverStore.failures.size() == 1
    }

    @Unroll
    def "computeGraceMillis uses liveQuest.gracePeriodMinutes=#minutes"() {
        given:
        final DayIndex fromIndex = DayIndex.of(0)
        final ModelDay fromDay = dayService.getOrCreateModelDay(fromIndex)
        final LiveQuest liveQuest = newLiveQuest(fromDay, fromDay.endTimestamp(), false, minutes)

        when:
        final long graceMillis = rolloverService.computeGraceMillis(liveQuest, fromDay)

        then:
        graceMillis == expectedMillis

        where:
        minutes || expectedMillis
        null    || 0L
        0       || 0L
        -5      || 0L
        1       || 60_000L
        10      || 10L * 60_000L
    }

    /// ----------------------------------------------------------------------
    /// Test fakes
    /// ----------------------------------------------------------------------

    static class InMemoryRolloverStore implements RolloverStore {

        final List<LiveQuest> liveQuests = new ArrayList<>()
        final List<QuestFailed> failures = new ArrayList<>()

        @Override
        List<LiveQuest> findActiveLiveQuests(final ModelDay day) {
            return new ArrayList<>(liveQuests.findAll { it.dayIndex == day.dayNum })
        }

        @Override
        QuestFailed createFailureRecord(final LiveQuest liveQuest, final RolloverContext context, final String failureReason) {
            final QuestFailed failed = X_Model.create(QuestFailed)
            failed.setInstanceKey(liveQuest.key)
            failed.setSourceDefinitionKey(liveQuest.sourceDefinitionKey)
            failed.setSourceRuleKey(liveQuest.sourceRuleKey)
            failed.setDayIndex(context.fromDay.dayNum)
            failed.setOccurredAtMillis(context.nowMillis)
            failed.setFailureReason(failureReason)

            final QuestSnapshot snapshot = X_Model.create(QuestSnapshot)
            snapshot.setName("snapshot")
            snapshot.setPriority(liveQuest.effectivePriority)
            snapshot.setTags(liveQuest.tags)
            failed.setSnapshot(snapshot)

            failures.add(failed)
            return failed
        }

        @Override
        void deleteLiveQuest(final LiveQuest liveQuest) {
            liveQuests.remove(liveQuest)
        }
    }

    static class InMemoryQuestDefinitionSource implements QuestDefinitionSource {
        Iterable<QuestDefinition> definitions = Collections.emptyList()

        @Override
        Iterable<QuestDefinition> findDefinitionsForUser(final ModelKey userKey) {
            return definitions
        }
    }

    static class InMemoryScheduleTemplateService implements ScheduleTemplateService {
        @Override
        boolean shouldSkip(final ModelDay day, final QuestDefinition questDefinition, final RecurrenceRule rule) {
            return false
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
            return quest
        }
    }
}
