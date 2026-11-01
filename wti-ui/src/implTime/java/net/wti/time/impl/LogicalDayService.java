package net.wti.time.impl;

import net.wti.time.api.DayIndex;
import xapi.time.api.TimeZoneInfo;

/// Explicit local-calendar day calculations for new callers.
/// Day zero is the UTC calendar date represented by the historical numeric
/// DayIndex.EPOCH_MILLIS (2025-10-11), not the inconsistent EPOCH_DATE text.
/// Existing keys, constants and DayIndexService behavior are deliberately unchanged.
/// Timezone rules come from the supplied TimeZoneInfo.getOffsetAt implementation;
/// platform adapters must supply accurate rules for their supported zones.
public final class LogicalDayService {
    private static final long DAY_MILLIS = 86400000L;
    private static final long EPOCH_DAY = Math.floorDiv(DayIndex.EPOCH_MILLIS, DAY_MILLIS);
    private static final int MAX_OFFSET_MILLIS = 18 * 3600000;

    private final TimeZoneInfo zone;
    private final int rolloverHour;

    /// Requires an explicit zone and local rollover hour; selects no household defaults.
    public LogicalDayService(final TimeZoneInfo zone, final int rolloverHour) {
        if (zone == null || zone.getId() == null || zone.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("An identified timezone is required");
        }
        if (rolloverHour < 0 || rolloverHour > 23) {
            throw new IllegalArgumentException("rolloverHour must be 0-23");
        }
        validateOffset(zone.getOffsetMillis());
        this.zone = zone;
        this.rolloverHour = rolloverHour;
    }

    /// Resolves the local calendar date, assigning times before rollover to yesterday.
    /// Floors negative dates correctly and rejects timestamps outside the int day range.
    /// DST offsets are looked up at the actual instant, never at the app epoch.
    public DayIndex dayIndex(final long epochMillis) {
        final int offset = zone.getOffsetAt(epochMillis);
        validateOffset(offset);
        try {
            final long localMillis = Math.addExact(epochMillis, offset);
            final long logicalMillis = Math.subtractExact(localMillis, rolloverHour * 3600000L);
            return checkedIndex(Math.floorDiv(logicalMillis, DAY_MILLIS) - EPOCH_DAY);
        } catch (final ArithmeticException failure) {
            throw new IllegalArgumentException("Timestamp is outside the supported day range", failure);
        }
    }

    /// Encodes this logical calendar date as UTC midnight for labels/date pickers.
    /// This is a date representation, not the actual instant of local rollover.
    public long dateMillis(final DayIndex dayIndex) {
        if (dayIndex == null) throw new IllegalArgumentException("dayIndex is required");
        return DayIndex.EPOCH_MILLIS + dayIndex.getDayNum() * DAY_MILLIS;
    }

    /// Converts a calendar date encoded as UTC midnight, independent of this zone/hour.
    /// Rejects times of day so a date-picker conversion cannot silently change dates.
    public DayIndex dateIndex(final long utcMidnightMillis) {
        if (Math.floorMod(utcMidnightMillis, DAY_MILLIS) != 0) {
            throw new IllegalArgumentException("Calendar date must be encoded as UTC midnight");
        }
        return checkedIndex(Math.floorDiv(utcMidnightMillis, DAY_MILLIS) - EPOCH_DAY);
    }

    /// Returns the explicitly supplied timezone rules.
    public TimeZoneInfo getZone() { return zone; }

    /// Returns the explicitly supplied local rollover hour.
    public int getRolloverHour() { return rolloverHour; }

    private static DayIndex checkedIndex(final long dayNum) {
        if (dayNum < Integer.MIN_VALUE || dayNum > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Date is outside the supported day range");
        }
        return DayIndex.of((int) dayNum);
    }

    private static void validateOffset(final int offset) {
        if (offset < -MAX_OFFSET_MILLIS || offset > MAX_OFFSET_MILLIS) {
            throw new IllegalArgumentException("Timezone offset must be within 18 hours of UTC");
        }
    }
}
