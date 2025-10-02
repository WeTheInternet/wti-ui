package net.wti.tasks.index

import net.wti.ui.demo.api.DayOfWeek
import net.wti.ui.demo.api.ModelRecurrence
import net.wti.ui.demo.api.ModelTask
import spock.lang.Specification
import spock.lang.Unroll
import xapi.model.X_Model
import xapi.model.api.ModelList

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class TaskFactoryTest extends Specification {

    // Helpers to compute expected results

    static ZoneId zone() {
        ZoneId.systemDefault()
    }

    static long nowMillis() {
        Instant.now().toEpochMilli()
    }

    static LocalDateTime nowLdt() {
        LocalDateTime.now(zone())
    }

    static DayOfWeek today() {
        DayOfWeek.valueOf(nowLdt().dayOfWeek.name())
    }

    static DayOfWeek todayPlus(final int days) {
        DayOfWeek.valueOf((nowLdt().dayOfWeek + days).name())
    }

    static DayOfWeek todayMinus(final int days) {
        DayOfWeek.valueOf((nowLdt().dayOfWeek - days).name())
    }

    static long startOfWeekMillis() {
        // Align with TaskFactory.toStartOfWeek(LocalDateTime now):
        // Uses Sunday as first day of week, NOT Monday (ISO)
        final LocalDate today = nowLdt().toLocalDate()
        final int daysSinceSunday = today.getDayOfWeek().getValue() % 7 // Sunday=0, Monday=1, ..., Saturday=6
        final LocalDate sunday = today.minusDays(daysSinceSunday)
        return ZonedDateTime.of(sunday, LocalTime.MIDNIGHT, zone()).toInstant().toEpochMilli()
    }

    static int minutesFromWeekStart(final DayOfWeek day, final int hour, final int minute) {
        (day.ordinal() * 24 * 60) + (hour * 60) + minute
    }

    static long expectedNextWeekly(final boolean neverFinished, final long lastFinished, final DayOfWeek recurDay, final int hour, final int minute) {
        final long sow = startOfWeekMillis()
        final long targetThisWeek = sow + minutesFromWeekStart(recurDay, hour, minute) * 60_000L
        if (neverFinished) {
            // first-time: if still upcoming this week (including "later today"), choose this week, else next week
            final long nowMs = nowMillis()
            if (targetThisWeek > nowMs) {
                return targetThisWeek
            } else {
                return targetThisWeek + 7L * 24 * 60 * 60 * 1000
            }
        } else {
            // subsequent runs: if the lastFinished was before this week's target, choose this week; else next week
            if (lastFinished < targetThisWeek) {
                return targetThisWeek
            } else {
                return targetThisWeek + 7L * 24 * 60 * 60 * 1000
            }
        }
    }

    static ModelRecurrence recurWeekly(final DayOfWeek day, final int hour, final int minute) {
        return ModelRecurrence.weekly(day, hour, minute)
    }

    static ModelList<ModelRecurrence> asModelList(final List<ModelRecurrence> recurrences) {
        // Minimal ModelList mock sufficient for nextTime: isEmpty() + iteration
        final ModelList<ModelRecurrence> list = X_Model.create(ModelList)
        list.setModelType(ModelRecurrence)
        list.addNow(recurrences)
        return list
    }

    static ModelTask mkTaskWithRecurrences(final List<ModelRecurrence> recurrences,
                                           final Long birth = null,
                                           final Long lastFinished = null) {
        final ModelTask task = X_Model.create(ModelTask)
        task.recurrence().addNow(recurrences)
        // Birth is required by nextRecurrence logic when lastFinished is null/0
        final Long birthVal = birth ?: (nowMillis() - 60_000)  // default: 1 minute ago
        task.setBirth(birthVal)
        task.setLastFinished(lastFinished)
        return task
    }

    @Unroll
    def "weekly nextTime is in the future and correct for first run (recur=#recurDay @ #hour:#minute)"() {
        given:
        // first run: never finished (lastFinished == birth)
        final long birth = nowMillis()
        final def task = mkTaskWithRecurrences([recurWeekly(recurDay, hour, minute)], birth, null)

        when:
        final Double next = TaskFactory.nextTime(task)

        then:
        next != null
        next > nowMillis()
        next.longValue() == expectedNextWeekly(true, birth, recurDay, hour, minute)

        where:
        // Cover today before, same as, and after the scheduled time by picking several recurrence days
        recurDay      | hour | minute
        today()       | 23   | 59
        today()       | 0    | 1
        todayPlus(1)  | 9    | 0
        todayMinus(1) | 12   | 30
    }

    @Unroll
    def "weekly nextTime after lastFinished chooses this week if pending else next week (recur=#recurDay @ #hour:#minute)"() {
        given:
        final def targetThisWeek = startOfWeekMillis() + minutesFromWeekStart(recurDay, hour, minute) * 60_000L
        // create lastFinished just before or just after target to exercise paths
        final long lf = lastFinishedBias == 'before' ? targetThisWeek - 1_000 : targetThisWeek + 1_000
        final def task = mkTaskWithRecurrences([recurWeekly(recurDay, hour, minute)],
                nowMillis() - 86_400_000L, // birth: yesterday
                lf)

        when:
        final Double next = TaskFactory.nextTime(task)

        then:
        next != null
        next > nowMillis()
        next.longValue() == expectedNextWeekly(false, lf, recurDay, hour, minute)

        where:
        recurDay      | hour | minute | lastFinishedBias
        today()       | 23   | 0      | 'before'
        today()       | 8    | 0      | 'after'
        todayPlus(2)  | 12   | 15     | 'before'
        todayMinus(3) | 18   | 45     | 'after'
    }

    def "multiple recurrences: nextTime picks the soonest of the candidates"() {
        given:
        final DayOfWeek d1 = today()
        final DayOfWeek d2 = todayPlus(1)
        final def r1 = recurWeekly(d1, 23, 0)      // later today
        final def r2 = recurWeekly(d2, 9, 0)       // tomorrow morning
        final def r3 = recurWeekly(d1, 0, 1)       // early today -> should roll to next week
        final def task = mkTaskWithRecurrences([r1, r2, r3], nowMillis() - 1_000, null)

        when:
        final Double next = TaskFactory.nextTime(task)

        then:
        next != null
        next > nowMillis()
        // Expected is the min of the three computed expectations for first-run
        final long e1 = expectedNextWeekly(true, task.getBirth(), d1, 23, 0)
        final long e2 = expectedNextWeekly(true, task.getBirth(), d2, 9, 0)
        final long e3 = expectedNextWeekly(true, task.getBirth(), d1, 0, 1)
        next.longValue() == Math.min(e1, Math.min(e2, e3))
    }

    def "ONCE recurrence returns exactly the given timestamp and sets the deadline"() {
        given:
        final ModelRecurrence once = ModelRecurrence.once(1, 0)
        final long futureTs = nowMillis() + TimeUnit.HOURS.toMillis(1)
        final ModelTask task = mkTaskWithRecurrences([once], nowMillis() - 1000, null)

        when:
        final Double next = TaskFactory.nextTime(task)

        then:
        next != null
        Math.abs(next - futureTs) < 100d // tolerate milliseconds of latency
    }

    def "no recurrence returns null and does not throw"() {
        given:
        final def emptyList = asModelList([])
        final def task = Mock(ModelTask)
        task.getRecurrence() >> emptyList

        when:
        final Double next = TaskFactory.nextTime(task)

        then:
        next == null
    }

    @Unroll
    def "weekly always computes a next time strictly in the future for wide variety (recur=#recurDay @ #hour:#minute, neverFinished=#neverFinished)"() {
        given:
        final long birth = nowMillis() - 5 * 60_000
        final Long lastFinished = neverFinished ? null : birth + 60_000
        final def task = mkTaskWithRecurrences([recurWeekly(recurDay, hour, minute)], birth, lastFinished)

        when:
        final Double next = TaskFactory.nextTime(task)

        then:
        next != null
        next > nowMillis()

        where:
        recurDay      | hour | minute | neverFinished
        today()       | 0    | 1      | true
        today()       | 23   | 59     | true
        todayPlus(1)  | 7    | 30     | true
        todayMinus(1) | 12   | 0      | true
        today()       | 0    | 1      | false
        today()       | 23   | 59     | false
        todayPlus(1)  | 7    | 30     | false
        todayMinus(1) | 12   | 0      | false
    }
}
