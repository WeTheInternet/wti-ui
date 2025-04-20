package net.wti.ui.demo.api;

import xapi.model.api.ModelList;
import xapi.time.X_Time;

import java.util.*;
import java.util.stream.Collectors;

/// # Schedule
///
/// Provides a higher-order abstraction over a task's recurrence rules.
/// Groups compatible `ModelRecurrence` entries and generates long-form descriptions
/// for use in expanded task UIs or visual calendars.
///
/// ### ✅ Features
/// - 『 ✓ 』 Groups compatible recurrence rules (unit + hour + minute)
/// - 『 ✓ 』 Memoizes rendered strings into each `ModelRecurrence`
/// - 『 ✓ 』 Automatically recomputes if task `.updated` field changes
/// - 『 ✓ 』 Client-friendly: time rendering avoids `java.time`
/// - 『 ✓ 』 Will support user timezone via `DemoSettingsModel`
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 21/04/2025 @ 02:19 CST
public class Schedule {

    private final ModelTask task;
    private final ModelList<ModelRecurrence> source;
    private final List<String> longDescriptions = new ArrayList<>();
    private boolean dirty = true;

    public Schedule(ModelTask task) {
        this.task = task;
        this.source = task.recurrence();

        // Reactively invalidate when recurrence model is updated
        task.onChange("updated", (before, after) -> this.dirty = true);

        build(); // eagerly compute at least once
    }

    /// Returns the list of long-form recurrence descriptions.
    /// Rebuilds if `dirty` flag is set.
    public List<String> getLongDescriptions() {
        if (dirty) {
            build();
        }
        return longDescriptions;
    }

    /// Determines if this task has no recurrence entries.
    public boolean isEmpty() {
        return source == null || source.isEmpty();
    }

    /// Determines if this task is a one-time event.
    public boolean isOnceOnly() {
        if (isEmpty()) return true;

        boolean once = true;
        for (ModelRecurrence r : source) {
            if (r.getUnit() != RecurrenceUnit.ONCE) return false;
            if (once) once = false;
            else return false;
        }
        return true;
    }

    /// Allows external code to force recomputation.
    public void invalidate() {
        this.dirty = true;
    }

    /// Computes merged recurrence descriptions and memoizes into models.
    private void build() {
        if (!dirty) return;

        longDescriptions.clear();
        dirty = false;

        // Group by (unit + hour + minute)
        Map<String, List<ModelRecurrence>> grouped = new LinkedHashMap<>();
        for (ModelRecurrence recur : source) {
            String key = groupKey(recur);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(recur);
        }

        for (Map.Entry<String, List<ModelRecurrence>> entry : grouped.entrySet()) {
            ModelRecurrence first = entry.getValue().get(0);

            // Only recompute if not memoized
            String rendered = first.getProperty("rendered");
            if (rendered == null || rendered.trim().isEmpty()) {
                RecurrenceUnit unit = first.getUnit();

                if (unit == RecurrenceUnit.ONCE) {
                    final long value = first.getValue();
                    if (value == 0L) {
                        first.setRendered("Once (any time)");
                    } else {
                        // Format the long timestamp into a string via X_Time
                        first.setRendered("Once on " + X_Time.print(value));
                    }
                    continue;
                }

                int hour = first.hour();
                int minute = first.minute();

                List<DayOfWeek> days = entry.getValue().stream()
                        .map(ModelRecurrence::dayOfWeek)
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .collect(Collectors.toList());

                rendered = renderRecurrence(unit, days, hour, minute);
                // Memoize
                for (ModelRecurrence r : entry.getValue()) {
                    r.setProperty("rendered", rendered);
                }
            }

            longDescriptions.add(rendered);
        }
    }

    /// Grouping key for recurrence merging (unit + hour + minute).
    private String groupKey(ModelRecurrence r) {
        return r.getUnit().name() + "-" + r.hour() + ":" + r.minute();
    }

    /// Builds a friendly human-readable string for a group of recurrences.
    ///
    /// This string will be stored in `ModelRecurrence.setProperty("rendered", ...)`
    /// and is client-friendly (no timezone awareness unless explicitly added).
    private String renderRecurrence(RecurrenceUnit unit, List<DayOfWeek> days, int hour, int minute) {
        final String time = formatTime(hour, minute);
        final String dayStr = days.stream()
                .map(day -> capitalize(day.name()))
                .collect(Collectors.joining(", "));

        switch (unit) {
            case ONCE:
                throw new IllegalStateException("Cannot use renderRecurrence for ONCE type recurrences");
            case DAILY:
                return "Every day at " + time;
            case WEEKLY:
                return "Weekly on " + dayStr + " at " + time;
            case BIWEEKLY:
                return "Every 2nd week on " + dayStr + " at " + time;
            case TRIWEEKLY:
                return "Every 3rd week on " + dayStr + " at " + time;
            case MONTHLY:
                return "Monthly on " + dayStr + " at " + time;
            case YEARLY:
                return "Yearly on " + dayStr + " at " + time;
            default:
                return unit.name() + " on " + dayStr + " at " + time;
        }
    }

    /// Returns a string like `7:00` or `16:45`
    /// TODO: Move into `X_Time.formatTime(...)` once localization is added.
    private static String formatTime(int hour, int minute) {
        return hour + ":" + String.format("%02d", minute);
    }

    /// Capitalizes a string: "MONDAY" -> "Monday"
    private static String capitalize(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}