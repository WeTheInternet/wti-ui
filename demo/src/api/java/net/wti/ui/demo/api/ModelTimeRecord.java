package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.Model;

/// ModelTimeRecord:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 23/04/2025 @ 02:54
@IsModel(
        modelType = ModelTimeRecord.MODEL_TIME_RECORD,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ModelTimeRecord extends Model {

    String MODEL_TIME_RECORD = "tr";

    double getSeconds();
    ModelTimeRecord setSeconds(double seconds);

    String getNote();
    ModelTimeRecord setNote(String note);
}
