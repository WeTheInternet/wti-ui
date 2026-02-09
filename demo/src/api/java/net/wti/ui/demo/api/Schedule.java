package net.wti.ui.demo.api;

import net.wti.tasks.index.TaskFactory;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelList;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.time.api.TimeComponents;

import java.util.*;
import java.util.stream.Collectors;


/// # Schedule
///
/// High-level abstraction for summarizing and grouping recurrence rules.
///
/// This class parses a task's `ModelRecurrence` entries and produces
/// human-friendly, long-form recurrence descriptions suitable for display.
///
/// ### ✅ Key Features
/// - Groups recurring rules by unit + time
/// - Renders stable, readable user strings
/// - Automatically re-evaluates when task changes
/// - Fills in `ModelRecurrence.setRendered(...)` for precomputed rendering
/// - Supports ONCE + mixed-mode rules
/// - Properly handles WEEKLY / BIWEEKLY / TRIWEEKLY / MONTHLY / YEARLY
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 21/04/2025 @ 00:37 CST
public class Schedule {

    private static final String nonRepeating = "Does not repeat";
    private final ModelTask task;
    private final ModelList<ModelRecurrence> source;
    private final List<String> longDescriptions = new ArrayList<>();
    private Long nextDueMillis;
    private String shortDescription;

    private boolean dirty = true;

    public Schedule(ModelTask task) {
        this.task = task;
        this.source = task.recurrence();

        // Automatically invalidate when model changes
        this.task.onChange("updated", (before, after) -> this.dirty = true);

        build(); // Compute once
    }

    /// Public accessor for UI rendering
    public List<String> getLongDescriptions() {
        if (dirty) {
            build();
        }
        return longDescriptions;
    }

    /// True if task has only a single ONCE rule or nothing at all
    public boolean isOnceOnly() {
        if (isEmpty()) return true;

        for (ModelRecurrence recur : source) {
            if (recur.getUnit() != RecurrenceUnit.ONCE) {
                return false;
            }
        }
        return true;
    }

    /// No recurrence entries at all
    public boolean isEmpty() {
        return source == null || source.isEmpty();
    }

    /// External hook to reset dirty flag
    public void invalidate() {
        dirty = true;
    }

    /// Main entry point for internal rebuild
    private void build() {
        if (!dirty) return;
        dirty = false;
        longDescriptions.clear();
        nextDueMillis = null;
        shortDescription = null;

        if (isEmpty()) {
            longDescriptions.add(nonRepeating);
            shortDescription = "";
            return;
        }

        boolean hasNonOnce = source.anyMatch(r -> r.getUnit() != RecurrenceUnit.ONCE);

        // Group recurrences into enum buckets
        EnumMap<RecurrenceUnit, List<ModelRecurrence>> unitMap = new EnumMap<>(RecurrenceUnit.class);

        for (ModelRecurrence recur : source) {
            // Skip ONCE recurrences if others exist
            if (hasNonOnce && recur.getUnit() == RecurrenceUnit.ONCE) continue;

            unitMap.computeIfAbsent(recur.getUnit(), k -> new ArrayList<>()).add(recur);
        }

        for (Map.Entry<RecurrenceUnit, List<ModelRecurrence>> entry : unitMap.entrySet()) {
            RecurrenceUnit unit = entry.getKey();
            List<ModelRecurrence> list = entry.getValue();

            // Group by hour/minute
            Map<String, List<ModelRecurrence>> byTime = list.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.hour() + ":" + r.minute(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (Map.Entry<String, List<ModelRecurrence>> timeEntry : byTime.entrySet()) {
                List<ModelRecurrence> recurrences = timeEntry.getValue();
                ModelRecurrence head = recurrences.get(0);
                int hour = head.hour();
                int minute = head.minute();

                String description = renderRecurrence(unit, recurrences, hour, minute);
                longDescriptions.add(description);

                // Fill in rendered field for clients
                for (ModelRecurrence r : recurrences) {
                    r.setRendered(description);
                }
            }
        }

        // If all recurrences were ONCE, but with specific timestamps
        if (!hasNonOnce) {
            for (ModelRecurrence r : source) {
                if (r.getUnit() == RecurrenceUnit.ONCE && r.getValue() != 0) {
                    long delta = r.getValue() - System.currentTimeMillis();
                    String str = delta < 0 ?
                            "Overdue by " + X_Time.print(-delta) :
                            "Within " + X_Time.print(delta);
                    longDescriptions.set(0, str); // overwrite default "Once only"
                    r.setRendered(str);
                }
            }
        }

        nextDueMillis = TaskFactory.nextTime(task).longValue();
    }

    /// Builds a readable sentence from recurrence rules with same unit/time
    private String renderRecurrence(RecurrenceUnit unit, List<ModelRecurrence> entries, int hour, int minute) {
        String time = X_String.formatTime(hour, minute);

        switch (unit) {
            case ONCE:
                return nonRepeating;
            case DAILY:
                return "Every day at " + time;
            case WEEKLY:
            case BIWEEKLY:
            case TRIWEEKLY: {
                List<DayOfWeek> days = sortedDays(entries);
                String label = days.stream().map(this::capitalize).collect(Collectors.joining(", "));
                switch (unit) {
                    case WEEKLY:
                        return "Weekly on " + label + " at " + time;
                    case BIWEEKLY:
                        return "Every 2nd week on " + label + " at " + time;
                    case TRIWEEKLY:
                        return "Every 3rd week on " + label + " at " + time;
                    default:
                        return ""; // unreachable
                }
            }
            case MONTHLY: {
                int size = entries.size();
                int[] days = new int[size];
                for (int i = 0; i < size; i++) {
                    days[i] = (int)(entries.get(i).getValue() / ModelRecurrence.MINUTES_PER_DAY);
                }

                Arrays.sort(days);
                String label = X_String.ordinalJoin(days);
                return "Monthly on the " + label + " at " + time;
            }
            case YEARLY: {
                List<Integer> days = entries.stream()
                        .map(r -> (int)(r.getValue() / ModelRecurrence.MINUTES_PER_DAY))
                        .sorted()
                        .collect(Collectors.toList());
                StringBuilder sb = new StringBuilder("Yearly on ");
                for (int i = 0; i < days.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(TimeUtils.dateOfDayOfYear(days.get(i)));
                }
                sb.append(" at ").append(time);
                return sb.toString();
            }
            default:
                return unit.name() + " on " + time;
        }
    }

    /// Capitalizes a string: MONDAY → Monday
    private String capitalize(Enum<?> val) {
        String name = val.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    /// Sorts day-of-week values relative to today
    private List<DayOfWeek> sortedDays(List<ModelRecurrence> list) {
        int today = new TimeComponents(X_Time.nowMillis(), ModelSettings.timeZone()).getDayOfWeek();
        return list.stream()
                .map(ModelRecurrence::dayOfWeek)
                .sorted(Comparator.comparingInt(d -> (d.ordinal() + 6 - today) % 7))
                .distinct()
                .collect(Collectors.toList());
    }

    public Long getNextDueMillis() {
        if (dirty) {
            build();
        }
        return nextDueMillis;
    }

    public String getShortDescription() {
        if (dirty) {
            build();
        }
        return shortDescription != null ? shortDescription : (longDescriptions.isEmpty() ? nonRepeating : longDescriptions.get(0));
    }

    public ModelTask getTask() {
        return task;
    }

    public ModelKey getKey() {
        return getTask().getKey();
    }
}