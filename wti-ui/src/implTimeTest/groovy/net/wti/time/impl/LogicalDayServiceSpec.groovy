package net.wti.time.impl

import net.wti.time.api.DayIndex
import spock.lang.Specification
import spock.lang.Unroll
import xapi.time.api.TimeZoneInfo

import java.time.Instant

class LogicalDayServiceSpec extends Specification {
    private static TimeZoneInfo fixed(int minutes) {
        new TimeZoneInfo("fixed:$minutes", "Fixed test zone", minutes * 60000, false)
    }

    private static TimeZoneInfo platformZone(String id) {
        final TimeZone delegate = TimeZone.getTimeZone(id)
        new TimeZoneInfo(id, id, delegate.rawOffset, delegate.useDaylightTime()) {
            @Override int getOffsetAt(double millis) { delegate.getOffset((long) millis) }
        }
    }

    @Unroll
    def 'local rollover uses offset #minutes at relative instant #delta'() {
        expect:
        new LogicalDayService(fixed(minutes), rollover).dayIndex(DayIndex.EPOCH_MILLIS + delta).dayNum == expected

        where:
        minutes | rollover | delta            | expected
        0       | 4        | 14400000L - 1    | -1
        0       | 4        | 14400000L        | 0
        540     | 4        | 0L               | 0
        -480    | 4        | 0L               | -1
        -480    | 4        | 43200000L - 1    | -1
        -480    | 4        | 43200000L        | 0
        330     | 4        | -5400000L        | 0
        330     | 4        | -5400000L - 1    | -1
        840     | 0        | 43200000L        | 1
        -720    | 0        | -1L              | -1
        0       | 0        | -86400000L       | -1
        0       | 0        | -86400000L - 1   | -2
        0       | 23       | 82800000L - 1    | -1
        0       | 23       | 82800000L        | 0
    }

    @Unroll
    def 'platform DST offset at #instant respects local rollover #hour'() {
        given:
        def service = new LogicalDayService(platformZone('America/New_York'), hour)
        long date = Instant.parse(dateText + 'T00:00:00Z').toEpochMilli()

        expect:
        service.dayIndex(Instant.parse(instant).toEpochMilli()) == service.dateIndex(date)

        where:
        instant                | hour | dateText
        '2026-03-08T06:59:59Z' | 2    | '2026-03-07'
        '2026-03-08T07:00:00Z' | 2    | '2026-03-08'
        '2026-03-08T07:59:59Z' | 4    | '2026-03-07'
        '2026-03-08T08:00:00Z' | 4    | '2026-03-08'
        '2026-11-01T05:59:59Z' | 2    | '2026-10-31'
        '2026-11-01T06:00:00Z' | 2    | '2026-10-31'
        '2026-11-01T07:00:00Z' | 2    | '2026-11-01'
        '2026-11-01T08:59:59Z' | 4    | '2026-10-31'
        '2026-11-01T09:00:00Z' | 4    | '2026-11-01'
    }

    @Unroll
    def 'date encoding preserves signed day #number and numeric epoch'() {
        given:
        def service = new LogicalDayService(fixed(540), 4)

        expect:
        service.dateIndex(service.dateMillis(DayIndex.of(number))) == DayIndex.of(number)
        service.dayIndex(service.dateMillis(DayIndex.of(number))) == DayIndex.of(number)

        where:
        number << [Integer.MIN_VALUE, -30000, -365, -1, 0, 1, 365, Integer.MAX_VALUE]
    }

    def 'legacy constants and public calculation remain untouched'() {
        expect:
        DayIndex.EPOCH_DATE == '2025-10-10'
        DayIndex.EPOCH_MILLIS == Instant.parse('2025-10-11T00:00:00Z').toEpochMilli()
        new DayIndexService(fixed(540), 4).computeDayIndex(DayIndex.EPOCH_MILLIS).dayNum == -1
        new LogicalDayService(fixed(540), 4).dayIndex(DayIndex.EPOCH_MILLIS).dayNum == 0
    }

    @Unroll
    def 'reject invalid settings zone=#zone hour=#hour'() {
        when:
        new LogicalDayService(zone, hour)

        then:
        thrown(IllegalArgumentException)

        where:
        zone                                            | hour
        null                                            | 4
        new TimeZoneInfo(null, 'bad', 0, false)          | 4
        new TimeZoneInfo(' ', 'bad', 0, false)           | 4
        fixed(0)                                        | -1
        fixed(0)                                        | 24
        fixed(1081)                                     | 4
    }

    def 'reject nonmidnight dates and unsafe timestamp ranges'() {
        given:
        def service = new LogicalDayService(fixed(0), 4)

        when:
        service.dateIndex(DayIndex.EPOCH_MILLIS - 1)
        then:
        thrown(IllegalArgumentException)

        when:
        service.dayIndex(Long.MIN_VALUE)
        then:
        thrown(IllegalArgumentException)

        when:
        service.dayIndex(Long.MAX_VALUE)
        then:
        thrown(IllegalArgumentException)

        when:
        service.dateIndex((Integer.MAX_VALUE + 1L) * 86400000L + DayIndex.EPOCH_MILLIS)
        then:
        thrown(IllegalArgumentException)
    }
}
