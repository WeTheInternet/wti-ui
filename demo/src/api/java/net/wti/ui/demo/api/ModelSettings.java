package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.X_Model;
import xapi.model.api.Model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

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

    static ZoneOffset timeZone() {
        String zone = INSTANCE.getTimeZone();
        if (zone == null) {
            final ZoneId defaultZone = ZoneId.systemDefault();
            INSTANCE.setTimeZone(defaultZone.getId());
            return defaultZone.getRules().getOffset(Instant.now());
        }
        ZoneId zoneId = ZoneId.of(zone);
        return zoneId.getRules().getOffset(Instant.now());
    }

    String getTimeZone();
    ModelSettings setTimeZone(String timeZone);

    Integer getDefaultHour();
    ModelSettings setDefaultHour(Integer hour);

    Integer getDefaultMinute();
    ModelSettings setDefaultMinute(Integer minute);

}
