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
import xapi.model.X_Model
import xapi.time.api.TimeZoneInfo

/// PlannerServiceSpec
///
/// Tests for PlannerService.ensureLiveQuestForDay:
/// - Does nothing when definition or rule inactive
/// - Respects rule.autoMaterialize flag
/// - Reuses existing LiveQuest for same (day, LiveKey)
/// - Computes deadlineMillis from TimeAnchor + ModelDay
/// - Sets basic fields (liveKey, dayIndex, parentDayKey, skip, status)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:04
class PlannerServiceSpec extends Specification {

    PlannerService planner
    InMemoryLiveQuestStore store

    ModelDayService dayService
    DayIndexService indexService
    TimeZoneInfo utcZone

    def setup() {
        utcZone = new TimeZoneInfo("UTC", "UTC", 0, false)
        indexService = new DayIndexService(utcZone, 4)
        dayService = new ModelDayService(indexService)

        store = new InMemoryLiveQuestStore()
        planner = new PlannerService(store)
    }

    private static QuestDefinition newDefinition(final String id = "def1", final boolean active = true) {
        final QuestDefinition qdef = X_Model.create(QuestDefinition)
        qdef.setKey(QuestDefinition.KEY_BUILDER_DEF.buildKey(id))
        qdef.setName("Test Quest " + id)
        qdef.setActive(active)
        return qdef
    }

    private static RecurrenceRule newRule(final String id = "rule1", final boolean active = true, final boolean auto = true, final int hour = 10, final int minute = 0) {
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

        // Optional cadence; not used by PlannerService yet.
        final ModelDuration dur = X_Model.create(ModelDuration)
        dur.setAmount(1)
        dur.setUnit(DurationUnit.DAY)
        rule.setCadence(dur)

        return rule
    }

    def "does nothing when QuestDefinition is inactive"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def-inactive", false)
        final RecurrenceRule rule = newRule()

        when:
        final def result = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        then:
        result == null
        store.all.isEmpty()
    }

    def "does nothing when RecurrenceRule is inactive"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def1", true)
        final RecurrenceRule rule = newRule("rule-inactive", false, true)

        when:
        final def result = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        then:
        result == null
        store.all.isEmpty()
    }

    def "does nothing when autoMaterialize is false"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def1", true)
        final RecurrenceRule rule = newRule("rule-auto-off", true, false)

        when:
        final def result = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        then:
        result == null
        store.all.isEmpty()
    }

    def "creates new LiveQuest when none exists"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def-new", true)
        final RecurrenceRule rule = newRule("rule-new", true, true, 9, 30)

        when:
        final LiveQuest lv = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        then: "Exactly one LiveQuest is created in store"
        store.all.size() == 1
        store.all[0].is(lv)

        and: "Keying + basic fields are set"
        lv.dayIndex == day.getDayNum()
        lv.parentDayKey == ModelDay.newKey(day.getDayNum())
        lv.sourceDefinitionKey == qdef.key
        lv.sourceRuleKey == rule.key
        lv.liveKey == QuestKeyUtil.liveKeyFor(qdef, rule)
        lv.status == QuestStatus.ACTIVE
        !lv.skip
    }

    def "reuse existing LiveQuest for same day + LiveKey"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def1", true)
        final RecurrenceRule rule = newRule("rule1", true, true)

        and: "First call creates a LiveQuest"
        final LiveQuest first = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        when: "Second call for same inputs"
        final LiveQuest second = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        then:
        first.is(second)
        store.all.size() == 1
    }

    def "applies skip flag on creation"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def-skip", true)
        final RecurrenceRule rule = newRule("rule-skip", true, true)

        when:
        final LiveQuest lv = planner.ensureLiveQuestForDay(day, qdef, rule, true)

        then:
        lv.skip
    }

    def "computes deadlineMillis from TimeAnchor and ModelDay"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def-deadline", true)
        final RecurrenceRule rule = newRule("rule-deadline", true, true, 10, 15)

        when:
        final LiveQuest lv = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        then: "deadlineMillis is start + 10h15m"
        final long expectedOffset = (10 * 60L + 15L) * 60_000L
        lv.deadlineMillis == day.startTimestamp() + expectedOffset
    }

    def "handles null rule by creating manual LiveQuest with no deadline"() {
        given:
        final ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        final QuestDefinition qdef = newDefinition("def-manual", true)
        final RecurrenceRule rule = null

        when:
        final LiveQuest lv = planner.ensureLiveQuestForDay(day, qdef, rule, false)

        then:
        lv != null
        lv.deadlineMillis == 0L
        lv.sourceDefinitionKey == qdef.key
        lv.sourceRuleKey == null
    }

}