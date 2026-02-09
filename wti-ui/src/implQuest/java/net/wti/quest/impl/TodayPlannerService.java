package net.wti.quest.impl;

import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestDefinition;
import net.wti.quest.api.QuestDefinitionSource;
import net.wti.quest.api.RecurrenceRule;
import net.wti.quest.api.ScheduleTemplateService;
import net.wti.time.api.DayIndex;
import net.wti.time.api.ModelDay;
import net.wti.time.impl.DayIndexService;
import net.wti.time.impl.ModelDayService;
import xapi.model.api.ModelKey;
import xapi.time.api.TimeZoneInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// TodayPlannerService
///
/// High-level orchestration for "Planner.ensureToday(user)".
///
/// Responsibilities:
///  - Resolve today's DayIndex and ModelDay (zone + rollover-aware).
///  - Load relevant QuestDefinitions for a user.
///  - For each active definition and rule:
///      - Ask ScheduleTemplateService whether to skip.
///      - Delegate to PlannerService.ensureLiveQuestForDay.
///
/// This class does NOT:
///  - Talk to storage directly (it uses PlannerService and QuestDefinitionSource).
///  - Decide how user-specific zones/rolloverHours are stored (caller can pass
///    a custom zone/rollover if needed).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:45
public class TodayPlannerService {

    private final DayIndexService dayIndexService;
    private final ModelDayService modelDayService;
    private final QuestDefinitionSource definitionSource;
    private final ScheduleTemplateService scheduleTemplateService;
    private final PlannerService plannerService;

    public TodayPlannerService(
            DayIndexService dayIndexService,
            ModelDayService modelDayService,
            QuestDefinitionSource definitionSource,
            ScheduleTemplateService scheduleTemplateService,
            PlannerService plannerService
    ) {
        if (dayIndexService == null) {
            throw new IllegalArgumentException("dayIndexService must not be null");
        }
        if (modelDayService == null) {
            throw new IllegalArgumentException("modelDayService must not be null");
        }
        if (definitionSource == null) {
            throw new IllegalArgumentException("definitionSource must not be null");
        }
        if (scheduleTemplateService == null) {
            throw new IllegalArgumentException("scheduleTemplateService must not be null");
        }
        if (plannerService == null) {
            throw new IllegalArgumentException("plannerService must not be null");
        }
        this.dayIndexService = dayIndexService;
        this.modelDayService = modelDayService;
        this.definitionSource = definitionSource;
        this.scheduleTemplateService = scheduleTemplateService;
        this.plannerService = plannerService;
    }

    /// Ensures "today" is materialized for the given user using the default
    /// zone and rolloverHour configured in DayIndexService.
    ///
    /// @return List of LiveQuest instances that were created or found.
    public List<LiveQuest> ensureToday(ModelKey userKey) {
        final DayIndex todayIndex = dayIndexService.today();
        final ModelDay today = modelDayService.getOrCreateModelDay(todayIndex);
        return ensureDay(userKey, today);
    }

    /// Ensures a specific day (by epoch millis) is materialized for the given user,
    /// using a custom zone and rolloverHour.
    ///
    /// Useful for "what will my day look like on X date in Y zone?"
    public List<LiveQuest> ensureDayForEpoch(
            ModelKey userKey,
            double epochMillis,
            TimeZoneInfo zone,
            int rolloverHour
    ) {
        DayIndex dayIndex = dayIndexService.computeDayIndex(epochMillis, zone, rolloverHour);
        ModelDay day = modelDayService.getOrCreateModelDay(dayIndex, zone, rolloverHour);
        return ensureDay(userKey, day);
    }

    /// Core entrypoint: given a user and a concrete ModelDay window,
    /// materialize all active rules for that day.
    public List<LiveQuest> ensureDay(ModelKey userKey, ModelDay day) {
        if (userKey == null) {
            throw new IllegalArgumentException("userKey must not be null");
        }
        if (day == null) {
            throw new IllegalArgumentException("day must not be null");
        }

        final Iterable<QuestDefinition> definitions = definitionSource.findDefinitionsForUser(userKey);
        if (definitions == null) {
            return Collections.emptyList();
        }

        final List<LiveQuest> results = new ArrayList<>();

        for (QuestDefinition questDefinition : definitions) {
            if (questDefinition == null) {
                continue;
            }
            if (Boolean.FALSE.equals(questDefinition.getActive())) {
                continue;
            }

            RecurrenceRule[] rules = questDefinition.getRules();
            if (rules == null || rules.length == 0) {
                /// No rules: definition can still be started manually; ensureToday does nothing.
                continue;
            }

            for (RecurrenceRule rule : rules) {
                if (rule == null) {
                    continue;
                }
                /// Inactive / non-autoMaterialize rules are already filtered in PlannerService,
                /// but we can short-circuit here as well.
                if (Boolean.FALSE.equals(rule.getActive())) {
                    continue;
                }
                if (Boolean.FALSE.equals(rule.getAutoMaterialize())) {
                    continue;
                }

                final boolean skip = scheduleTemplateService.shouldSkip(day, questDefinition, rule);

                LiveQuest liveQuest =
                        plannerService.ensureLiveQuestForDay(day, questDefinition, rule, skip);

                if (liveQuest != null) {
                    results.add(liveQuest);
                }
            }
        }

        return results;
    }
}
