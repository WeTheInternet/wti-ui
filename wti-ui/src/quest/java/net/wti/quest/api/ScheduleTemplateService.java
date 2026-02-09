package net.wti.quest.api;

import net.wti.time.api.ModelDay;

/// ScheduleTemplateService
///
/// Computes skip behavior for a given (day, definition, rule).
///
/// This is where you plug in:
///  - Workday / off-day / holiday logic
///  - User PTO / special calendars
///  - Template rules (always-present-with-skip vs not-present)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:44
public interface ScheduleTemplateService {

    /// Returns true if the given (definition, rule) should be skipped on the given day.
    ///
    /// NOTE:
    ///  - "skip" here means: create the LiveQuest with skip=true, OR potentially
    ///    not create it at all if your template policy says "not present".
    ///  - For now, TodayPlannerService always creates a LiveQuest and sets skip
    ///    according to this method. You can adjust that later.
    boolean shouldSkip(ModelDay day, QuestDefinition questDefinition, RecurrenceRule rule);
}
