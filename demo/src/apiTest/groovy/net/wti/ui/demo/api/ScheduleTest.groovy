package net.wti.ui.demo.api

import spock.lang.Specification
import spock.lang.Unroll
import xapi.model.X_Model

///
/// # ScheduleTest
///
/// Validates the behavior of [`Schedule`](#Schedule) for grouping,
/// formatting, and explaining recurrence rules attached to a task.
///
/// Uses a test DSL for readability:
///
/// - `once(...)`      — ONCE recurrence
/// - `daily(...)`     — DAILY recurrence
/// - `weekly(...)`    — WEEKLY recurrence
/// - `biweekly(...)`  — BIWEEKLY recurrence
/// - `triweekly(...)` — TRIWEEKLY recurrence
/// - `monthly(...)`   — MONTHLY recurrence (1-31)
/// - `yearly(...)`    — YEARLY recurrence (day-of-year + time)
///
/// ❗ This test suite is comprehensive. Failures should be expected during development.
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 19/04/2025 @ 22:32 CST
///
class ScheduleTest extends Specification {

    // --------------------------------------------------------------------------------
    // ONCE
    // --------------------------------------------------------------------------------

    @Unroll
    def "ONCE: #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        description                   | recurrences                 | expected
        "Single ONCE"                | [once()]                    | ["Within 11h59m"]
        "Multiple ONCEs"             | [once(), once()]            | ["Within 11h59m"]
        "ONCE + daily"               | [once(), daily(10, 0)]      | ["Every day at 10:00"]
        "ONCE + empty"               | []                          | ["Once only"]
        "ONCE unlimited"            | [onceUnlimited()]           | ["Once only"]
    }

    /// These are test cases which currently do a "fuzzy match" via .startsWith
    /// These tests should graduate to the main case above, once we have hammered out
    /// the precise rules for rendering multiple recurrences together.
    @Unroll
    def "ONCE (fuzzy match): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions.stream().allMatch { it.startsWith(expectedPrefix)}

        where:
        description                 | recurrences                 | expectedPrefix
        "ONCE limited"              | [once(2, 30)]               | "Within"
        "ONCE overdue"              | [oncePast()]                | "Overdue by"
    }


    // --------------------------------------------------------------------------------
    // DAILY
    // --------------------------------------------------------------------------------

    @Unroll
    def "DAILY: #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        description                         | recurrences                       | expected
        "8:30 every day"                   | [daily(8, 30)]                    | ["Every day at 8:30"]
        "Identical daily merged"          | [daily(9, 0), daily(9, 0)]        | ["Every day at 9:00"]
        "Two distinct times"              | [daily(8, 0), daily(14, 0)]       | ["Every day at 8:00", "Every day at 14:00"]
    }

    // --------------------------------------------------------------------------------
    // WEEKLY
    // --------------------------------------------------------------------------------

    @Unroll
    def "WEEKLY: #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        description                          | recurrences                                                 | expected
        "Monday 9am"                        | [weekly(DayOfWeek.MONDAY, 9, 0)]                            | ["Weekly on Monday at 9:00"]
        "Monday AM, PM"                    | [weekly(DayOfWeek.MONDAY, 9, 0), weekly(DayOfWeek.MONDAY, 17, 0)] | ["Weekly on Monday at 9:00", "Weekly on Monday at 17:00"]
    }

    @Unroll
    def "WEEKLY (fuzzy match): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions.each {
            assert it.startsWith(expectedPrefix)
        }

        where:
        description                          | recurrences
        "Multiple days, same time"          | [weekly(DayOfWeek.MONDAY, 9, 0), weekly(DayOfWeek.FRIDAY, 9, 0)]
        expectedPrefix = "Weekly on"
    }

    // --------------------------------------------------------------------------------
    // BIWEEKLY
    // --------------------------------------------------------------------------------

    @Unroll
    def "BIWEEKLY: #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        description                         | recurrences                                                                     | expected
        "Tue AM, PM"                      | [biweekly(DayOfWeek.TUESDAY, 0, 8, 0), biweekly(DayOfWeek.TUESDAY, 0, 9, 30)]     | ["Every 2nd week on Tuesday at 8:00", "Every 2nd week on Tuesday at 9:30"]
    }

    @Unroll
    def "BIWEEKLY (fuzzy match): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions.each {
            assert it.startsWith(expectedPrefix)
        }

        where:
        description                   | recurrences
        "Tue + Thu 18:15"            | [biweekly(DayOfWeek.TUESDAY, 0, 18, 15), biweekly(DayOfWeek.THURSDAY, 0, 18, 15)]
        expectedPrefix = "Every 2nd week on"
    }


    // --------------------------------------------------------------------------------
    // MONTHLY
    // --------------------------------------------------------------------------------

    @Unroll
    def "MONTHLY: #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        description                              | recurrences                                     | expected
        "Monthly on 5th + 15th"                 | [monthly(5, 12, 0), monthly(15, 12, 0)]         | ["Monthly on the 5th and 15th at 12:00"]
        "Same day, diff times"                 | [monthly(12, 10, 0), monthly(12, 18, 30)]       | ["Monthly on the 12th at 10:00", "Monthly on the 12th at 18:30"]
    }

    // --------------------------------------------------------------------------------
    // YEARLY
    // --------------------------------------------------------------------------------

    @Unroll
    def "YEARLY (fuzzy match): #description"() {
        expect:
        def descriptions = schedule(newTask(recurrences)).longDescriptions
        for (int i = 0; i < descriptions.size(); i++) {
            assert descriptions[i].startsWith(expectedPrefixes[i])
        }

        where:
        description                                 | recurrences                                                           | expectedPrefixes
        "Single yearly"                            | [yearly(100, 10, 0)]                                                  | ["Yearly on April the 10th at 10:00"]
        "Multiple same-time days"                  | [yearly(100, 10, 0), yearly(200, 10, 0)]                              | ["Yearly on April the 10th, July the 19th at 10:00"]
        "Multiple different times"                 | [yearly(50, 9, 0), yearly(365, 17, 15)]                               | ["Yearly on February the 19th at 9:00", "Yearly on December the 31st at 17:15"]
    }

    // --------------------------------------------------------------------------------
    // MIXED
    // --------------------------------------------------------------------------------
    @Unroll
    def "MIXED: #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        description                         | recurrences                                                                             | expected
        "Empty = Once"                     | []                                                                                      | ["Once only"]
    }

    @Unroll
    def "MIXED (fuzzy match): #description"() {
        expect:
        def descriptions = schedule(newTask(recurrences)).longDescriptions
        for (int i = 0; i < descriptions.size(); i++) {
            assert descriptions[i].startsWith(expectedPrefixes[i])
        }

        where:
        description                         | recurrences                                                                             | expectedPrefixes
        "Weekly + Biweekly"                | [weekly(DayOfWeek.MONDAY, 9, 0), biweekly(DayOfWeek.MONDAY, 0, 9, 0)]                    | ["Weekly on", "Every 2nd week on"]
        "Daily + Weekly + Monthly"         | [daily(8, 0), weekly(DayOfWeek.TUESDAY, 8, 0), monthly(30, 8, 0)]                        | ["Every day", "Weekly", "Monthly"]
        "Once + Monthly"                   | [onceUnlimited(), monthly(30, 6, 30)]                                                   | ["Monthly"]
        "Everything"                       | [once(), daily(7, 0), weekly(DayOfWeek.SATURDAY, 12, 0), yearly(200, 8, 0)]             | ["Every day", "Weekly", "Yearly"]
    }

    private static ModelTask newTask(List<ModelRecurrence> recurrences) {
        def task = X_Model.create(ModelTask)
        task.setBirth(System.currentTimeMillis())
        task.setUpdated(System.currentTimeMillis())
        for (ModelRecurrence r : recurrences) {
            task.recurrence().add(r)
        }
        return task
    }

    private static Schedule schedule(ModelTask task) {
        return new Schedule(task)
    }

    private static ModelRecurrence once() {
        return ModelRecurrence.once(System.currentTimeMillis(), 0, 12, 0)
    }

    private static ModelRecurrence once(int hour, int minute) {
        return ModelRecurrence.once(System.currentTimeMillis(), 0, hour, minute)
    }

    private static ModelRecurrence onceUnlimited() {
        def r = X_Model.create(ModelRecurrence)
        r.setUnit(RecurrenceUnit.ONCE)
        r.setValue(0L)
        return r
    }

    private static ModelRecurrence oncePast() {
        return ModelRecurrence.once(System.currentTimeMillis() - 86_400_000, 0, 8, 0)
    }

    private static ModelRecurrence daily(int hour, int minute) {
        return ModelRecurrence.daily(hour, minute)
    }

    private static ModelRecurrence weekly(DayOfWeek day, int hour, int minute) {
        return ModelRecurrence.weekly(day, hour, minute)
    }

    private static ModelRecurrence biweekly(DayOfWeek day, int offset = 0, int hour = 9, int min = 0) {
        return ModelRecurrence.biweekly(day, offset, hour, min)
    }

    private static ModelRecurrence triweekly(DayOfWeek day, int offset = 0, int hour = 9, int min = 0) {
        return ModelRecurrence.triweekly(day, offset, hour, min)
    }

    private static ModelRecurrence monthly(int dayOfMonth, int hour, int minute) {
        return ModelRecurrence.monthly(dayOfMonth, hour, minute)
    }

    private static ModelRecurrence yearly(int dayOfYear, int hour, int minute) {
        def r = X_Model.create(ModelRecurrence)
        r.setUnit(RecurrenceUnit.YEARLY)
        r.setValue(dayOfYear * 60 * 24 + hour * 60 + minute)
        return r
    }
}