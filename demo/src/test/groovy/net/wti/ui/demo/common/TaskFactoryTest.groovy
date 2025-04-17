package net.wti.ui.demo.common

import net.wti.ui.demo.api.*
import spock.lang.Specification
import xapi.time.X_Time

import java.time.LocalDateTime

class TaskManagerTest extends Specification {

    def "TaskManager prints accurate human readable deadlines for ONCE recurrence"() {
        when:
        long amount = 2 * X_Time.ONE_DAY + 3 * X_Time.ONE_HOUR + 30 * X_Time.ONE_MINUTE
        ModelTask task = TaskManager.create("ONCE TEST")
        ModelRecurrence recur = ModelRecurrence.once(3, 30)
        recur.setUnit(RecurrenceUnit.ONCE)
        recur.setValue(amount)
        task.recurrence().add(recur)
        then:
        TaskManager.INSTANCE.nextTime(task).longValue() == amount
    }

    def "TaskManager prints accurate human readable deadlines for WEEKLY recurrence"() {
        when:
        ModelTask task = TaskManager.create("WEEKLY TEST")
        LocalDateTime now = LocalDateTime.now()
        LocalDateTime later = now.plusDays(1).plusHours(3).plusMinutes(30).minusSeconds(now.getSecond())
        ModelRecurrence recur = ModelRecurrence.weekly(DayOfWeek.valueOf(later.getDayOfWeek().name()), later.getHour(), later.getMinute())
        task.recurrence().add(recur)
        then:
        new Date(TaskManager.INSTANCE.nextTime(task).longValue()) == new Date(1000 * later.toEpochSecond(ModelSettings.timeZone()))

        when:
        later = now.plusDays(2).plusHours(6).plusMinutes(57).minusSeconds(now.getSecond())
        recur = ModelRecurrence.weekly(DayOfWeek.valueOf(later.getDayOfWeek().name()), later.getHour(), later.getMinute())
        task.recurrence().clear()
        task.recurrence().add(recur)
        then:
        new Date(TaskManager.INSTANCE.nextTime(task).longValue()) == new Date(1000 * later.toEpochSecond(ModelSettings.timeZone()))

        when:
        later = now.plusDays(3).plusHours(16).plusMinutes(57).minusSeconds(now.getSecond())
        recur = ModelRecurrence.weekly(DayOfWeek.valueOf(later.getDayOfWeek().name()), later.getHour(), later.getMinute())
        task.recurrence().clear()
        task.recurrence().add(recur)
        then:
        new Date(TaskManager.INSTANCE.nextTime(task).longValue()) == new Date(1000 * later.toEpochSecond(ModelSettings.timeZone()))

        when:
        later = now.plusDays(4).plusHours(1).plusMinutes(5).minusSeconds(now.getSecond())
        recur = ModelRecurrence.weekly(DayOfWeek.valueOf(later.getDayOfWeek().name()), later.getHour(), later.getMinute())
        task.recurrence().clear()
        task.recurrence().add(recur)
        then:
        new Date(TaskManager.INSTANCE.nextTime(task).longValue()) == new Date(1000 * later.toEpochSecond(ModelSettings.timeZone()))

        when:
        later = now.plusDays(5).plusHours(23).plusMinutes(59).minusSeconds(now.getSecond())
        recur = ModelRecurrence.weekly(DayOfWeek.valueOf(later.getDayOfWeek().name()), later.getHour(), later.getMinute())
        task.recurrence().clear()
        task.recurrence().add(recur)
        then:
        new Date(TaskManager.INSTANCE.nextTime(task).longValue()) == new Date(1000 * later.toEpochSecond(ModelSettings.timeZone()))

        when:
        later = now.plusDays(6).plusHours(0).plusMinutes(0).minusSeconds(now.getSecond())
        recur = ModelRecurrence.weekly(DayOfWeek.valueOf(later.getDayOfWeek().name()), later.getHour(), later.getMinute())
        task.recurrence().clear()
        task.recurrence().add(recur)
        then:
        new Date(TaskManager.INSTANCE.nextTime(task).longValue()) == new Date(1000 * later.toEpochSecond(ModelSettings.timeZone()))

    }

}
