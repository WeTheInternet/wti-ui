package net.wti.time.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.fu.api.Ignore;
import xapi.model.X_Model;
import xapi.model.api.Model;

///
/// ModelDuration:
///
/// amount + unit, used by RecurrenceRule cadence.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:38
@IsModel(
        modelType = ModelDuration.MODEL_DURATION,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ModelDuration extends Model {

    String MODEL_DURATION = "dur";

    Integer getAmount();
    ModelDuration setAmount(Integer amount);

    DurationUnit getUnit();
    ModelDuration setUnit(DurationUnit unit);

    @Ignore("model")
    default long toMillis() {
        final Integer amt = getAmount();
        if (amt == null) {
            return 0;
        }
        return getUnit().toMillis(amt);
    }
    static ModelDuration duration(int amt, DurationUnit unit) {
        if (unit == null) {
            throw new NullPointerException("No unit specified for duration amount " + amt);
        }
        final ModelDuration duration = X_Model.create(ModelDuration.class);
        duration.setAmount(amt);
        duration.setUnit(unit);
        // TODO: cache these somewhere
        return duration;
    }
}
