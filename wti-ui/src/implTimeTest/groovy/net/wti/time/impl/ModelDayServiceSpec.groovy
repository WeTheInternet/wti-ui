package net.wti.time.impl

import net.wti.time.api.DayIndex
import net.wti.time.api.ModelDay
import spock.lang.Specification
import spock.lang.Unroll
import xapi.time.api.TimeZoneInfo

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/// DayServiceSpec
///
/// Comprehensive tests for DayService caching and factory behavior:
/// - Cache hit/miss behavior
/// - Cache key generation (dayNum + zone + rolloverHour)
/// - getOrCreateModelDay with various parameter combinations
/// - ModelDay lazy computation behavior
/// - Cache clearing
/// - Concurrent access (cache safety)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2025-11-18
class ModelDayServiceSpec extends Specification {

    ModelDayService service
    DayIndexService indexService
    TimeZoneInfo utcZone
    TimeZoneInfo estZone
    TimeZoneInfo pstZone

    def setup() {
        utcZone = new TimeZoneInfo("UTC", "UTC", 0, false)
        estZone = new TimeZoneInfo("America/New_York", "Eastern", -5 * 3600000, true)
        pstZone = new TimeZoneInfo("America/Los_Angeles", "Pacific", -8 * 3600000, true)

        indexService = new DayIndexService(utcZone, 4)
        service = new ModelDayService(indexService)
    }

    // -------------------------------------------------------------------------
    // Basic cache behavior
    // -------------------------------------------------------------------------

    def "should cache ModelDay instances"() {
        given: "A DayIndex"
        DayIndex dayIndex = DayIndex.of(0)

        when: "Getting ModelDay twice"
        ModelDay first = service.getOrCreateModelDay(dayIndex)
        ModelDay second = service.getOrCreateModelDay(dayIndex)

        then: "Should return same instance (cached)"
        first.is(second)
        service.cacheSize == 1
    }

    def "should create separate cache entries for different DayIndex"() {
        given: "Two different DayIndex values"
        DayIndex day0 = DayIndex.of(0)
        DayIndex day1 = DayIndex.of(1)

        when: "Getting ModelDay for both"
        ModelDay modelDay0 = service.getOrCreateModelDay(day0)
        ModelDay modelDay1 = service.getOrCreateModelDay(day1)

        then: "Should be different instances"
        !modelDay0.is(modelDay1)
        service.cacheSize == 2
    }

    def "should create separate cache entries for different zones"() {
        given: "Same DayIndex, different zones"
        DayIndex dayIndex = DayIndex.of(0)

        when: "Getting ModelDay with different zones"
        ModelDay utcDay = service.getOrCreateModelDay(dayIndex, utcZone)
        ModelDay estDay = service.getOrCreateModelDay(dayIndex, estZone)

        then: "Should be different instances"
        !utcDay.is(estDay)
        service.cacheSize == 2
        utcDay.zone == utcZone
        estDay.zone == estZone
    }

    def "should create separate cache entries for different rolloverHours"() {
        given: "Same DayIndex and zone, different rolloverHour"
        DayIndex dayIndex = DayIndex.of(0)

        when: "Getting ModelDay with different rolloverHours"
        ModelDay rollover4 = service.getOrCreateModelDay(dayIndex, utcZone, 4)
        ModelDay rollover12 = service.getOrCreateModelDay(dayIndex, utcZone, 12)

        then: "Should be different instances"
        !rollover4.is(rollover12)
        service.cacheSize == 2
        rollover4.rolloverHour == 4
        rollover12.rolloverHour == 12
    }

    def "should clear cache"() {
        given: "Cache with some entries"
        service.getOrCreateModelDay(DayIndex.of(0))
        service.getOrCreateModelDay(DayIndex.of(1))
        service.getOrCreateModelDay(DayIndex.of(2))

        when: "Clearing cache"
        service.clearCache()

        then: "Cache should be empty"
        service.cacheSize == 0
    }

    def "should repopulate cache after clearing"() {
        given: "Cache with entry"
        DayIndex dayIndex = DayIndex.of(0)
        ModelDay original = service.getOrCreateModelDay(dayIndex)

        when: "Clearing and re-fetching"
        service.clearCache()
        ModelDay afterClear = service.getOrCreateModelDay(dayIndex)

        then: "Should create new instance"
        !original.is(afterClear)
        service.cacheSize == 1
    }

    // -------------------------------------------------------------------------
    // getOrCreateModelDay variations
    // -------------------------------------------------------------------------

    def "should create ModelDay from DayIndex (default zone/rollover)"() {
        given: "DayIndex"
        DayIndex dayIndex = DayIndex.of(5)

        when: "Getting ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(dayIndex)

        then: "Should use default zone and rolloverHour"
        modelDay.dayNum == 5
        modelDay.zone == utcZone
        modelDay.rolloverHour == 4
    }

    def "should create ModelDay from DayIndex with custom zone"() {
        given: "DayIndex"
        DayIndex dayIndex = DayIndex.of(5)

        when: "Getting ModelDay with custom zone"
        ModelDay modelDay = service.getOrCreateModelDay(dayIndex, estZone)

        then: "Should use custom zone"
        modelDay.dayNum == 5
        modelDay.zone == estZone
        modelDay.rolloverHour == 4 // still default rollover
    }

    def "should create ModelDay from DayIndex with custom zone and rolloverHour"() {
        given: "DayIndex"
        DayIndex dayIndex = DayIndex.of(5)

        when: "Getting ModelDay with custom zone and rolloverHour"
        ModelDay modelDay = service.getOrCreateModelDay(dayIndex, estZone, 12)

        then: "Should use both custom values"
        modelDay.dayNum == 5
        modelDay.zone == estZone
        modelDay.rolloverHour == 12
    }

    def "should create ModelDay from epoch millis (default zone/rollover)"() {
        given: "Epoch millis for a specific time"
        long epochMillis = DayIndex.EPOCH_MILLIS + (12 * 3600 * 1000L) // noon on epoch date

        when: "Getting ModelDay from millis"
        ModelDay modelDay = service.getOrCreateModelDay((double) epochMillis)

        then: "Should compute DayIndex and create ModelDay"
        modelDay.dayNum == 0 // noon on epoch date is still day 0 (day 0 runs from 4am on epoch date)
        modelDay.zone == utcZone
    }

    def "should create ModelDay from epoch millis with custom zone"() {
        given: "Epoch millis"
        long epochMillis = DayIndex.EPOCH_MILLIS + (12 * 3600 * 1000L)

        when: "Getting ModelDay with custom zone"
        ModelDay modelDay = service.getOrCreateModelDay((double) epochMillis, estZone)

        then: "Should compute DayIndex in that zone"
        modelDay.zone == estZone
    }

    def "should create ModelDay from epoch millis with custom zone and rolloverHour"() {
        given: "Epoch millis"
        long epochMillis = DayIndex.EPOCH_MILLIS + (12 * 3600 * 1000L)

        when: "Getting ModelDay with custom zone and rolloverHour"
        ModelDay modelDay = service.getOrCreateModelDay((double) epochMillis, estZone, 12)

        then: "Should use both custom values"
        modelDay.zone == estZone
        modelDay.rolloverHour == 12
    }

    def "should get today's ModelDay (default zone)"() {
        when: "Getting today"
        ModelDay today = service.today()

        then: "Should return valid ModelDay"
        today != null
        today.dayNum != null
        today.zone == utcZone
    }

    def "should get today's ModelDay (custom zone)"() {
        when: "Getting today with custom zone"
        ModelDay today = service.today(estZone)

        then: "Should use custom zone"
        today.zone == estZone
    }

    def "should cache today's ModelDay"() {
        when: "Getting today twice"
        ModelDay first = service.today()
        ModelDay second = service.today()

        then: "Should return same instance"
        first.is(second)
    }

    // -------------------------------------------------------------------------
    // ModelDay field computation
    // -------------------------------------------------------------------------

    def "should compute ModelDay fields lazily"() {
        given: "A ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(DayIndex.of(0))

        when: "Accessing computed fields"
        int dayOfWeek = modelDay.dayOfWeek()
        int dayOfMonth = modelDay.dayOfMonth()
        int dayOfYear = modelDay.dayOfYear()
        String dayName = modelDay.dayName()
        long duration = modelDay.durationMillis()

        then: "All fields should be computed"
        dayOfWeek >= 0 && dayOfWeek <= 6
        dayOfMonth >= 1 && dayOfMonth <= 31
        dayOfYear >= 1 && dayOfYear <= 366
        dayName != null
        duration > 0
    }

    def "should cache computed fields"() {
        given: "A ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(DayIndex.of(0))

        when: "Accessing same field twice"
        String name1 = modelDay.dayName()
        String name2 = modelDay.dayName()

        then: "Should return same instance (cached)"
        name1.is(name2)
    }

    def "should compute startComponents lazily"() {
        given: "A ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(DayIndex.of(0))

        when: "Accessing startComponents"
        def components = modelDay.startComponents()

        then: "Should be computed"
        components != null
        components.epochMillis == modelDay.startTimestamp
    }

    def "should compute endComponents lazily"() {
        given: "A ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(DayIndex.of(0))

        when: "Accessing endComponents"
        def components = modelDay.endComponents()

        then: "Should be computed"
        components != null
        components.epochMillis == modelDay.endTimestamp
    }

    // -------------------------------------------------------------------------
    // ModelDay contains() method
    // -------------------------------------------------------------------------

    def "should correctly identify if timestamp is within day"() {
        given: "A ModelDay for day 0"
        ModelDay modelDay = service.getOrCreateModelDay(DayIndex.of(0), utcZone, 4)
        long start = modelDay.startTimestamp()
        long end = modelDay.endTimestamp()

        expect: "Timestamps within range return true"
        modelDay.contains(start)
        modelDay.contains(start + 1000)
        modelDay.contains(end)
        modelDay.contains((start + end) / 2.0d)

        and: "Timestamps outside range return false"
        !modelDay.contains(start - 1)
        !modelDay.contains(end + 1)
    }

    // -------------------------------------------------------------------------
    // Edge cases and consistency
    // -------------------------------------------------------------------------

    def "should handle negative DayIndex"() {
        given: "Negative DayIndex"
        DayIndex dayIndex = DayIndex.of(-10)

        when: "Getting ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(dayIndex)

        then: "Should create valid ModelDay"
        modelDay.dayNum == -10
        modelDay.startTimestamp < DayIndex.EPOCH_MILLIS
    }

    def "should handle large positive DayIndex"() {
        given: "Large positive DayIndex (100 years in future)"
        DayIndex dayIndex = DayIndex.of(36525) // ~100 years

        when: "Getting ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(dayIndex)

        then: "Should create valid ModelDay"
        modelDay.dayNum == 36525
    }

    def "should handle large negative DayIndex"() {
        given: "Large negative DayIndex (100 years in past)"
        DayIndex dayIndex = DayIndex.of(-36525)

        when: "Getting ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(dayIndex)

        then: "Should create valid ModelDay"
        modelDay.dayNum == -36525
    }

    def "should generate correct keyPrefix"() {
        given: "ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay(DayIndex.of(5))

        when: "Getting keyPrefix"
        String prefix = modelDay.keyPrefix()

        then: "Should match DayIndex format"
        prefix == "dy/5"
    }

    @Unroll
    def "should handle consecutive days #day1 and #day2"() {
        given: "Two consecutive DayIndex values"
        DayIndex index1 = DayIndex.of(day1)
        DayIndex index2 = DayIndex.of(day2)

        when: "Getting ModelDay for both"
        ModelDay modelDay1 = service.getOrCreateModelDay(index1)
        ModelDay modelDay2 = service.getOrCreateModelDay(index2)

        then: "End of day1 should be adjacent to start of day2"
        modelDay1.endTimestamp() + 1 == modelDay2.startTimestamp()

        where:
        day1 | day2
        0    | 1
        -1   | 0
        99   | 100
        -100 | -99
    }

    // -------------------------------------------------------------------------
    // Concurrent access
    // -------------------------------------------------------------------------

    def "should be thread-safe for concurrent cache access"() {
        given: "Multiple threads trying to access same ModelDay"
        DayIndex dayIndex = DayIndex.of(0)
        List<ModelDay> results = Collections.synchronizedList(new ArrayList<>())

        when: "Accessing concurrently"
        def threads = (1..10).collect { threadNum ->
            Thread.start {
                results.add(service.getOrCreateModelDay(dayIndex))
            }
        }
        threads*.join()

        then: "All threads should get same instance"
        results.size() == 10
        results.every { it.is(results[0]) }
        service.cacheSize == 1
    }

    def "should be thread-safe for concurrent different day access"() {
        given: "Multiple threads accessing different days"
        List<ModelDay> results = Collections.synchronizedList(new ArrayList<>())

        when: "Accessing different days concurrently"
        def threads = (0..9).collect { dayNum ->
            Thread.start {
                results.add(service.getOrCreateModelDay(DayIndex.of(dayNum)))
            }
        }
        threads*.join()

        then: "Should create 10 different ModelDay instances"
        results.size() == 10
        service.cacheSize == 10
        results.collect { it.dayNum }.toSet().size() == 10
    }

    // -------------------------------------------------------------------------
    // Integration with DayIndexService
    // -------------------------------------------------------------------------

    def "should use DayIndexService for epoch millis conversion"() {
        given: "Epoch millis at 3:59am (before rollover)"
        long before4am = DayIndex.EPOCH_MILLIS + (3 * 3600 + 59 * 60) * 1000L

        when: "Getting ModelDay"
        ModelDay modelDay = service.getOrCreateModelDay((double) before4am, utcZone, 4)

        then: "Should be day 0 (before rollover)"
        modelDay.dayNum == 0
    }

    def "should use DayIndexService for DST handling"() {
        given: "EST zone and spring forward day"
        ZonedDateTime dstDay = ZonedDateTime.of(
                LocalDateTime.of(2024, 3, 10, 12, 0),
                ZoneId.of("America/New_York")
        )

        when: "Getting ModelDay for DST day"
        ModelDay modelDay = service.getOrCreateModelDay(dstDay.toInstant().toEpochMilli(), estZone, 4)

        then: "Should create valid ModelDay"
        modelDay != null
        modelDay.zone == estZone
    }

    // -------------------------------------------------------------------------
    // Cache key uniqueness
    // -------------------------------------------------------------------------

    def "should create unique cache keys for all parameter combinations"() {
        given: "Various parameter combinations"
        DayIndex day0 = DayIndex.of(0)
        DayIndex day1 = DayIndex.of(1)

        when: "Creating ModelDay instances with different combinations"
        def m1 = service.getOrCreateModelDay(day0, utcZone, 4)
        def m2 = service.getOrCreateModelDay(day0, utcZone, 12)
        def m3 = service.getOrCreateModelDay(day0, estZone, 4)
        def m4 = service.getOrCreateModelDay(day0, estZone, 12)
        def m5 = service.getOrCreateModelDay(day1, utcZone, 4)
        def m6 = service.getOrCreateModelDay(day1, utcZone, 12)
        def m7 = service.getOrCreateModelDay(day1, estZone, 4)
        def m8 = service.getOrCreateModelDay(day1, estZone, 12)

        then: "All should be different instances"
        def instances = [m1, m2, m3, m4, m5, m6, m7, m8]
        instances.collect { System.identityHashCode(it) }.toSet().size() == 8
        service.cacheSize == 8
    }
}
