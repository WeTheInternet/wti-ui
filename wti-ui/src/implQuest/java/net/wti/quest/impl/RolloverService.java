package net.wti.quest.impl;

import net.wti.quest.api.*;
import net.wti.time.api.DayIndex;
import net.wti.time.api.ModelDay;
import net.wti.time.impl.DayIndexService;
import net.wti.time.impl.ModelDayService;
import xapi.model.api.ModelKey;

import java.util.ArrayList;
import java.util.List;

/// RolloverService
///
/// Performs post-rollover processing:
///  - For each LiveQuest in the closing day:
///      - If deadlineMillis > 0 and skip == false and now > deadline + grace:
///          - Write QuestFailed record
///          - Delete LiveQuest
///      - Otherwise: leave LiveQuest as-is (including skip==true).
///
///  - Then materializes the new day for the user via TodayPlannerService.
///
/// Assumptions:
///  - Per-user zone/rollover are handled by the caller when constructing
///    the ModelDay objects and/or DayIndexService.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:49
public class RolloverService {

    private final DayIndexService dayIndexService;
    private final ModelDayService modelDayService;
    private final RolloverStore rolloverStore;
    private final TodayPlannerService todayPlannerService;
    private final QuestDefinitionSource definitionSource;

    public RolloverService(
            DayIndexService dayIndexService,
            ModelDayService modelDayService,
            RolloverStore rolloverStore,
            TodayPlannerService todayPlannerService,
            QuestDefinitionSource definitionSource
    ) {
        if (dayIndexService == null) {
            throw new IllegalArgumentException("dayIndexService must not be null");
        }
        if (modelDayService == null) {
            throw new IllegalArgumentException("modelDayService must not be null");
        }
        if (rolloverStore == null) {
            throw new IllegalArgumentException("rolloverStore must not be null");
        }
        if (todayPlannerService == null) {
            throw new IllegalArgumentException("todayPlannerService must not be null");
        }
        if (definitionSource == null) {
            throw new IllegalArgumentException("definitionSource must not be null");
        }
        this.dayIndexService = dayIndexService;
        this.modelDayService = modelDayService;
        this.rolloverStore = rolloverStore;
        this.todayPlannerService = todayPlannerService;
        this.definitionSource = definitionSource;
    }

    /// Runs rollover from the given "from" day to its next day, using nowMillis
    /// as the current time for deadline/grace comparisons.
    ///
    /// @param userKey   User for whom to run rollover.
    /// @param fromDay   Day being closed out.
    /// @param nowMillis Current time in epoch millis (usually just after rollover).
    ///
    /// @return List of QuestFailed records produced by this rollover.
    public List<QuestFailed> runRollover(ModelKey userKey, ModelDay fromDay, long nowMillis) {
        if (userKey == null) {
            throw new IllegalArgumentException("userKey must not be null");
        }
        if (fromDay == null) {
            throw new IllegalArgumentException("fromDay must not be null");
        }

        DayIndex fromIndex = fromDay.dayIndex();
        DayIndex toIndex = DayIndex.of(fromIndex.getDayNum() + 1);
        ModelDay toDay = modelDayService.getOrCreateModelDay(toIndex, fromDay.zone(), fromDay.rolloverHour());

        RolloverContext context = new RolloverContext(fromDay, toDay, nowMillis);

        List<QuestFailed> failures = failOverdueLiveQuests(context);

        /// After closing out "from" day, ensure materialization for "to" day.
        todayPlannerService.ensureDay(userKey, toDay);

        return failures;
    }

    /// Fails overdue LiveQuest instances for the "from" day in the context.
    protected List<QuestFailed> failOverdueLiveQuests(RolloverContext context) {
        ModelDay fromDay = context.getFromDay();
        long nowMillis = context.getNowMillis();

        List<LiveQuest> liveQuests = rolloverStore.findActiveLiveQuests(fromDay);
        List<QuestFailed> failures = new ArrayList<>();

        for (LiveQuest liveQuest : liveQuests) {
            if (liveQuest == null) {
                continue;
            }
            Long deadlineValue = liveQuest.getDeadlineMillis();
            if (deadlineValue == null || deadlineValue <= 0L) {
                /// No deadline => never auto-fails on rollover.
                continue;
            }
            Boolean skipFlag = liveQuest.getSkip();
            if (Boolean.TRUE.equals(skipFlag)) {
                /// Explicitly skipped => do not fail.
                continue;
            }

            long graceMillis = computeGraceMillis(liveQuest, fromDay);
            long failThreshold = deadlineValue + graceMillis;

            if (nowMillis > failThreshold) {
                QuestFailed failure = rolloverStore.createFailureRecord(
                        liveQuest,
                        context,
                        "deadline+grace exceeded during rollover"
                );
                failures.add(failure);
                rolloverStore.deleteLiveQuest(liveQuest);
            }
        }

        return failures;
    }

    /// Computes grace period in millis for a LiveQuest.
    ///
    /// CURRENT BEHAVIOR:
    ///  - If LiveQuest.gracePeriodMinutes is non-null, use that.
    ///  - Otherwise, 0 (no grace).
    ///
    /// TODO: Integrate user/definition defaults when those policies are defined.
    protected long computeGraceMillis(LiveQuest liveQuest, ModelDay fromDay) {
        Integer graceMinutes = liveQuest.getGracePeriodMinutes();
        if (graceMinutes == null) {
            /// Future: inspect QuestDefinition or user settings for default grace.
            return 0L;
        }
        if (graceMinutes <= 0) {
            return 0L;
        }
        return graceMinutes.longValue() * 60_000L;
    }

    /// Helper for callers that do not have a ModelDay instance and want to run
    /// rollover for "yesterday" relative to the default DayIndexService config.
    public List<QuestFailed> runRolloverForYesterday(ModelKey userKey, long nowMillis) {
        DayIndex todayIndex = dayIndexService.today();
        DayIndex fromIndex = DayIndex.of(todayIndex.getDayNum() - 1);
        ModelDay fromDay = modelDayService.getOrCreateModelDay(fromIndex, dayIndexService.getDefaultZone(), dayIndexService.getDefaultRolloverHour());
        return runRollover(userKey, fromDay, nowMillis);
    }
}
