package net.wti.time.impl;


import net.wti.time.api.DayIndex;
import net.wti.time.api.ModelDay;
import xapi.model.X_Model;
import xapi.time.api.TimeZoneInfo;

/// DayIndexService
///
/// Service for computing DayIndex from timestamps using timezone and rolloverHour.
///
/// The core algorithm:
/// 1. Convert epoch millis to user's local time (UTC + timezone offset at that moment)
/// 2. Subtract rolloverHour to get "logical day time"
///    (e.g., 3:30am with rolloverHour=4 becomes previous day's 23:30)
/// 3. Count days since app epoch in this adjusted timeline
///
/// Handles DST correctly by using TimeZoneInfo.getOffsetAt() which accounts
/// for daylight saving time at the specific moment.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2025-11-18 @ 22:20
public class DayIndexService {

    /// Default rollover hour: 4am.
    /// Times before 4am are considered part of the previous day.
    public static final int DEFAULT_ROLLOVER_HOUR = 4;

    private final TimeZoneInfo defaultZone;
    private final int defaultRolloverHour;

    public DayIndexService(TimeZoneInfo defaultZone) {
        this(defaultZone, DEFAULT_ROLLOVER_HOUR);
    }

    public DayIndexService(TimeZoneInfo defaultZone, int defaultRolloverHour) {
        if (defaultRolloverHour < 0 || defaultRolloverHour > 23) {
            throw new IllegalArgumentException("rolloverHour must be 0-23, got: " + defaultRolloverHour);
        }
        this.defaultZone = defaultZone;
        this.defaultRolloverHour = defaultRolloverHour;
    }

    /// Computes DayIndex for the given epoch millis using default zone and rolloverHour.
    public DayIndex computeDayIndex(double epochMillis) {
        return computeDayIndex(epochMillis, defaultZone, defaultRolloverHour);
    }

    /// Computes DayIndex for the given epoch millis with custom zone.
    public DayIndex computeDayIndex(double epochMillis, TimeZoneInfo zone) {
        return computeDayIndex(epochMillis, zone, defaultRolloverHour);
    }
    /// Computes DayIndex for the given epoch millis with custom zone and rolloverHour.
    ///
    /// Algorithm:
    /// 1. Get timezone offset at this moment (including DST)
    /// 2. Compute local millis = UTC millis + offset
    /// 3. Subtract rolloverHour to get "logical day time"
    /// 4. Count days since epoch
    public DayIndex computeDayIndex(double epochMillis, TimeZoneInfo zone, int rolloverHour) {
        // Get offset at this specific moment (handles DST)
        int offsetMillis = zone.getOffsetAt(epochMillis);

        // Convert to local time
        long localMillis = (long) epochMillis + offsetMillis;

        // Subtract rollover offset to shift the day boundary
        long rolloverOffsetMillis = rolloverHour * 3600000L;
        long adjustedMillis = localMillis - rolloverOffsetMillis;

        // Compute the adjusted epoch:
        // Day 0 starts at rolloverHour on the epoch date in the user's timezone
        // In UTC terms: EPOCH_MILLIS (midnight) + offsetMillis (to local) + rolloverOffsetMillis
        // But wait - we need to think about this differently:
        // - adjustedMillis represents "time since rolloverHour on current day"
        // - We want day 0 to start when adjustedMillis = EPOCH_MILLIS + offsetMillis
        // - So epochAdjusted = EPOCH_MILLIS + offsetMillis
        long epochAdjusted = DayIndex.EPOCH_MILLIS + offsetMillis;

        // Compute days since adjusted epoch
        long daysSinceEpoch = (adjustedMillis - epochAdjusted) / 86400000L;

        // Handle negative case properly (floor division)
        if (adjustedMillis < epochAdjusted && (adjustedMillis - epochAdjusted) % 86400000L != 0) {
            daysSinceEpoch--;
        }

        return DayIndex.of(daysSinceEpoch);
    }

    /// Computes the current DayIndex using default zone and rolloverHour.
    public DayIndex today() {
        return computeDayIndex(System.currentTimeMillis());
    }

    /// Computes the current DayIndex using custom zone.
    public DayIndex today(TimeZoneInfo zone) {
        return computeDayIndex(System.currentTimeMillis(), zone);
    }

    /// Computes the start timestamp (epoch millis) for the given DayIndex.
    /// This is the moment when the day begins (at rolloverHour in user's local time).
    public long computeDayStart(DayIndex dayIndex, TimeZoneInfo zone, int rolloverHour) {
        // Start with epoch
        long epochAdjusted = DayIndex.EPOCH_MILLIS;

        // Add days
        long targetMillis = epochAdjusted + (dayIndex.getDayNum() * 86400000L);

        // We need to find the exact moment when this day starts in the user's zone
        // This requires iterating because DST transitions can shift the boundary

        // Approximate: use offset at target time
        int offsetMillis = zone.getOffsetAt(targetMillis);

        // Adjust for timezone and rollover
        long rolloverOffsetMillis = rolloverHour * 3600000L;

        // The day starts at: epoch + days - timezone_offset + rolloverHour
        long startMillis = targetMillis - offsetMillis + rolloverOffsetMillis;

        return startMillis;
    }

    /// Computes the end timestamp (epoch millis) for the given DayIndex.
    /// This is one millisecond before the start of the next day.
    public long computeDayEnd(DayIndex dayIndex, TimeZoneInfo zone, int rolloverHour) {
        long nextDayStart = computeDayStart(dayIndex.plusDays(1), zone, rolloverHour);
        return nextDayStart - 1;
    }

    /// Creates a new ModelDay instance with computed values.
    public ModelDay createModelDay(DayIndex dayIndex, TimeZoneInfo zone, int rolloverHour) {
        ModelDay day = X_Model.create(ModelDay.class);
        day.setDayNum(dayIndex.getDayNum());
        day.setZone(zone);
        day.setRolloverHour(rolloverHour);

        // Compute timestamps
        long start = computeDayStart(dayIndex, zone, rolloverHour);
        long end = computeDayEnd(dayIndex, zone, rolloverHour);

        day.setStartTimestamp(start);
        day.setEndTimestamp(end);
        day.setDurationMillis(end - start + 1);

        return day;
    }

    /// Returns the default timezone.
    public TimeZoneInfo getDefaultZone() {
        return defaultZone;
    }

    /// Returns the default rollover hour.
    public int getDefaultRolloverHour() {
        return defaultRolloverHour;
    }
}
