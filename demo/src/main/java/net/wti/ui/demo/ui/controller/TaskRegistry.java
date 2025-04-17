package net.wti.ui.demo.ui.controller;

import net.wti.ui.demo.api.ModelTask;
import xapi.fu.In1;

/// TaskRegistry
///
/// Manages the registration and movement of tasks between UI views.
/// This abstraction allows the controller to stay decoupled from view state.
///
/// ## Roadmap Checklist
///
/// ### 1. ⚙️ Initialization and Setup
/// 『 ✓ 』 Initialize `TaskRegistry` with callbacks from DemoApp
///
/// ### 2. 🗃 Task Table UI Setup
/// 『 ✓ 』 Route `moveToDone` to DONE task list view
/// 『 ✓ 』 Route `updateAndReschedule` to active task list view
/// 『   』 Support removal of task from active without re-adding
///
/// ### 3. 🪄 Task Interactions
/// 『   』 Hook `cancel` and `defer` logic into controller and registry
///
/// ### 4. 🧠 Recurrence / Scheduling Engine
/// 『 ✓ 』 Provides handlers for moving tasks to the Done list or reinserting in Active
/// 『   』 Future: integrate with `Schedule` abstraction
/// 『   』 Future: undo a task completion
/// 『   』 Future: approve / disapprove
///
/// ### 5. ✅ UX Polish
/// 『   』 Add animation hooks for task transition (ACTIVE → DONE)
/// 『   』 Show success state or undo on move-to-done
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:10:28 CST
public class TaskRegistry {

    private final In1<ModelTask> moveToDoneCallback;
    private final In1<ModelTask> reinsertTodoCallback;

    /// Constructs a registry with the given callbacks for moving and rescheduling tasks.
    public TaskRegistry(In1<ModelTask> moveToDone, In1<ModelTask> reinsertTodo) {
        this.moveToDoneCallback = moveToDone;
        this.reinsertTodoCallback = reinsertTodo;
    }

    /// Moves a finished one-time task to the done list
    public void moveToDone(ModelTask task) {
        if (moveToDoneCallback != null) {
            moveToDoneCallback.in(task);
        }
    }

    /// Updates and reschedules a recurring task back into the active list
    public void updateAndReschedule(ModelTask task) {
        if (reinsertTodoCallback != null) {
            reinsertTodoCallback.in(task);
        }
    }
}
