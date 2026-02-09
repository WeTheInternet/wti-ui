package net.wti.time.api;

import xapi.time.X_Time;

import static xapi.time.X_Time.*;

///
/// DurationUnit:
///
/// A set duration enums for time values suited to human consumption,
/// ranging from seconds to years; even seconds is likely overkill.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:35
public enum DurationUnit {
    SECOND,
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR
    ;

    public long toMillis(long amt) {
        switch (this) {
            case SECOND:
                return amt * 1000L;
            case MINUTE:
                return amt * ONE_MINUTE;
            case HOUR:
                return amt * ONE_HOUR;
            case DAY:
                return amt * ONE_DAY;
            case MONTH:
                return amt * 30L * ONE_DAY;
            case WEEK:
                return amt * 7L * ONE_DAY;
            case YEAR:
                return amt * 365L * ONE_DAY;
            default:
                throw new IllegalStateException("Unhandled unit: " + this);
        }
    }

}
