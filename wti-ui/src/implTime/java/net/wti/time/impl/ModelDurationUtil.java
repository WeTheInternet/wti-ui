package net.wti.time.impl;


import net.wti.time.api.DayIndex;
import net.wti.time.api.DurationUnit;
import net.wti.time.api.ModelDuration;

/// ModelDurationUtil
///
/// Utilities for applying ModelDuration to DayIndex.
///
/// NOTE:
/// - DAY and WEEK are implemented in terms of simple integer offsets.
/// - MONTH and YEAR currently throw UnsupportedOperationException, because
///   they should be defined in terms of calendar dates + rollover rules.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:58
public final class ModelDurationUtil {

    private ModelDurationUtil() {
        // utility
    }

    /**
     * Adds the given duration to the base DayIndex a number of times.
     *
     * @param base      Base DayIndex (not null).
     * @param duration  Duration model (amount + unit; not null).
     * @param times     How many times to apply (may be negative).
     */
    public static DayIndex add(DayIndex base, ModelDuration duration, int times) {
        if (base == null) {
            throw new IllegalArgumentException("base DayIndex must not be null");
        }
        if (duration == null) {
            throw new IllegalArgumentException("ModelDuration must not be null");
        }
        if (times == 0) {
            return base;
        }

        final Integer amountObj = duration.getAmount();
        if (amountObj == null) {
            throw new IllegalArgumentException("ModelDuration.amount must not be null");
        }
        final int amount = amountObj;
        final DurationUnit unit = duration.getUnit();
        if (unit == null) {
            throw new IllegalArgumentException("ModelDuration.unit must not be null");
        }

        final int baseNum = base.getDayNum();
        final int totalAmount = amount * times;

        switch (unit) {
            case DAY:
                return DayIndex.of(baseNum + totalAmount);

            case WEEK:
                return DayIndex.of(baseNum + totalAmount * 7);

            case MONTH:
            case YEAR:
                throw new UnsupportedOperationException(
                        "ModelDurationUtil.add: unit " + unit + " not yet implemented; " +
                                "define calendar-based behavior before using."
                );

            default:
                throw new IllegalStateException("Unhandled DurationUnit: " + unit);
        }
    }

    /**
     * Convenience: apply a single duration once (times=1).
     */
    public static DayIndex add(DayIndex base, ModelDuration duration) {
        return add(base, duration, 1);
    }
}
