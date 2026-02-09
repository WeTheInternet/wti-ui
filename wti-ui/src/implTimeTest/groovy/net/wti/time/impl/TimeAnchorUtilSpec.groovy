package net.wti.time.impl

import net.wti.time.api.DayIndex
import net.wti.time.api.ModelDay
import net.wti.time.api.TimeAnchor
import net.wti.time.api.TimeAnchorKind
import spock.lang.Specification
import spock.lang.Unroll
import xapi.model.X_Model
import xapi.time.api.TimeZoneInfo

/// TimeAnchorUtilSpec
///
/// Tests for TimeAnchorUtil:
/// - validate() enforces required fields per TimeAnchorKind
/// - computeDeadlineMillis() for DAILY anchors
/// - Deadline within ModelDay window and at expected hour/minute.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:59
class TimeAnchorUtilSpec extends Specification {

    DayIndexService indexService
    ModelDayService dayService
    TimeZoneInfo utcZone

    def setup() {
        utcZone = new TimeZoneInfo("UTC", "UTC", 0, false)
        indexService = new DayIndexService(utcZone, 4)
        dayService = new ModelDayService(indexService)
    }

    // -------------------------------------------------------------------------
    // validate()
    // -------------------------------------------------------------------------

    @Unroll
    def "validate DAILY anchor requires hour & minute (hour=#hour, minute=#minute)"() {
        given:
        TimeAnchor anchor = X_Model.create(TimeAnchor)

        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(hour)
        anchor.setMinute(minute)

        when:
        TimeAnchorUtil.validate(anchor)

        then:
        thrown == null

        where:
        hour | minute || thrown
        0    | 0      || null
        23   | 59     || null
    }

    @Unroll
    def "validate DAILY anchor rejects missing hour/minute (hour=#hour, minute=#minute)"() {
        given:
        TimeAnchor anchor = X_Model.create(TimeAnchor)

        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(hour)
        anchor.setMinute(minute)

        when:
        TimeAnchorUtil.validate(anchor)

        then:
        thrown(IllegalArgumentException)

        where:
        hour | minute
        null | 0
        0    | null
        null | null
    }

    def "validate WEEKLY anchor requires dayOfWeek/hour/minute"() {
        given:
        TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.WEEKLY)
        anchor.setDayOfWeek(1)
        anchor.setHour(10)
        anchor.setMinute(30)

        when:
        TimeAnchorUtil.validate(anchor)

        then:
        noExceptionThrown()
    }

    def "validate WEEKLY anchor rejects missing fields"() {
        given:
        TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.WEEKLY)
        anchor.setDayOfWeek(null)
        anchor.setHour(10)
        anchor.setMinute(30)

        when:
        TimeAnchorUtil.validate(anchor)

        then:
        thrown(IllegalArgumentException)
    }

    // -------------------------------------------------------------------------
    // computeDeadlineMillis() for DAILY
    // -------------------------------------------------------------------------

    def "computeDeadlineMillis DAILY: 0:00 is exactly ModelDay.startTimestamp"() {
        given: "A ModelDay for day index 0 with default UTC/4am rollover"
        ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))

        and: "A DAILY TimeAnchor at 00:00"
        TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(0)
        anchor.setMinute(0)

        when:
        long deadline = TimeAnchorUtil.computeDeadlineMillis(day, anchor)

        then:
        deadline == day.startTimestamp()
        deadline >= day.startTimestamp()
        deadline <= day.endTimestamp()
    }

    def "computeDeadlineMillis DAILY: 10:30 is start + 10h30m"() {
        given:
        ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))

        and: "A DAILY anchor at 10:30"
        TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(10)
        anchor.setMinute(30)

        when:
        long deadline = TimeAnchorUtil.computeDeadlineMillis(day, anchor)
        long expectedOffsetMillis = (10 * 60L + 30L) * 60_000L
        long expected = day.startTimestamp() + expectedOffsetMillis

        then:
        deadline == expected
        deadline >= day.startTimestamp()
        deadline <= day.endTimestamp()
    }

    def "computeDeadlineMillis DAILY: invalid hour throws"() {
        given:
        ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(24) // invalid
        anchor.setMinute(0)

        when:
        TimeAnchorUtil.computeDeadlineMillis(day, anchor)

        then:
        thrown(IllegalArgumentException)
    }

    def "computeDeadlineMillis rejects null day or anchor"() {
        given:
        TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.DAILY)
        anchor.setHour(1)
        anchor.setMinute(0)

        when: "null day"
        TimeAnchorUtil.computeDeadlineMillis(null, anchor)

        then:
        thrown(IllegalArgumentException)

        when: "null anchor"
        ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        TimeAnchorUtil.computeDeadlineMillis(day, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "computeDeadlineMillis WEEKLY currently unsupported"() {
        given:
        ModelDay day = dayService.getOrCreateModelDay(DayIndex.of(0))
        TimeAnchor anchor = X_Model.create(TimeAnchor)
        anchor.setKind(TimeAnchorKind.WEEKLY)
        anchor.setDayOfWeek(day.dayOfWeek())
        anchor.setHour(10)
        anchor.setMinute(0)

        when:
        TimeAnchorUtil.computeDeadlineMillis(day, anchor)

        then:
        thrown(UnsupportedOperationException)
    }
}
