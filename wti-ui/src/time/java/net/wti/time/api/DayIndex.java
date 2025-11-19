package net.wti.time.api;

import java.io.Serializable;
import java.util.Objects;

/// DayIndex
///
/// Represents a day number relative to the app epoch (2025-10-10).
/// DayIndex=0 is the epoch date; negative values are before, positive after.
///
/// DayIndex boundaries are computed using a user's timezone and rolloverHour.
/// The default rolloverHour is 4 (4am), meaning times before 4am are
/// considered part of the previous day.
///
/// This is a value type - immutable and comparable.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2025-11-18
public final class DayIndex implements Serializable, Comparable<DayIndex> {
    private static final long serialVersionUID = 1L;

    /// App epoch: 2025-10-10 (the reference date for DayIndex=0)
    public static final String EPOCH_DATE = "2025-10-10";

    /// Epoch millis for 2025-10-10 00:00:00 UTC
    public static final long EPOCH_MILLIS = 1760140800000L;

    private final long dayNum;

    private DayIndex(long dayNum) {
        this.dayNum = dayNum;
    }

    /// Creates a DayIndex from a day number.
    /// @param dayNum number of days since epoch (can be negative)
    public static DayIndex of(long dayNum) {
        return new DayIndex(dayNum);
    }

    /// Returns DayIndex for the epoch date (2025-10-10).
    public static DayIndex epoch() {
        return new DayIndex(0);
    }

    /// Returns the day number (signed long).
    public long getDayNum() {
        return dayNum;
    }

    /// Returns a new DayIndex offset by the specified number of days.
    public DayIndex plusDays(long days) {
        return new DayIndex(dayNum + days);
    }

    /// Returns a new DayIndex offset by the specified number of days (negative).
    public DayIndex minusDays(long days) {
        return new DayIndex(dayNum - days);
    }

    /// Computes the number of days between this and another DayIndex.
    public long daysUntil(DayIndex other) {
        return other.dayNum - this.dayNum;
    }

    /// Returns true if this DayIndex is before the other.
    public boolean isBefore(DayIndex other) {
        return dayNum < other.dayNum;
    }

    /// Returns true if this DayIndex is after the other.
    public boolean isAfter(DayIndex other) {
        return dayNum > other.dayNum;
    }

    @Override
    public int compareTo(DayIndex other) {
        return Long.compare(dayNum, other.dayNum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DayIndex)) return false;
        DayIndex dayIndex = (DayIndex) o;
        return dayNum == dayIndex.dayNum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayNum);
    }

    @Override
    public String toString() {
        return "DayIndex{" + dayNum + "}";
    }

    /// Returns the key prefix for this day in the splaying scheme.
    /// Format: "dy/{DayNum}"
    public String toKeyPrefix() {
        return "dy/" + dayNum;
    }
}

