package net.wti.ui.demo.ui.controller;

import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import net.wti.tasks.index.TaskIndex;
import net.wti.ui.demo.api.CompletionStatus;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.api.ModelTaskDescription;
import net.wti.ui.demo.ui.dialog.TaskEditDialog;
import net.wti.ui.demo.view.api.IsTaskView;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.util.api.SuccessHandler;

/// TaskController
///
/// Mediates between UI and the persistence/model layer, **and** keeps a
/// centralized `TaskIndex` in sync by emitting events after every state change.
/// This class encapsulates task actions and lifecycle events: save, finish, defer, cancel, etc.
///
/// ### Roadmap Checklist
/// - 『 ✓ 』 CTL‑1 Implement cancel()
/// - 『 ○ 』 CTL‑2 Implement defer()
/// - 『 ○ 』 CTL‑3 Hook recurrence editing logic
/// - 『 ○ 』 CTL‑4 Undo for recent completions
/// - 『 ○ 』 CTL‑5 Persist‑finished notification hooks
/// - 『 ○ 』 CTL‑6 Snooze logic persistence
///
/// ### Event emission policy
/// - Create:      `onTaskCreated(task)`
/// - Start:       `onTaskStarted(task)`
/// - Finish:      `onTaskFinished(task)`
/// - Cancel:      `onTaskCancelled(task)`
/// - Update/edit: `onTaskUpdated(task)`
/// - Delete:      `onTaskDeleted(key)`
///
/// Notes:
/// - For recurring tasks, after we compute the next instance, we also emit
///   `onTaskUpdated(task)` so live views can refresh row contents.
/// - All `TaskIndex` notifications are posted on the GL thread (by TaskIndex),
///   so it’s UI-safe to subscribe from Scene2D widgets.
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:53:00 CST
public class TaskController {

    public enum CancelMode { FOREVER, NEXT, SNOOZE }

    private final TaskRegistry registry;
    private final TaskIndex taskIndex;

    /// Inject both registry (persistence ops) and task index (event/cache).
    public TaskController(TaskRegistry registry, TaskIndex taskIndex) {
        this.registry = registry;
        this.taskIndex = taskIndex;
    }

    /// Persists a new or existing task, then notifies the index.
    /// If you need to distinguish "created" vs "updated", create a separate
    /// factory path that calls `taskIndex.onTaskCreated(...)`.
    public void save(ModelTask task) {
        X_Model.persist(task, result -> {
            // You may want to detect "new" vs "existing" here (e.g., null id prior to persist)
            taskIndex.onTaskUpdated(task);
        });
    }

    public void save(ModelTaskCompletion taskCompletion) {
        X_Model.persist(taskCompletion, SuccessHandler.noop());
    }

    /// Marks a task as completed. Emits `Finished`, then either updates or moves to "done".
    public void markAsDone(ModelTask task) {
        final ModelTaskCompletion done = X_Model.create(ModelTaskCompletion.class);
        done.setName(task.getName());
        done.setDescription(task.getDescription());
        done.setCompleted(System.currentTimeMillis());
        done.setSourceTask(task.getKey());
        done.setStatus(CompletionStatus.COMPLETED);

        X_Model.persist(done, result -> {
            task.setLastFinished(System.currentTimeMillis());

            // Fire "finished" first so active lists drop the row quickly.
            taskIndex.onTaskFinished(task);

            if (task.hasRecurrence()) {
                // Compute next recurrence; this mutates the same task record.
                registry.updateAndReschedule(task);
                // Let views know this task's scheduling changed.
                taskIndex.onTaskUpdated(task);
            } else {
                // Non-recurring task: move to historical bucket.
                registry.moveToDone(task);
                // Optionally also emit onTaskUpdated if your UI expects it.
            }
        });
    }

    /// Cancel entry point – routes to forever/next/snooze behaviors.
    public void cancel(ModelTask task, CancelMode mode, double snoozeUntil) {
        switch (mode) {
            case FOREVER:
                cancelForever(task);
                break;
            case NEXT:
                cancelNext(task);
                break;
            case SNOOZE:
                registry.snooze(task, snoozeUntil);
                taskIndex.onTaskUpdated(task);
                break;
        }
    }

    private void cancelForever(ModelTask task) {
        ModelTaskCompletion entry = X_Model.create(ModelTaskCompletion.class);
        entry.setName(task.getName());
        entry.setDescription(task.getDescription());
        entry.setCompleted(System.currentTimeMillis());
        entry.setSourceTask(task.getKey());
        entry.setStatus(CompletionStatus.CANCELLED);

        // Persist the completion/cancellation record, move task, and notify.
        X_Model.persist(entry, result -> {
            registry.moveToDone(task);
            taskIndex.onTaskCancelled(task);
        });
    }

    private void cancelNext(ModelTask task) {
        // Compute and persist the "skip next" / next occurrence.
        registry.updateAndReschedule(task);
        taskIndex.onTaskUpdated(task);
    }

    /// Defers a task (stub – wire your actual snooze/reschedule policy here).
    public void defer(ModelTask task) {
        // If you decide to write to a snooze field and persist:
        // registry.snooze(task, untilEpochMillis);
        // X_Model.persist(task, ...);
        taskIndex.onTaskUpdated(task);
    }

    /// Reloads a task from persistent storage; caller decides what to do next.
    public void reload(ModelKey key, SuccessHandler<ModelTask> callback) {
        X_Model.load(ModelTask.class, key, callback);
    }

    /// Permanently delete a task and notify the index.
    public void deleteTask(final ModelTask task) {
        final ModelKey key = task.getKey();
        X_Model.delete(key, r -> {
            // Use the ModelKey overload to actually remove from the index map.
            taskIndex.onTaskDeleted(key);
        });
    }

    public void deleteTaskDescription(final ModelTaskDescription task) {
        X_Model.delete(task.getKey(), SuccessHandler.noop());
        // If descriptions are surfaced as tasks, emit a delete/update as appropriate.
    }

    /// Opens the task-edit dialog; after a positive result, emits an update event.
    public void edit(IsTaskView<ModelTask> view) {
        final ModelTask mod = view.getTask();
        TaskEditDialog dialog = new TaskEditDialog(view.getStage(), view.getSkin(), mod, this) {
            @Override
            protected void result(final Object obj) {
                if (Boolean.TRUE == obj) {
                    // the model is saved! ensure UI + index are updated
                    view.rerender();
                    taskIndex.onTaskUpdated(mod);
                }
                super.result(obj);
            }
        };
        dialog.show(view.getStage(), Actions.fadeIn(0.3f));
    }

    public TaskIndex getIndex() {
        return taskIndex;
    }
}