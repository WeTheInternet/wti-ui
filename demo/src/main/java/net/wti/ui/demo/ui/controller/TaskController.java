package net.wti.ui.demo.ui;

import net.wti.ui.demo.api.ModelRecurrence;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.api.RecurrenceUnit;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.util.api.SuccessHandler;

///
/// TaskController:
///
/// Interface between the UI and the persistence/model layer.
/// All scheduling, recurrence, and list reflowing logic can hook in here.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/04/2025 @ 20:28
public class TaskController {

    public void save(ModelTask task) {
        // Stubbed: will later update task schedule and re-sort in UI
        X_Model.persist(task, SuccessHandler.NO_OP);
    }

    public void markAsDone(ModelTask task) {
        final ModelTaskCompletion done = X_Model.create(ModelTaskCompletion.class);
        done.setName(task.getName());
        done.setDescription(task.getDescription());
        done.setCompleted(System.currentTimeMillis());
        done.setSourceKey(task.getKey().toString());
        done.setStatus(net.wti.ui.demo.api.ModelTaskCompletion.CompletionStatus.COMPLETED);

        X_Model.persist(done, result -> {
            System.out.println("Task marked as done: " + result.getName());
            task.setLastFinished(System.currentTimeMillis());

            // Determine if this task recurs or is a one-off
            boolean isOnce = true;
            for (final ModelRecurrence recur : task.getRecurrence()) {
                if (recur.getUnit() != RecurrenceUnit.ONCE) {
                    isOnce = false;
                    break;
                }
            }

            if (isOnce) {
                // Remove from UI TODO list, add to DONE list
                TaskRegistry.moveToDone(task);
            } else {
                // Compute new deadline and reinsert into TODO list
                TaskRegistry.updateAndReschedule(task);
            }
        });
    }

    public void cancel(ModelTask task) {
        // Stubbed: handle cancellation logic (e.g., delete or flag)
        System.out.println("Cancelling task: " + task.getName());
    }

    public void defer(ModelTask task) {
        // Stubbed: modify task deadline or recurrence to reschedule
        System.out.println("Deferring task: " + task.getName());
    }

    public void reload(ModelKey key, SuccessHandler<ModelTask> callback) {
        X_Model.load(ModelTask.class, key, callback);
    }
}
