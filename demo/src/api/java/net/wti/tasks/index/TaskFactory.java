package net.wti.tasks.index;

import net.wti.ui.demo.api.*;
import xapi.model.X_Model;
import xapi.model.api.ModelList;
import xapi.time.X_Time;

import java.time.LocalDateTime;

/// TaskFactory
///
/// Utility to create new {@link ModelTask} instances for demo / injection / testing.
/// Intended to replace inline newTask() code in DemoApp with DRY helpers.
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:12 CST
public class TaskFactory {

    private TaskFactory() {}

    public static ModelTask create(final String name) {
        return create(name, "");
    }

    public static ModelTask create(final String name, final String description) {
        final ModelTask task = X_Model.create(ModelTask.class);
        task.setBirth(X_Time.now().millisLong());
        task.setName(name);
        task.setDescription(description);
        nextTime(task); // sets the deadline on the task, if there is one
        return task;
    }

    /// Computes the next due time for a task, based on its recurrence list
    public static Double nextTime(final ModelTask task) {
        final ModelList<ModelRecurrence> recurrence = task.getRecurrence();
        if (recurrence == null || recurrence.isEmpty()) {
            return null;
        }

        double nextTime = Double.MAX_VALUE;
        for (ModelRecurrence recur : recurrence) {
            Double next = nextRecurrence(task, recur, nextTime);
            if (next != null && next < nextTime) {
                nextTime = next;
            }
        }
        final Double result = nextTime == Double.MAX_VALUE ? null : nextTime;
        task.setDeadline(result);
        return result;
    }

    /// Calculates the next occurrence time for a recurrence
    private static Double nextRecurrence(final ModelTask task, final ModelRecurrence recur, Double limitTime) {
        final RecurrenceUnit unit = recur.getUnit();
        final long value = recur.getValue();

        if (unit == RecurrenceUnit.ONCE) {
            return (double) value;
        }

        if (limitTime == null) {
            limitTime = Double.MAX_VALUE;
        }

        Long lastFinished = task.getLastFinished();
        if (lastFinished == null || lastFinished == 0) {
            lastFinished = task.getBirth();
        }

        final boolean neverFinished = lastFinished.equals(task.getBirth());
        final DayOfWeek day = recur.dayOfWeek();
        final LocalDateTime now = LocalDateTime.now();
        final DayOfWeek today = dayOf(now);
        final Long startOfWeek = toStartOfWeek(now);

        final long targetThisWeek = startOfWeek + (value * 60000L);
        if (neverFinished) {
            // First-time execution: compute based on current weekday position
            final long nowMillis = System.currentTimeMillis();

            // If target is still in the future this week, use it; otherwise next week
            if (targetThisWeek > nowMillis) {
                return (double) targetThisWeek;
            } else {
                return startOfWeek + ((value + ModelRecurrence.MINUTES_PER_WEEK) * 60000.0d);
            }

        } else {
            // Recurrence after previous execution
            if (lastFinished < targetThisWeek) {
                return (double) targetThisWeek;
            } else {
                return startOfWeek + ((value + ModelRecurrence.MINUTES_PER_WEEK) * 60000.0d);
            }
        }
    }

    /// Computes the start of this calendar week (Sunday 00:00)
    public static long toStartOfWeek(final LocalDateTime moment) {
        LocalDateTime result;
        switch (moment.getDayOfWeek()) {
            case MONDAY:    result = moment.minusDays(1); break;
            case TUESDAY:   result = moment.minusDays(2); break;
            case WEDNESDAY: result = moment.minusDays(3); break;
            case THURSDAY:  result = moment.minusDays(4); break;
            case FRIDAY:    result = moment.minusDays(5); break;
            case SATURDAY:  result = moment.minusDays(6); break;
            case SUNDAY:    result = moment; break;
            default:
                throw new IllegalStateException("Unexpected value: " + moment.getDayOfWeek());
        }

        return result
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toEpochSecond(ModelSettings.timeZone()) * 1000;
    }

    /// Applies 4am rule to consider before-4am as previous day
    private static DayOfWeek dayOf(LocalDateTime now) {
        if (now.getHour() < 4) {
            now = now.minusHours(4);
        }
        switch (now.getDayOfWeek()) {
            case MONDAY:    return DayOfWeek.MONDAY;
            case TUESDAY:   return DayOfWeek.TUESDAY;
            case WEDNESDAY: return DayOfWeek.WEDNESDAY;
            case THURSDAY:  return DayOfWeek.THURSDAY;
            case FRIDAY:    return DayOfWeek.FRIDAY;
            case SATURDAY:  return DayOfWeek.SATURDAY;
            case SUNDAY:    return DayOfWeek.SUNDAY;
            default:
                throw new IllegalStateException("Unexpected enum: " + now.getDayOfWeek());
        }
    }
}
