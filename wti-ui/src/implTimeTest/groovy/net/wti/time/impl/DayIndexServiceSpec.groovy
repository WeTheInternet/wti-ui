package net.wti.time.impl

import net.wti.time.api.DayIndex
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import xapi.fu.log.Log
import xapi.time.api.TimeZoneInfo

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
/// DayIndexServiceSpec
///
/// Comprehensive tests for DayIndex computation, including:
/// - Rollover boundaries (3:59am -> 4:00am)
/// - DST transitions (spring forward, fall back)
/// - Leap years
/// - Strange rollover hours (0, 12, 23)
/// - Different timezones
/// - Edge cases around epoch
/// - Negative day numbers (dates before epoch)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2025-11-18
class DayIndexServiceSpec extends Specification {

    @Shared
    DayIndexService service
    @Shared
    TimeZoneInfo utcZone
    @Shared
    TimeZoneInfo estZone
    @Shared
    TimeZoneInfo pstZone
    @Shared
    TimeZoneInfo tokyoZone

    def setup() {
        utcZone = new TimeZoneInfo("UTC", "UTC", 0, false)
        estZone = new TimeZoneInfo("America/New_York", "Eastern", -5 * 3600000, true)
        pstZone = new TimeZoneInfo("America/Los_Angeles", "Pacific", -8 * 3600000, true)
        tokyoZone = new TimeZoneInfo("Asia/Tokyo", "Tokyo", 9 * 3600000, false)

        service = new DayIndexService(utcZone, 4)
    }

    // -------------------------------------------------------------------------
    // Basic DayIndex computation
    // -------------------------------------------------------------------------

    def "should compute DayIndex=-1 for epoch date at midnight UTC"() {
        given: "Midnight on epoch date (2025-10-10 00:00:00 UTC)"
        final long epochMillis = DayIndex.EPOCH_MILLIS

        when: "Computing DayIndex with rolloverHour=4"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, 4)

        then: "Should be DayIndex=-1 (before 4am rollover)"
        Log.tryLog(DayIndexServiceSpec.class, "Midnight epoch test:",
                "epochMillis:", epochMillis,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: -1 (midnight is before 4am rollover)")
        dayIndex.dayNum == -1
    }

    def "should compute DayIndex=-1 for epoch date before rollover hour"() {
        given: "2025-10-10 03:59:59 UTC (just before 4am)"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (3 * 3600 + 59 * 60 + 59) * 1000L

        when: "Computing DayIndex with rolloverHour=4"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, 4)

        then: "Should be DayIndex=-1 (still previous day)"
        Log.tryLog(DayIndexServiceSpec.class, "Pre-rollover test:",
                "epochMillis:", epochMillis,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: -1 (3:59:59 is before 4am rollover)")
        dayIndex.dayNum == -1
    }

    def "should compute DayIndex=0 for epoch date at rollover hour"() {
        given: "2025-10-10 04:00:00 UTC (exactly at rollover)"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (4 * 3600) * 1000L

        when: "Computing DayIndex with rolloverHour=4"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, 4)

        then: "Should be DayIndex=0 (day 0 starts)"
        Log.tryLog(DayIndexServiceSpec.class, "At rollover test:",
                "epochMillis:", epochMillis,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: 0 (4:00:00 is exactly at rollover)")
        dayIndex.dayNum == 0
    }

    def "should compute DayIndex=0 for noon on epoch date"() {
        given: "2025-10-10 12:00:00 UTC"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (12 * 3600) * 1000L

        when: "Computing DayIndex"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, 4)

        then: "Should be DayIndex=0"
        Log.tryLog(DayIndexServiceSpec.class, "Noon epoch test:",
                "epochMillis:", epochMillis,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: 0 (noon is after 4am rollover)")
        dayIndex.dayNum == 0
    }

    def "should compute DayIndex=1 for next day at rollover hour"() {
        given: "2025-10-11 04:00:00 UTC (24 hours + 4 hours after epoch midnight)"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (28 * 3600) * 1000L

        when: "Computing DayIndex with rolloverHour=4"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, 4)

        then: "Should be DayIndex=1"
        Log.tryLog(DayIndexServiceSpec.class, "Next day rollover test:",
                "epochMillis:", epochMillis,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: 1 (4am next day)")
        dayIndex.dayNum == 1
    }
    def "should compute negative DayIndex for dates before epoch"() {
        given: "2025-10-09 12:00:00 UTC (day before epoch, noon)"
        final long epochMillis = DayIndex.EPOCH_MILLIS - (12 * 3600) * 1000L

        when: "Computing DayIndex"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, 4)

        then: "Should be DayIndex=-1 (day -1 runs from 4am on 2025-10-09)"
        Log.tryLog(DayIndexServiceSpec.class, "Negative DayIndex test:",
                "epochMillis:", epochMillis,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: -1 (noon on 2025-10-09 is after 4am, so it's day -1)")
        dayIndex.dayNum == -1
    }

    def "should compute far future DayIndex correctly"() {
        given: "100 days + 12 hours after epoch midnight"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (100 * 86400 + 12 * 3600) * 1000L

        when: "Computing DayIndex"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis)

        then: "Should be DayIndex=100 (noon on that day is after 4am, so it's day 100)"
        Log.tryLog(DayIndexServiceSpec.class, "Far future test:",
                "epochMillis:", epochMillis,
                "Days added:", 100,
                "Hours added:", 12,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: 100 (noon is after 4am rollover)")
        dayIndex.dayNum == 100
    }

    def "should compute far past DayIndex correctly"() {
        given: "100 days + 12 hours before epoch midnight"
        final long epochMillis = DayIndex.EPOCH_MILLIS - (100 * 86400 + 12 * 3600) * 1000L

        when: "Computing DayIndex"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis)

        then: "Should be DayIndex=-101 (noon is after 4am, so it's that day)"
        Log.tryLog(DayIndexServiceSpec.class, "Far past test:",
                "epochMillis:", epochMillis,
                "Days subtracted:", 100,
                "Hours subtracted:", 12,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: -101 (noon is after 4am rollover)")
        dayIndex.dayNum == -101
    }

    // -------------------------------------------------------------------------
    // Rollover boundary testing
    // -------------------------------------------------------------------------

    @Unroll
    def "should handle rollover boundary: #hour:#minute with rolloverHour=#rollover"() {
        given: "Epoch date at specific time"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (hour * 3600 + minute * 60) * 1000L

        when: "Computing DayIndex"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, rollover)

        then: "DayIndex should match expected"
        dayIndex.dayNum == expected

        where:
        rollover | hour | minute | expected
        4        | 3    | 59     | -1       // just before rollover (previous day)
        4        | 4    | 0      | 0        // exactly at rollover (day 0 starts)
        4        | 4    | 1      | 0        // just after rollover
        4        | 12   | 0      | 0        // noon on day 0
        0        | 0    | 0      | 0        // midnight rollover (day 0 starts)
        0        | 23   | 59     | 0        // before next midnight
        12       | 11   | 59     | -1       // before noon rollover
        12       | 12   | 0      | 0        // noon rollover (day 0 starts)
        12       | 23   | 59     | 0        // after noon, before next noon
        23       | 22   | 59     | -1       // before 11pm rollover
        23       | 23   | 0      | 0        // 11pm rollover (day 0 starts)
    }

    @Unroll
    def "should handle 3:59am vs 4:00am boundary on day #dayOffset"() {
        given: "Times just before and at rollover on different days"
        final long baseMillis = DayIndex.EPOCH_MILLIS + (dayOffset * 86400) * 1000L
        final long beforeRollover = baseMillis + (3 * 3600 + 59 * 60) * 1000L  // 3:59am
        final long atRollover = baseMillis + (4 * 3600) * 1000L                 // 4:00am

        when: "Computing DayIndex for both times"
        final DayIndex beforeIndex = service.computeDayIndex(beforeRollover, utcZone, 4)
        final DayIndex atIndex = service.computeDayIndex(atRollover, utcZone, 4)

        then: "Should be consecutive days"
        atIndex.dayNum == dayOffset
        beforeIndex.dayNum == dayOffset - 1

        where:
        dayOffset << [-10, -1, 0, 1, 10, 100]
    }

    // -------------------------------------------------------------------------
    // DST handling
    // -------------------------------------------------------------------------

    def "should handle rollover during DST spring forward"() {
        given: "EST zone during spring forward (2024-03-10, clocks skip 2am->3am)"
        final ZonedDateTime beforeDST = ZonedDateTime.of(
                LocalDateTime.of(2024, 3, 10, 1, 59),
                ZoneId.of("America/New_York")
        )
        final ZonedDateTime afterDST = ZonedDateTime.of(
                LocalDateTime.of(2024, 3, 10, 4, 0),
                ZoneId.of("America/New_York")
        )

        when: "Computing DayIndex for times around DST transition"
        final DayIndex before = service.computeDayIndex(beforeDST.toInstant().toEpochMilli(), estZone, 4)
        final DayIndex after = service.computeDayIndex(afterDST.toInstant().toEpochMilli(), estZone, 4)

        then: "Should handle correctly despite missing hour"
        after.dayNum == before.dayNum  // 4am is still same day as 1:59am (day hasn't rolled yet at 2am)
    }

    def "should handle rollover during DST fall back"() {
        given: "EST zone during fall back (2024-11-03, clocks repeat 1am->2am)"
        final ZonedDateTime beforeDST = ZonedDateTime.of(
                LocalDateTime.of(2024, 11, 3, 1, 59),
                ZoneId.of("America/New_York")
        )
        // During fall back, we get first 1am-2am in DST, then again in standard
        final ZonedDateTime afterDST = beforeDST.plusHours(2)  // Jump past the ambiguous hour

        when: "Computing DayIndex"
        final DayIndex before = service.computeDayIndex(beforeDST.toInstant().toEpochMilli(), estZone, 4)
        final DayIndex after = service.computeDayIndex(afterDST.toInstant().toEpochMilli(), estZone, 4)

        then: "Should handle correctly despite repeated hour"
        after.dayNum == before.dayNum  // Still same day
    }

    // -------------------------------------------------------------------------
    // Timezone handling
    // -------------------------------------------------------------------------

    @Unroll
    def "should compute correct DayIndex in #zoneName timezone"() {
        given: "Same UTC time in different timezones"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (12 * 3600) * 1000L  // noon UTC on epoch

        when: "Computing DayIndex in different zones"
        final DayIndex utcIndex = service.computeDayIndex(epochMillis, utcZone, 4)
        final DayIndex zoneIndex = service.computeDayIndex(epochMillis, zone, 4)

        then: "Should respect timezone offset"
        Log.tryLog(DayIndexServiceSpec.class, "Testing timezone:", zoneName,
                "epochMillis:", epochMillis,
                "UTC index:", utcIndex.dayNum,
                "Zone index:", zoneIndex.dayNum,
                "Expected:", expected,
                "Zone offset hours:", zone.getOffsetAt(epochMillis) / 3600000.0)

        utcIndex.dayNum == 0
        zoneIndex.dayNum == expected

        where:
        zoneName          | zone       | expected | comment
        "UTC"             | utcZone    | 0        | "noon UTC on 2025-10-10 = after 4am UTC rollover = day 0"
        "EST (UTC-5)"     | estZone    | 0        | "noon UTC = 7am EST on 2025-10-10 = after 4am EST rollover = day 0"
        "PST (UTC-8)"     | pstZone    | 0        | "noon UTC = 4am PST on 2025-10-10 = exactly at 4am PST rollover = day 0"
        "Tokyo (UTC+9)"   | tokyoZone  | 0        | "noon UTC = 9pm JST on 2025-10-10 = well after 4am JST rollover = day 0"
    }

    def "should handle international date line crossing"() {
        given: "Midnight UTC"
        final long midnightUTC = DayIndex.EPOCH_MILLIS

        when: "Computing DayIndex in Tokyo vs PST"
        final DayIndex tokyoIndex = service.computeDayIndex(midnightUTC, tokyoZone, 4)
        final DayIndex pstIndex = service.computeDayIndex(midnightUTC, pstZone, 4)

        then: "Tokyo should be ahead of PST, but both in day -1 (before 4am in their zones)"
        tokyoIndex.dayNum == -1  // midnight UTC = 9am JST (after 4am, day -1)
        pstIndex.dayNum == -1    // midnight UTC = 4pm previous day PST (after 4am, day -1)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Unroll
    def "should handle extreme rolloverHour=#rollover"() {
        given: "Epoch date at specific time"
        final long epochMillis = DayIndex.EPOCH_MILLIS + (hour * 3600) * 1000L

        when: "Computing DayIndex with extreme rollover"
        final DayIndex dayIndex = service.computeDayIndex(epochMillis, utcZone, rollover)

        then: "Should handle correctly"
        dayIndex.dayNum == expected

        where:
        rollover | hour | expected
        0        | 0    | 0        // midnight rollover, at rollover
        0        | 12   | 0        // midnight rollover, noon
        0        | 23   | 0        // midnight rollover, 11pm
        23       | 22   | -1       // 11pm rollover, before it
        23       | 23   | 0        // 11pm rollover, at it
        1        | 0    | -1       // 1am rollover, midnight
        1        | 1    | 0        // 1am rollover, at it
    }

    def "should handle leap second scenarios"() {
        given: "Time near a leap second boundary"
        final long nearLeapSecond = DayIndex.EPOCH_MILLIS + (3600 * 12) * 1000L

        when: "Computing DayIndex"
        final DayIndex dayIndex = service.computeDayIndex(nearLeapSecond)

        then: "Should compute correctly (leap seconds handled by JVM)"
        Log.tryLog(DayIndexServiceSpec.class, "Leap second test:",
                "epochMillis:", nearLeapSecond,
                "Computed dayNum:", dayIndex.dayNum,
                "Expected: 0 (noon on epoch day)")
        dayIndex.dayNum == 0
    }

    def "should be consistent for same instant computed multiple times"() {
        given: "A specific timestamp"
        final long millis = DayIndex.EPOCH_MILLIS + (100 * 86400 + 12 * 3600) * 1000L

        when: "Computing DayIndex multiple times"
        final def results = (1..1000).collect { service.computeDayIndex(millis) }

        then: "All results should be identical"
        results.every { it.dayNum == results[0].dayNum }
    }

    // -------------------------------------------------------------------------
    // today() method
    // -------------------------------------------------------------------------

    def "should compute today's DayIndex"() {
        when: "Getting today"
        final DayIndex today = service.today()

        then: "Should return valid DayIndex"
        Log.tryLog(DayIndexServiceSpec.class, "Today test:",
                "today:", today,
                "dayNum:", today?.dayNum)

        today != null
        today.dayNum >= 0  // the app epoch is always in the past
        today.dayNum <= 7300   // reasonable bounds: ~15 years in the future. It will be a glorious day when this test breaks ^-^
    }


    def "should compute today with custom zone"() {
        when: "Getting today in different zones"
        final DayIndex utcToday = service.today(utcZone)
        final DayIndex estToday = service.today(estZone)

        then: "Should return valid DayIndex"
        utcToday != null
        estToday != null
        // Depending on current time, they might differ by 1 day
        Math.abs(utcToday.dayNum - estToday.dayNum) <= 1
    }

    // -------------------------------------------------------------------------
    // Integration tests with computeDayStart/End
    // -------------------------------------------------------------------------

    def "should have consistent start/end timestamps"() {
        given: "DayIndex 0"
        final DayIndex dayIndex = DayIndex.of(0)

        when: "Computing start and end"
        final long start = service.computeDayStart(dayIndex, utcZone, 4)
        final long end = service.computeDayEnd(dayIndex, utcZone, 4)

        then: "End should be just before next day's start"
        end == service.computeDayStart(dayIndex.plusDays(1), utcZone, 4) - 1
        end - start + 1 == 86400000L  // 24 hours
    }

    def "should have day 0 start at 4am on epoch date"() {
        given: "DayIndex 0"
        final DayIndex dayIndex = DayIndex.of(0)

        when: "Computing start timestamp"
        final long start = service.computeDayStart(dayIndex, utcZone, 4)

        then: "Should be 4am on 2025-10-10 UTC"
        start == DayIndex.EPOCH_MILLIS + (4 * 3600 * 1000L)
    }
}
