package net.wti.ui.demo.ui.controller;

import net.wti.ui.demo.api.*;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.util.api.SuccessHandler;

/// TaskController
///
/// Acts as a mediator between UI components and the persistence model layer.
/// This class encapsulates task actions and lifecycle events: save, finish, defer, cancel, etc.
///
/// ## Roadmap Checklist
///
/// ### 1. ⚙️ Model Integration
/// 『 ✓ 』 Persist tasks using `X_Model`
/// 『   』 Hook recurrence editing logic
///
/// ### 2. 🚀 Task Lifecycle Actions
/// 『 ✓ 』 Mark task as done → create `ModelTaskCompletion`
/// 『 ✓ 』 Reschedule recurring tasks instead of removing them
/// 『   』 Implement `cancel()` logic
/// 『   』 Implement `defer()` logic
///
/// ### 3. 🧰 UX / UI Hooks
/// 『 ✓ 』 Use `TaskRegistry` to route updates to views
/// 『   』 Undo functionality for recent completions
/// 『   』 Notify when long-running persist operation finishes
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:53:00 CST
public class TaskController {

    private final TaskRegistry registry;

    public TaskController(TaskRegistry registry) {
        this.registry = registry;
    }

    /// Persists the updated state of a task
    public void save(ModelTask task) {
        X_Model.persist(task, SuccessHandler.NO_OP);
    }
    public void save(ModelTaskCompletion taskCompletion) {
        X_Model.persist(taskCompletion, SuccessHandler.NO_OP);
    }

    /// Marks a task as completed and moves or reschedules it
    public void markAsDone(ModelTask task) {
        final ModelTaskCompletion done = X_Model.create(ModelTaskCompletion.class);
        done.setName(task.getName());
        done.setDescription(task.getDescription());
        done.setCompleted(System.currentTimeMillis());
        done.setSourceKey(task.getKey().toString());
        done.setStatus(CompletionStatus.COMPLETED);

        X_Model.persist(done, result -> {
            task.setLastFinished(System.currentTimeMillis());

            boolean isOnce = true;
            for (final ModelRecurrence recur : task.getRecurrence()) {
                if (recur.getUnit() != RecurrenceUnit.ONCE) {
                    isOnce = false;
                    break;
                }
            }

            if (isOnce) {
                registry.moveToDone(task);
            } else {
                registry.updateAndReschedule(task);
            }
        });
    }

    /// Cancels a task (placeholder)
    public void cancel(ModelTask task) {
        System.out.println("Cancelling task: " + task.getName());
    }

    /// Defers a task (placeholder)
    public void defer(ModelTask task) {
        System.out.println("Deferring task: " + task.getName());
    }

    /// Reloads a task from persistent storage by key
    public void reload(ModelKey key, SuccessHandler<ModelTask> callback) {
        X_Model.load(ModelTask.class, key, callback);
    }
}