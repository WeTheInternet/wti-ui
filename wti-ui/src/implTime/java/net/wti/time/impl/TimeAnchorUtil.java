package net.wti.time.impl;

import jdk.jfr.internal.LogLevel;
import net.wti.time.api.ModelDay;
import net.wti.time.api.TimeAnchor;
import net.wti.time.api.TimeAnchorKind;
import xapi.fu.log.Log;

/// TimeAnchorUtil
///
/// Utilities for working with TimeAnchor:
/// - validate(): ensure required fields are present for each TimeAnchorKind
/// - computeDeadlineMillis(): compute an absolute deadline inside a ModelDay window
///
/// IMPORTANT: For now, only DAILY is fully supported. WEEKLY / MONTHLY / YEARLY
/// will throw UnsupportedOperationException until we define exact semantics.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:57
public final class TimeAnchorUtil {

    private TimeAnchorUtil() {
        // utility
    }

    public static void validate(TimeAnchor anchor) {
        if (anchor == null) {
            throw new IllegalArgumentException("TimeAnchor must not be null");
        }
        final TimeAnchorKind kind = anchor.getKind();
        if (kind == null) {
            throw new IllegalArgumentException("TimeAnchor.kind must not be null");
        }
        final Integer hour = anchor.getHour();
        final Integer minute = anchor.getMinute();

        switch (kind) {
            case DAILY:
                requireNotNull(hour, "hour", kind);
                requireNotNull(minute, "minute", kind);
                break;

            case WEEKLY:
                requireNotNull(hour, "hour", kind);
                requireNotNull(minute, "minute", kind);
                requireNotNull(anchor.getDayOfWeek(), "dayOfWeek", kind);
                break;

            case MONTHLY:
                requireNotNull(hour, "hour", kind);
                requireNotNull(minute, "minute", kind);
                requireNotNull(anchor.getDayOfMonth(), "dayOfMonth", kind);
                break;

            case YEARLY:
                requireNotNull(hour, "hour", kind);
                requireNotNull(minute, "minute", kind);
                requireNotNull(anchor.getDayOfYear(), "dayOfYear", kind);
                break;

            default:
                throw new IllegalArgumentException("Unknown TimeAnchorKind: " + kind);
        }
    }

    private static void requireNotNull(Object value, String field, TimeAnchorKind kind) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "TimeAnchor." + field + " must not be null for kind " + kind
            );
        }
    }

    /**
     * Computes an absolute deadline inside the given ModelDay window.
     *
     * CURRENT BEHAVIOR:
     * - DAILY: hour/minute are interpreted as offset from ModelDay.startTimestamp().
     *          i.e. 0:00 is exactly startTimestamp, 1:00 is +1h from start, etc.
     *
     * - WEEKLY / MONTHLY / YEARLY: not yet supported; throws UnsupportedOperationException.
     */
    public static long computeDeadlineMillis(ModelDay day, TimeAnchor anchor) {
        if (day == null) {
            throw new IllegalArgumentException("ModelDay must not be null");
        }
        validate(anchor);

        switch (anchor.getKind()) {
            case DAILY:
                return computeDailyDeadline(day, anchor);

            case WEEKLY:
            case MONTHLY:
            case YEARLY:
                throw new UnsupportedOperationException(
                        "TimeAnchorKind " + anchor.getKind() + " not yet implemented. " +
                                "Define semantics in TimeAnchorUtil before using."
                );

            default:
                throw new IllegalStateException("Unhandled TimeAnchorKind: " + anchor.getKind());
        }
    }

    private static long computeDailyDeadline(ModelDay day, TimeAnchor anchor) {
        final long start = day.startTimestamp();
        final int hour = anchor.getHour();
        final int minute = anchor.getMinute();

        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be 0–23; got " + hour);
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("minute must be 0–59; got " + minute);
        }

        final long offsetMinutes = (long) hour * 60L + (long) minute;
        final long millis = start + offsetMinutes * 60_000L;

        if (millis < day.startTimestamp() || millis > day.endTimestamp()) {
            // This can happen on weird DST days; log for now, but still return.
            Log.tryLog(TimeAnchorUtil.class, null, LogLevel.DEBUG, "TimeAnchorUtil.computeDailyDeadline: computed millis [", millis,
                    "] outside [", day.startTimestamp(), ", ", day.endTimestamp(),
                    "] for anchor ", anchor, " and day ", day);
        }

        return millis;
    }
}
