package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.X_Model;
import xapi.model.api.Model;

/// ModelRecurrence:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 21:04
@IsModel(
        modelType = ModelRecurrence.MODEL_RECURRENCE
        ,persistence = @Persistent(strategy= PersistenceStrategy.Remote)
)
public interface ModelRecurrence extends Model {
    long NEVER = Long.MIN_VALUE;
    String MODEL_RECURRENCE = "rcr";
    int MINUTES_PER_DAY = 60 * 24;
    int MINUTES_PER_WEEK = MINUTES_PER_DAY * 7;

    RecurrenceUnit getUnit();
    ModelRecurrence setUnit(RecurrenceUnit unit);

    long getValue();
    ModelRecurrence setValue(long value);

    default DayOfWeek dayOfWeek() {
        assert getUnit() != RecurrenceUnit.ONCE : "Do not calculate .dayOfWeek() when unit==ONCE";
        final long value = getValue();
        long dayNum = (value - (value % MINUTES_PER_DAY)) / MINUTES_PER_DAY;
        return DayOfWeek.values()[(int)dayNum % 7];
    }
    default int hour() {
        assert getUnit() != RecurrenceUnit.ONCE : "Do not calculate .hour() when unit==ONCE";
        final long value = getValue();
        long dayMinutes = value % MINUTES_PER_DAY;
        long hours = (dayMinutes - (dayMinutes % 60)) / 60;
        return (int)hours;
    }
    default int minute() {
        assert getUnit() != RecurrenceUnit.ONCE : "Do not calculate .minute() when unit==ONCE";
        final long value = getValue();
        return (int)(value % 60);
    }

    static ModelRecurrence weekly(DayOfWeek dayOfWeek) {
        return weekly(dayOfWeek, ModelSettings.defaultHour(), ModelSettings.defaultMinute()); // noon
    }
    static ModelRecurrence weekly(DayOfWeek dayOfWeek, int hour, int minute) {
        assert hour >= 0 && hour < 27 : "Illegal hour: " + hour + " must be 0-26 (24, 25 and 26 represent 12am, 1am and 2am)";
        assert minute >= 0 && minute < 60 : "Illegal minute: " + minute + " must be 0-59";
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.WEEKLY);
        recur.setValue(dayOfWeek.ordinal() * MINUTES_PER_DAY + hour * 60 + minute);
        return recur;
    }
    static ModelRecurrence biweekly(DayOfWeek dayOfWeek) {
        return biweekly(dayOfWeek, 0);
    }
    static ModelRecurrence biweekly(DayOfWeek dayOfWeek, int offset) {
        return biweekly(dayOfWeek, offset, 20, 0); // 8 pm
    }
    static ModelRecurrence biweekly(DayOfWeek dayOfWeek, int offset, int hour, int minute) {
        assert offset == 0 || offset == 1 : "Illegal offset: " + offset + " must be 1 or 0";
        assert hour >= 0 && hour < 27 : "Illegal hour: " + hour + " must be 0-26 (24, 25 and 26 represent 12am, 1am and 2am)";
        assert minute >= 0 && minute < 60 : "Illegal minute: " + minute + " must be 0-59";
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.BIWEEKLY);
        recur.setValue( (offset * 7 + dayOfWeek.ordinal()) * MINUTES_PER_DAY + hour * 60 + minute);
        return recur;
    }
    static ModelRecurrence triweekly(DayOfWeek dayOfWeek) {
        return triweekly(dayOfWeek, 0);
    }
    static ModelRecurrence triweekly(final DayOfWeek dayOfWeek, final int offset) {
        return triweekly(dayOfWeek, offset, ModelSettings.defaultHour(), ModelSettings.defaultMinute());
    }
    static ModelRecurrence triweekly(final DayOfWeek dayOfWeek, final int offset, final int hour, final int minute) {
        assert offset == 0 || offset == 1 || offset == 3 : "Illegal offset: " + offset + " must be 0, 1 or 2";
        assert hour >= 0 && hour < 27 : "Illegal hour: " + hour + " must be 0-26 (24, 25 and 26 represent 12am, 1am and 2am)";
        assert minute >= 0 && minute < 60 : "Illegal minute: " + minute + " must be 0-59";
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.TRIWEEKLY);
        recur.setValue((offset * 7 + dayOfWeek.ordinal()) * MINUTES_PER_DAY + hour * 60 + minute);
        return recur;
    }
    static ModelRecurrence monthly(final int dayOfWeek) {
        return monthly(dayOfWeek, ModelSettings.defaultHour(), ModelSettings.defaultMinute());
    }
    static ModelRecurrence monthly(final int dayOfWeek, final int hour, final int minute) {
        assert dayOfWeek > 0 && dayOfWeek < 32 : "Illegal day of week: " + dayOfWeek + ", must be 1-31";
        assert hour >= 0 && hour < 27 : "Illegal hour: " + hour + " must be 0-26 (24, 25 and 26 represent 12am, 1am and 2am)";
        assert minute >= 0 && minute < 60 : "Illegal minute: " + minute + " must be 0-59";
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.MONTHLY);
        recur.setValue(dayOfWeek * MINUTES_PER_DAY + hour * 60 + minute);
        return recur;
    }
    static ModelRecurrence yearly(int dayOfYear) {
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.MONTHLY);
        if (dayOfYear > 365) {
            throw new IllegalArgumentException("dayOfYear must be less than 365");
        }
        recur.setValue(dayOfYear);
        return recur;
    }
    static ModelRecurrence dayOfWeek(DayOfWeek dayOfWeek) {
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.DAY_OF_WEEK);
        recur.setValue(dayOfWeek.ordinal());
        return recur;
    }
    static ModelRecurrence once(int hour, int minute) {
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.ONCE);
        //noinspection IntegerMultiplicationImplicitCastToLong
        recur.setValue(hour * 60 + minute);
        return recur;
    }
    static ModelRecurrence once(long birth, int days, int hour, int minute) {
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.ONCE);
        //noinspection IntegerMultiplicationImplicitCastToLong
        recur.setValue(birth + 60_000 * (days * MINUTES_PER_DAY + hour * 60 + minute));
        return recur;
    }
    static ModelRecurrence daily(int hour, int minute) {
        final ModelRecurrence recur = X_Model.create(ModelRecurrence.class);
        recur.setUnit(RecurrenceUnit.DAILY);
        //noinspection IntegerMultiplicationImplicitCastToLong
        recur.setValue(hour * 60 + minute);
        return recur;
    }

}
