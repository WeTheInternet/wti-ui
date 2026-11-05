package net.wti.quest.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Authoritative acknowledged daily summaries for exactly one child and routine.
/// firstDay is the enrollment/coverage boundary, not an arbitrary query cutoff.
/// The caller must supply all completed days from firstDay through currentDay.
/// Missing closed days are incomplete; a missing/incomplete current day is pending.
/// Summaries must come from acknowledged domain completion, never UI toggle state.
public final class DailyRoutineHistory {
    private final String childKey, routineKey;
    private final int firstDay, currentDay;
    private final List<DaySummary> days;

    /// Copies the history so evaluation cannot observe later list edits.
    public DailyRoutineHistory(final String childKey, final String routineKey, final int firstDay,
                               final int currentDay, final List<DaySummary> days) {
        this.childKey = childKey;
        this.routineKey = routineKey;
        this.firstDay = firstDay;
        this.currentDay = currentDay;
        this.days = Collections.unmodifiableList(new ArrayList<>(days));
    }

    /// Full child identity, including namespace where applicable.
    public String getChildKey() { return childKey; }
    /// Routine identity whose required completion is summarized.
    public String getRoutineKey() { return routineKey; }
    /// First enrolled day, inclusive.
    public int getFirstDay() { return firstDay; }
    /// Current logical day under the explicitly chosen time settings.
    public int getCurrentDay() { return currentDay; }
    /// Immutable acknowledged summaries; repeated equal day results are allowed.
    public List<DaySummary> getDays() { return days; }

    /// One acknowledged all-required-items completion summary with explicit owner/routine identity.
    /// acknowledgementKey identifies persisted evidence; this value object cannot prove its provenance.
    public static final class DaySummary {
        private final int day;
        private final boolean complete;
        private final String childKey, routineKey, acknowledgementKey;

        /// Copies scalar evidence identity and the acknowledged completion result.
        public DaySummary(final String childKey, final String routineKey, final int day,
                          final boolean complete, final String acknowledgementKey) {
            this.childKey = childKey;
            this.routineKey = routineKey;
            this.day = day;
            this.complete = complete;
            this.acknowledgementKey = acknowledgementKey;
        }

        /// Child owning the acknowledged result.
        public String getChildKey() { return childKey; }
        /// Routine owning the acknowledged result.
        public String getRoutineKey() { return routineKey; }
        /// Logical day of the acknowledged result.
        public int getDay() { return day; }
        /// Whether all routine requirements for this day are complete.
        public boolean isComplete() { return complete; }
        /// Stable identity of acknowledged evidence, not a pending command ID.
        public String getAcknowledgementKey() { return acknowledgementKey; }
    }
}
