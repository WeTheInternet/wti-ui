package net.wti.ui.demo.api;

import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.model.api.ModelList;

/// RealizedTaskModel:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 23/04/2025 @ 02:58
public interface RealizedModelTask<Self extends RealizedModelTask<Self>> extends BasicModelTask<Self> {

    In1Out1<RealizedModelTask<?>, ModelList<ModelTimeRecord>> TIME_RECORD_GETTER = RealizedModelTask::getTimeRecord;
    In2<RealizedModelTask<?>, ModelList<ModelTimeRecord>> TIME_RECORD_SETTER = RealizedModelTask::setTimeRecord;

    /// Recurrence schedule
    ModelList<ModelTimeRecord> getTimeRecord();
    @SuppressWarnings("UnusedReturnValue")
    ModelTask setTimeRecord(ModelList<ModelTimeRecord> timeRecord);

    /// Ensures that a recurrence list is available, initializing if null.
    default ModelList<ModelTimeRecord> timeRecord() {
        return getOrCreateModelList(
                ModelTimeRecord.class,
                TIME_RECORD_GETTER.supply(this),
                TIME_RECORD_SETTER.provide1(this)
        );
    }

}
