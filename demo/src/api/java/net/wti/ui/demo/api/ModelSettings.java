package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.time.X_Time;
import xapi.time.api.TimeZoneInfo;

/// ModelSettings:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 22:30
@IsModel(
        modelType = ModelSettings.MODEL_SETTINGS
        ,persistence = @Persistent(strategy= PersistenceStrategy.Remote)
)
public interface ModelSettings extends Model {

    ModelSettings INSTANCE = X_Model.create(ModelSettings.class);

    String MODEL_SETTINGS = "stgs";

    static int defaultHour() {
        Integer defaultHour = INSTANCE.getDefaultHour();
        if (defaultHour == null) {
            INSTANCE.setDefaultHour(defaultHour = 20); // 8 pm
        }
        return defaultHour;
    }

    static int defaultMinute() {
        Integer defaultMinute = INSTANCE.getDefaultMinute();
        if (defaultMinute == null) {
            INSTANCE.setDefaultMinute(defaultMinute = 0);
        }
        return defaultMinute;
    }

    static TimeZoneInfo timeZone() {
        TimeZoneInfo zone = INSTANCE.getTimeZone();
        if (zone == null) {
            zone = X_Time.systemZone();
            INSTANCE.setTimeZone(zone);
        }
        return zone;
    }

    TimeZoneInfo getTimeZone();
    ModelSettings setTimeZone(TimeZoneInfo timeZone);

    Integer getDefaultHour();
    ModelSettings setDefaultHour(Integer hour);

    Integer getDefaultMinute();
    ModelSettings setDefaultMinute(Integer minute);

}
