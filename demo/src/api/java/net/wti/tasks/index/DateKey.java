package net.wti.tasks.index;

import xapi.time.api.TimeComponents;

///
/// DateKey:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 03/10/2025 @ 04:44
public class DateKey {

    private final TimeComponents time;
    private final int year;
    private final int dayOfYear;

    private DateKey(TimeComponents time) {
        this.time = time;
        this.year = time.getYear();
        this.dayOfYear = time.getDayOfYear();
    }

    public static DateKey from(TimeComponents tc) {
        return new DateKey(tc);
    }

    public TimeComponents getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateKey)) return false;
        DateKey that = (DateKey) o;
        return year == that.year && dayOfYear == that.dayOfYear;
    }

    @Override
    public int hashCode() {
        int result = year;
        result = 31 * result + dayOfYear;
        return result;
    }

    @Override
    public String toString() {
        return "DateKey{" + year + ":" + dayOfYear + "}";
    }

    public DateKey minusDays(final int daysToSubtract) {
        return plusDays(-daysToSubtract);
    }

    public DateKey plusDays(final int daysToAdd) {
        long millisToAdd = daysToAdd * 24L * 60L * 60L * 1000L;
        double newEpochMillis = time.getEpochMillis() + millisToAdd;
        // TODO: get this from a cache
        TimeComponents adjusted = new TimeComponents(newEpochMillis, time.getZone());
        return DateKey.from(adjusted);
    }
}