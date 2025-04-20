package net.wti.ui.demo.api


import spock.lang.Specification
import spock.lang.Unroll
import xapi.model.X_Model
/// # ScheduleTest
///
/// Unit tests for `Schedule` recurrence description builder.
///
/// This test suite verifies grouping and long-form description generation
/// for all known `RecurrenceUnit` values, including mixed-type scenarios.
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 20/04/2025 @ 23:32 CST
class ScheduleTest extends Specification {

    @Unroll
    def "ONCE recurrence (case #num): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == [expected]

        where:
        num | description                 | recurrences           | expected
        1   | "Single ONCE recurrence"    | [once()]              | "Once only"
        2   | "Multiple ONCE recurrences" | [once(), once()]      | "Once only"
        3   | "ONCE with daily"           | [once(), daily(9, 0)] | "Every day at 9:00"
        4   | "Empty recurrence list"     | []                    | "Once only"
    }

    @Unroll
    def "DAILY recurrence (case #num): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        num | description                     | recurrences                 | expected
        1   | "Daily at 8:30"                 | [daily(8, 30)]              | ["Every day at 8:30"]
        2   | "Two identical daily times"     | [daily(9, 0), daily(9, 0)]  | ["Every day at 9:00"]
        3   | "Different times split entries" | [daily(8, 0), daily(12, 0)] | ["Every day at 8:00", "Every day at 12:00"]
    }

    @Unroll
    def "WEEKLY recurrence (case #num): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        num | description                | recurrences                                                       | expected
        1   | "Single day weekly"        | [weekly(DayOfWeek.MONDAY, 9, 0)]                                  | ["Weekly on Monday at 9:00"]
        2   | "Multiple days, same time" | [weekly(DayOfWeek.MONDAY, 9, 0), weekly(DayOfWeek.FRIDAY, 9, 0)]  | ["Weekly on Monday, Friday at 9:00"]
        3   | "Different times split"    | [weekly(DayOfWeek.MONDAY, 9, 0), weekly(DayOfWeek.MONDAY, 14, 0)] | ["Weekly on Monday at 9:00", "Weekly on Monday at 14:00"]
    }

    @Unroll
    def "BIWEEKLY recurrence (case #num): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        num | description                 | recurrences                                                                       | expected
        1   | "Same time, different days" | [biweekly(DayOfWeek.TUESDAY, 0, 18, 15), biweekly(DayOfWeek.THURSDAY, 0, 18, 15)] | ["Every 2nd week on Tuesday, Thursday at 18:15"]
        2   | "Same day, different times" | [biweekly(DayOfWeek.TUESDAY, 0, 8, 0), biweekly(DayOfWeek.TUESDAY, 0, 9, 30)]     | ["Every 2nd week on Tuesday at 8:00", "Every 2nd week on Tuesday at 9:30"]
    }

    @Unroll
    def "MONTHLY recurrence (case #num): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        num | description                      | recurrences                                                            | expected
        1   | "Monthly on Saturday and Sunday" | [monthly(DayOfWeek.SATURDAY, 12, 0), monthly(DayOfWeek.SUNDAY, 12, 0)] | ["Monthly on Sunday, Monday at 12:00"]
        2   | "Two different times"            | [monthly(DayOfWeek.MONDAY, 9, 0), monthly(DayOfWeek.MONDAY, 15, 0)]    | ["Monthly on Tuesday at 9:00", "Monthly on Tuesday at 15:00"]
    }

    @Unroll
    def "YEARLY recurrence (case #num): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        num | description              | recurrences                                                                  | expected
        1   | "Single yearly entry"    | [yearly(DayOfWeek.THURSDAY, 0, 10, 0)]                                       | ["Yearly on Thursday at 10:00"]
        2   | "Group yearly same time" | [yearly(DayOfWeek.THURSDAY, 0, 10, 0), yearly(DayOfWeek.SATURDAY, 0, 10, 0)] | ["Yearly on Thursday, Saturday at 10:00"]
    }

    @Unroll
    def "MIXED recurrence (case #num): #description"() {
        expect:
        schedule(newTask(recurrences)).longDescriptions == expected

        where:
        num | description                    | recurrences                                                                      | expected
        1   | "Weekly + Biweekly mix"        | [weekly(DayOfWeek.MONDAY, 9, 0), biweekly(DayOfWeek.MONDAY, 0, 9, 0)]            | ["Weekly on Monday at 9:00", "Every 2nd week on Monday at 9:00"]
        2   | "Daily, Weekly, Monthly combo" | [daily(8, 0), weekly(DayOfWeek.TUESDAY, 8, 0), monthly(DayOfWeek.TUESDAY, 8, 0)] | ["Every day at 8:00", "Weekly on Tuesday at 8:00", "Monthly on Wednesday at 8:00"]
        3   | "ONCE + Monthly"               | [once(), monthly(DayOfWeek.SUNDAY, 6, 30)]                                       | ["Monthly on Sunday at 6:30"]
        4   | "Empty recurrence list"        | []                                                                               | ["Once only"]
    }

    // ---------------------------------------------------------
    // 🧪 Builder helpers
    // ---------------------------------------------------------

    private static ModelTask newTask(ArrayList<ModelRecurrence> recurrences) {
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

    private static ModelRecurrence daily(int hour, int min) {
        return ModelRecurrence.daily(hour, min)
    }

    private static ModelRecurrence weekly(DayOfWeek day, int hour, int min) {
        return ModelRecurrence.weekly(day, hour, min)
    }

    private static ModelRecurrence biweekly(DayOfWeek day, int offset = 0, int hour = 9, int min = 0) {
        return ModelRecurrence.biweekly(day, offset, hour, min)
    }

    private static ModelRecurrence triweekly(DayOfWeek day, int offset = 0, int hour = 9, int min = 0) {
        return ModelRecurrence.triweekly(day, offset, hour, min)
    }

    private static ModelRecurrence monthly(DayOfWeek day, int hour, int min) {
        return ModelRecurrence.monthly(day.ordinal() + 1, hour, min);
    }

    private static ModelRecurrence yearly(DayOfWeek day, int offset = 0, int hour = 9, int min = 0) {
        def recur = ModelRecurrence.weekly(day, hour, min)
        recur.setUnit(RecurrenceUnit.YEARLY)
        return recur
    }
}