package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.model.api.ModelList;

@IsModel(
        modelType = ModelTask.MODEL_TASK
        ,persistence = @Persistent(strategy= PersistenceStrategy.Remote)
)
public interface ModelTask extends BasicModelTask<ModelTask> {
    String MODEL_TASK = "tsk";
    In1Out1<ModelTask, ModelList<ModelRecurrence>> RECURRENCE_GETTER = ModelTask::getRecurrence;
    In2<ModelTask, ModelList<ModelRecurrence>> RECURRENCE_SETTER = ModelTask::setRecurrence;

    long getBirth();
    ModelTask setBirth(long birth);

    Integer getAlarmMinutes();
    ModelTask setAlarmMinutes(Integer alarmMinutes);

    Double getDeadline();
    ModelTask setDeadline(Double deadline);

    long getGoal();
    ModelTask setGoal(long goal);

    Long getLastFinished();
    ModelTask setLastFinished(Long lastFinished);

    ModelList<ModelRecurrence> getRecurrence();
    void setRecurrence(ModelList<ModelRecurrence> recurrence);
    default ModelList<ModelRecurrence> recurrence() {
        return getOrCreateModelList(ModelRecurrence.class, RECURRENCE_GETTER.supply(this), RECURRENCE_SETTER.provide1(this));
    }

}