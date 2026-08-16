package net.wti.time.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.time.api.TimeComponents;
import xapi.time.api.TimeZoneInfo;

/// ModelDay
///
/// A logical model representing a single day's window in the LifeQuest system.
/// Keyed by DayIndex and computed lazily from timezone and rolloverHour.
///
/// Contains derived components:
/// - startTimestamp, endTimestamp (DST-safe, may be >24h span during transitions)
/// - dayOfWeek (0=Sunday, 6=Saturday)
/// - dayOfMonth (1-31)
/// - dayOfYear (1-366)
/// - dayName (localized, e.g., "Monday")
/// - timezoneId, rolloverHour used to compute this window
///
/// Used as the parent key for LiveQuest and history records:
/// - dy/{DayNum}/lv/* (live quests)
/// - dy/{DayNum}/dn/* (completed)
/// - dy/{DayNum}/fld/* (failed)
/// etc.
///
/// Can be persisted or computed-only (create-if-missing semantics).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2025-11-18 @ 22:14
@IsModel(
        modelType = ModelDay.MODEL_DAY,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ModelDay extends Model {
    String MODEL_DAY = "day";
    KeyBuilder KEY_BUILDER_DAY = KeyBuilder.build(MODEL_DAY).withType(ModelKey.KEY_TYPE_LONG);
    static ModelKey newKey(long day) {
        return KEY_BUILDER_DAY.buildKeyLong(day);
    }
    /// Returns the DayIndex this ModelDay represents.
    Integer getDayNum();
    void setDayNum(Integer dayNum);
    default DayIndex dayIndex() {
        Integer num = getDayNum();
        if (num == null) {
            throw new IllegalStateException("ModelDay has no dayNum set");
        }
        return DayIndex.of(num);
    }

    /// Returns the start timestamp (epoch millis) of this day.
    /// This is when the day begins at rolloverHour in the user's timezone.
    Long getStartTimestamp();
    void setStartTimestamp(Long startTimestamp);
    default long startTimestamp() {
        Long ts = getStartTimestamp();
        if (ts == null) {
            throw new IllegalStateException("ModelDay startTimestamp not computed");
        }
        return ts;
    }

    /// Returns the end timestamp (epoch millis) of this day.
    /// This is one millisecond before the start of the next day.
    Long getEndTimestamp();
    void setEndTimestamp(Long endTimestamp);
    default long endTimestamp() {
        Long ts = getEndTimestamp();
        if (ts == null) {
            throw new IllegalStateException("ModelDay endTimestamp not computed");
        }
        return ts;
    }

    /// Returns the duration of this day in milliseconds.
    /// Usually 86400000 (24 hours), but can be different during DST transitions.
    Long getDurationMillis();
    void setDurationMillis(Long durationMillis);
    default long durationMillis() {
        Long dur = getDurationMillis();
        if (dur == null) {
            dur = endTimestamp() - startTimestamp() + 1;
            setDurationMillis(dur);
        }
        return dur;
    }

    /// Returns day of week (0=Sunday, 1=Monday, ..., 6=Saturday).
    Integer getDayOfWeek();
    void setDayOfWeek(Integer dayOfWeek);
    default int dayOfWeek() {
        Integer dow = getDayOfWeek();
        if (dow == null) {
            dow = startComponents().getDayOfWeek();
            setDayOfWeek(dow);
        }
        return dow;
    }

    /// Returns day of month (1-31).
    Integer getDayOfMonth();
    void setDayOfMonth(Integer dayOfMonth);
    default int dayOfMonth() {
        Integer dom = getDayOfMonth();
        if (dom == null) {
            dom = startComponents().getDayOfMonth();
            setDayOfMonth(dom);
        }
        return dom;
    }

    /// Returns day of year (1-366).
    Integer getDayOfYear();
    void setDayOfYear(Integer dayOfYear);
    default int dayOfYear() {
        Integer doy = getDayOfYear();
        if (doy == null) {
            doy = startComponents().getDayOfYear();
            setDayOfYear(doy);
        }
        return doy;
    }

    /// Returns localized day name (e.g., "Monday").
    String getDayName();
    void setDayName(String dayName);
    default String dayName() {
        String name = getDayName();
        if (name == null) {
            name = computeDayName(dayOfWeek());
            setDayName(name);
        }
        return name;
    }

    /// Returns the timezone info used for this day.
    TimeZoneInfo getZone();
    void setZone(TimeZoneInfo zone);
    default TimeZoneInfo zone() {
        TimeZoneInfo zone = getZone();
        if (zone == null) {
            throw new IllegalStateException("ModelDay zone not set");
        }
        return zone;
    }

    /// Returns the rollover hour used for this day.
    Integer getRolloverHour();
    void setRolloverHour(Integer rolloverHour);
    default int rolloverHour() {
        Integer hour = getRolloverHour();
        if (hour == null) {
            hour = 4; // default
            setRolloverHour(hour);
        }
        return hour;
    }

    /// Returns the timezone ID string.
    default String timezoneId() {
        return zone().getId();
    }

    /// Returns the TimeComponents for the start of this day.
    xapi.time.api.TimeComponents getStartComponents();
    void setStartComponents(TimeComponents startComponents);
    default TimeComponents startComponents() {
        TimeComponents tc = getStartComponents();
        if (tc == null) {
            // TODO: use a cache
            tc = new TimeComponents(startTimestamp(), zone());
            setStartComponents(tc);
        }
        return tc;
    }

    /// Returns the TimeComponents for the end of this day.
    TimeComponents getEndComponents();
    void setEndComponents(TimeComponents endComponents);
    default TimeComponents endComponents() {
        TimeComponents tc = getEndComponents();
        if (tc == null) {
            // TODO: use a cache
            tc = new TimeComponents(endTimestamp(), zone());
            setEndComponents(tc);
        }
        return tc;
    }

    /// Returns the key for this ModelDay in the splaying scheme.
    /// Format: "dy/{DayNum}"
    default String keyPrefix() {
        return dayIndex().toKeyPrefix();
    }

    /// Checks if a given timestamp falls within this day's window.
    default boolean contains(double epochMillis) {
        long millis = (long) epochMillis;
        return millis >= startTimestamp() && millis <= endTimestamp();
    }

    static String computeDayName(int dayOfWeek) {
        String[] names = {"Sunday", "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday"};
        return names[dayOfWeek];
    }
}
