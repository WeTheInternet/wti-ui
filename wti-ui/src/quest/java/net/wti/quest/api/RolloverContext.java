package net.wti.quest.api;

/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:48

import net.wti.time.api.ModelDay;

/// RolloverContext
///
/// Immutable value object describing a rollover run:
///  - fromDay: the day being closed out
///  - toDay:   the day being opened
///  - now:     current time in epoch millis, used for deadline checks
///
/// Created by AI Assistant on 2025-12-08
public final class RolloverContext {

    private final ModelDay fromDay;
    private final ModelDay toDay;
    private final long nowMillis;

    public RolloverContext(ModelDay fromDay, ModelDay toDay, long nowMillis) {
        if (fromDay == null) {
            throw new IllegalArgumentException("fromDay must not be null");
        }
        if (toDay == null) {
            throw new IllegalArgumentException("toDay must not be null");
        }
        this.fromDay = fromDay;
        this.toDay = toDay;
        this.nowMillis = nowMillis;
    }

    public ModelDay getFromDay() {
        return fromDay;
    }

    public ModelDay getToDay() {
        return toDay;
    }

    public long getNowMillis() {
        return nowMillis;
    }
}
