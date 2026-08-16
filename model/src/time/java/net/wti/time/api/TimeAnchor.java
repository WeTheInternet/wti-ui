package net.wti.time.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.Model;

/// TimeAnchor
///
/// Encodes a position within a day/week/month/year window,
/// used to compute absolute deadlines inside a ModelDay.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:39
@IsModel(
        modelType = TimeAnchor.MODEL_TIME_ANCHOR,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface TimeAnchor extends Model {

    String MODEL_TIME_ANCHOR = "tanc";

    TimeAnchorKind getKind();
    TimeAnchor setKind(TimeAnchorKind kind);

    /// 0–23
    Integer getHour();
    TimeAnchor setHour(Integer hour);

    /// 0–59
    Integer getMinute();
    TimeAnchor setMinute(Integer minute);

    /// For WEEKLY: dayOfWeek (0=Sunday .. 6=Saturday) or your chosen convention.
    Integer getDayOfWeek();
    TimeAnchor setDayOfWeek(Integer dayOfWeek);

    /// For MONTHLY: 1–31
    Integer getDayOfMonth();
    TimeAnchor setDayOfMonth(Integer dayOfMonth);

    /// For YEARLY: 1–366
    Integer getDayOfYear();
    TimeAnchor setDayOfYear(Integer dayOfYear);

}
