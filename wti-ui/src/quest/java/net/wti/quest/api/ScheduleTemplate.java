package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;

/// ScheduleTemplate
///
/// Work/off/holiday policy that sets skip behavior.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:54
@IsModel(
        modelType = ScheduleTemplate.MODEL_SCHEDULE_TEMPLATE,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ScheduleTemplate extends Model {

    String MODEL_SCHEDULE_TEMPLATE = "tmpl";

    KeyBuilder KEY_BUILDER_TEMPLATE =
            KeyBuilder.build(MODEL_SCHEDULE_TEMPLATE).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(String id) {
        return KEY_BUILDER_TEMPLATE.buildKey(id);
    }

    String getName();
    ScheduleTemplate setName(String name);

    /// Weekdays considered "on" (0–6 or 1–7; choose consistent convention with your time APIs).
    Integer[] getWeekdaysOn();
    ScheduleTemplate setWeekdaysOn(Integer[] weekdays);

    /// Optional named holiday sets (to be resolved elsewhere).
    String[] getHolidaySetKeys();
    ScheduleTemplate setHolidaySetKeys(String[] keys);

    OffDaySkipBehavior getOffDaySkipBehavior();
    ScheduleTemplate setOffDaySkipBehavior(OffDaySkipBehavior behavior);
}

