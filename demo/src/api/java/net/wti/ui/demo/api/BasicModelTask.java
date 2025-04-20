package net.wti.ui.demo.api;

import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.api.Ignore;
import xapi.model.api.Model;
import xapi.model.api.ModelList;

/// BasicModelTask:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/04/2025 @ 23:09
@SuppressWarnings("UnusedReturnValue")
public interface BasicModelTask<Self extends BasicModelTask<Self>> extends Model {

    In1Out1<BasicModelTask<?>, ModelList<ModelRecurrence>> RECURRENCE_GETTER = BasicModelTask::getRecurrence;
    In2<BasicModelTask<?>, ModelList<ModelRecurrence>> RECURRENCE_SETTER = BasicModelTask::setRecurrence;

    /// The name of the task (concise, but complete)
    String getName();
    Self setName(String name);

    /// The description of the task (any additional instructions)
    String getDescription();
    Self setDescription(String description);

    /// The priority (higher enum .ordinal() == higher priority)
    TaskPriority getPriority();
    Self setPriority(TaskPriority priority);

    /// Timestamp when the task was first created
    long getBirth();
    Self setBirth(long birth);

    /// Timestamp when the task was last updated
    long getUpdated();
    Self setUpdated(long updated);

    /// Number of minutes before deadline to consider "alarm-worthy"
    Integer getAlarmMinutes();
    Self setAlarmMinutes(Integer alarmMinutes);

    /// Recurrence schedule
    ModelList<ModelRecurrence> getRecurrence();
    Self setRecurrence(ModelList<ModelRecurrence> recurrence);

    /// Ensures that a recurrence list is available, initializing if null.
    default ModelList<ModelRecurrence> recurrence() {
        return getOrCreateModelList(
                ModelRecurrence.class,
                RECURRENCE_GETTER.supply(this),
                RECURRENCE_SETTER.provide1(this)
        );
    }

    /// Copies *shared* fields from any BasicModelTask to another.
    static <F extends BasicModelTask<F>, T extends BasicModelTask<T>>
    void copyModel(F from, T to) {
        to.setName(from.getName())
                .setDescription(from.getDescription())
                .setPriority(from.getPriority())
                .setAlarmMinutes(from.getAlarmMinutes())
                .setBirth(from.getBirth());
        final ModelList<ModelRecurrence> recurs = to.recurrence();
        recurs.clear();
        recurs.absorb(from.recurrence());
    }

    @Ignore("model")
    Self self();

    /// convenience default
    default <T extends BasicModelTask<T>> T copyTo(T target) {
        copyModel(self(), target);
        return target;
    }
}
