package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.ModelKey;

/// # ModelTask
///
/// Primary task model for the application. All task metadata, behavior, and
/// persistence fields are defined here.
///
/// This interface is used by the runtime XApi model engine to generate
/// strongly-typed, persistable task instances.
///
/// ### 🧠 Core Fields
/// | Field           | Type        | Purpose                                                       |
/// |------------------|-------------|----------------------------------------------------------------|
/// | `birth`         | `long`      | Timestamp of task creation                                     |
/// | `alarmMinutes`  | `Integer`   | Minutes before deadline when alarms should trigger            |
/// | `deadline`      | `Double`    | Target completion time as a floating timestamp (epoch millis) |
/// | `goal`          | `long`      | Optional goal / estimation value for time tracking            |
/// | `lastFinished`  | `Long`      | When this task was most recently completed                    |
/// | `recurrence`    | `ModelList<ModelRecurrence>` | Active recurrence schedule                       |
///
/// ### 🏷 New State Fields
/// | Field       | Type                | Description                                              |
/// |-------------|---------------------|----------------------------------------------------------|
/// | `paused`    | `boolean`           | True if task is intentionally inactive (user-controlled) |
/// | `archived`  | `boolean`           | True if task is no longer active (historical)            |
/// | `snooze`    | `ModelRecurrence?`  | One-off override of task's recurrence/due time           |
///
/// ### ⛏ Internal Helpers
/// - `recurrence()` ensures recurrence list is always usable
/// - All fields support generated fluent `setX` + `getX` methods
///
/// Created by ChatGPT 4o and James X. Nelson on 2025-04-17 @ 04:20 CST

@SuppressWarnings("UnusedReturnValue")
@IsModel(
        modelType = ModelTask.MODEL_TASK,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ModelTask extends BasicModelTask<ModelTask> {

    String MODEL_TASK = "tsk";

    /// Soft deadline target (stored as Double epoch millis for consistency)
    Double getDeadline();
    ModelTask setDeadline(Double deadline);

    /// When the task was most recently completed
    Long getLastFinished();
    ModelTask setLastFinished(Long lastFinished);

    /// True if task is paused by the user
    boolean isPaused();
    ModelTask setPaused(boolean paused);

    /// True if task is archived (excluded from normal views)
    boolean isArchived();
    ModelTask setArchived(boolean archived);

    /// Optional snoozed timeout -- hides this task until the snooze timeout is reached
    Double getSnooze();
    ModelTask setSnooze(Double snooze);

    /// A key to the instance of [ModelTaskDescription] used to create this ModelTask
    ModelKey getTaskSource();
    ModelTask setTaskSource(ModelKey taskSource);

    @Override
    default ModelTask self() {
        return this;
    }
}