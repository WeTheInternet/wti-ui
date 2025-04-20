package net.wti.ui.demo.ui.controller;

import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import net.wti.ui.demo.api.*;
import net.wti.ui.demo.ui.dialog.TaskEditDialog;
import net.wti.ui.demo.view.api.IsTaskView;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelList;
import xapi.util.api.SuccessHandler;

/// TaskController
///
/// Acts as a mediator between UI components and the persistence model layer.
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
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:53:00 CST
public class TaskController {

    public enum CancelMode {FOREVER, NEXT, SNOOZE}

    private final TaskRegistry registry;

    public TaskController(TaskRegistry registry) {
        this.registry = registry;
    }

    /// Persists the updated state of a task
    public void save(ModelTask task) {
        X_Model.persist(task, SuccessHandler.noop());
    }

    public void save(ModelTaskCompletion taskCompletion) {
        X_Model.persist(taskCompletion, SuccessHandler.noop());
    }

    /// Marks a task as completed and moves or reschedules it
    public void markAsDone(ModelTask task) {
        final ModelTaskCompletion done = X_Model.create(ModelTaskCompletion.class);
        done.setName(task.getName());
        done.setDescription(task.getDescription());
        done.setCompleted(System.currentTimeMillis());
        done.setSourceTask(task.getKey());
        done.setStatus(CompletionStatus.COMPLETED);

        X_Model.persist(done, result -> {
            task.setLastFinished(System.currentTimeMillis());

            boolean isOnce = true;
            final ModelList<ModelRecurrence> recurrence = task.getRecurrence();
            if (recurrence != null) {
                for (final ModelRecurrence recur : recurrence) {
                    if (recur.getUnit() != RecurrenceUnit.ONCE) {
                        isOnce = false;
                        break;
                    }
                }
            }

            if (isOnce) {
                registry.moveToDone(task);
            } else {
                registry.updateAndReschedule(task);
            }
        });
    }

    /* -------------------------------------------------- */

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
        save(entry);

        registry.moveToDone(task);
    }

    private void cancelNext(ModelTask task) {
        registry.updateAndReschedule(task); // already computes next recurrence
    }

    /// Defers a task (placeholder)
    public void defer(ModelTask task) {
        System.out.println("Deferring task: " + task.getName());
    }

    /// Reloads a task from persistent storage by key
    public void reload(ModelKey key, SuccessHandler<ModelTask> callback) {
        X_Model.load(ModelTask.class, key, callback);
    }

    /// -----------------------------------------------------------------
    ///  Opens a task‑edit dialog
    /// -----------------------------------------------------------------
    public void edit(IsTaskView<ModelTask> view) {
        final ModelTask mod = view.getTask();
        System.out.println("Editing task: " + mod.getName());
        TaskEditDialog dialog = new TaskEditDialog(view.getStage(), view.getSkin(), mod, this) {
            @Override
            protected void result(final Object obj) {
                if (Boolean.TRUE == obj) {
                    // the model is saved!
                    view.rerender();
                }
                super.result(obj);
            }
        };
        dialog.show(view.getStage(), Actions.fadeIn(0.3f));
    }
}