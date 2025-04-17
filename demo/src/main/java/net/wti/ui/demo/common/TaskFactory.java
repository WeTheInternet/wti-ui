package net.wti.ui.demo.common;

import net.wti.ui.demo.api.*;
import xapi.model.X_Model;
import xapi.model.api.ModelList;
import xapi.time.X_Time;

import java.time.LocalDateTime;
import java.time.Month;

/// TaskManager:
///
/// Helper class to create / mutate / save {@link net.wti.ui.demo.api.ModelTask} objects.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 22:18
public class TaskManager {

    public static final TaskManager INSTANCE = new TaskManager();

    private TaskManager() {}

    public static ModelTask create(final String name) {
        return create(name, "");
    }
    public static ModelTask create(final String name, final String description) {
        return INSTANCE.newTask(name, description);
    }

    ModelTask newTask(final String name, final String description) {
        final ModelTask task = X_Model.create(ModelTask.class);
        task.setBirth(X_Time.now().millisLong());
        task.setName(name);
        task.setDescription(description);
        return task;
    }

    public Double nextTime(final ModelTask task) {
        final ModelList<ModelRecurrence> recurrence = task.getRecurrence();
        if (recurrence == null || recurrence.isEmpty()) {
            return null;
        }

        double nextTime = Double.MAX_VALUE;
        for (ModelRecurrence recur : recurrence) {
            Double next = nextRecurrence(task, recur, nextTime);
            if (next != null) {
                if (next < nextTime) {
                    nextTime = next;
                }
            }
        }
        return nextTime == Double.MAX_VALUE ? null : nextTime;
    }

    private Double nextRecurrence(final ModelTask task, final ModelRecurrence recur, Double limitTime) {
        final RecurrenceUnit unit = recur.getUnit();
        final long value = recur.getValue();
        if (unit == RecurrenceUnit.ONCE) {
            return (double)value;
        }
        Double result = null;
        if (limitTime == null) {
            limitTime = Double.MAX_VALUE;
        }
        Long lastFinished = task.getLastFinished();
        if (lastFinished == null) {
            lastFinished = task.getBirth();
        }
        final boolean neverFinished = lastFinished == task.getBirth();
        final DayOfWeek day = recur.dayOfWeek();
        final int hour = recur.hour();
        final int minute = recur.minute();
        final LocalDateTime moment = LocalDateTime.now();
        DayOfWeek today = dayOf(moment);
        int year = moment.getYear();
        Month month = moment.getMonth();
        int dayOfMonth = moment.getDayOfMonth();
        final Long startOfWeek = toStartOfWeek(moment);
        if (neverFinished) {
            // if the task has never finished, the next recurrence depends on whether that day has passed or not
            if (today.ordinal() <= day.ordinal()) {
                // the task can be this week
                return startOfWeek + (value * 60000.0d);
            } else {
                // the task is for a day of week before today, use next week
                return startOfWeek + ( (value + ModelRecurrence.MINUTES_PER_WEEK) * 60000.0d);
            }
        } else {
            // need to consider the previous finish time to decide which week to start in
            if (lastFinished < (startOfWeek + value)) {
                // the task can be this week
                return startOfWeek + (value * 60000.0d);
            } else {
                // the task is for a day of week before today, use next week
                return startOfWeek  + ( value + ModelRecurrence.MINUTES_PER_WEEK) * 60000.0d;
            }
        }
//        switch (unit) {
//            case WEEKLY:
//                // using
//
//        }
//
//        // pick closest time based on lastFinished
//        if (limitTime > 0) {
//            // soonest time > lastFinished and < limitTime
//
//        } else {
//            // soonest time > lastFinished
//        }
//        return result;
    }

    public static long toStartOfWeek(final LocalDateTime moment) {
        LocalDateTime result;
        switch (moment.getDayOfWeek()) {
            case MONDAY:
                result = moment.minusDays(1);
                break;
            case TUESDAY:
                result = moment.minusDays(2);
                break;
            case WEDNESDAY:
                result = moment.minusDays(3);
                break;
            case THURSDAY:
                result = moment.minusDays(4);
                break;
            case FRIDAY:
                result = moment.minusDays(5);
                break;
            case SATURDAY:
                result = moment.minusDays(6);
                break;
            case SUNDAY:
                result = moment;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + moment.getDayOfWeek());
        }
        result = result.minusHours(moment.getHour()).minusMinutes(moment.getMinute())
                .minusSeconds(moment.getSecond());
        return result.toEpochSecond(ModelSettings.timeZone()) * 1000;
    }

    private DayOfWeek dayOf(LocalDateTime now) {
        if (now.getHour() < 4) {
            // count any time up to 3am as part of the previous day
            now = now.minusHours(4);
        }
        final java.time.DayOfWeek dow = now.getDayOfWeek();
        switch (dow) {
            case MONDAY:
                return DayOfWeek.MONDAY;
            case TUESDAY:
                return DayOfWeek.TUESDAY;
            case WEDNESDAY:
                return DayOfWeek.WEDNESDAY;
            case THURSDAY:
                return DayOfWeek.THURSDAY;
            case FRIDAY:
                return DayOfWeek.FRIDAY;
            case SATURDAY:
                return DayOfWeek.SATURDAY;
            case SUNDAY:
                return DayOfWeek.SUNDAY;
            default:
                throw new IllegalStateException("can't get here");
        }
    }
}
