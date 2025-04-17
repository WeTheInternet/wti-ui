package net.wti.ui.demo.view.api;

import net.wti.ui.api.IsDeadlineView;
import net.wti.ui.demo.api.BasicModelTask;

/// IsTaskView
///
/// Abstraction for components that render a task and provide task-specific logic.
/// This interface is implemented by task views such as `TaskViewExpandable`, allowing
/// external UI lists to manipulate or inspect task components in a uniform way.
///
/// ## Roadmap Checklist
///
/// ### 1. 🧩 View Representation
/// 『 ✓ 』 Return associated task model
/// 『 ✓ 』 Expose deadline-aware view (for visual highlighting, refresh, etc.)
/// 『   』 Track expanded/collapsed state
///
/// ### 2. 🔄 Interactive Behavior
/// 『   』 Add methods for expand(), collapse(), rerender()
/// 『   』 Optional: keyboard/focus support
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:52:00 CST
public interface IsTaskView<Model extends BasicModelTask<Model>> {

    /// @return The underlying task model being rendered
    Model getTask();

    /// @return The deadline-aware view used to show time until due
    IsDeadlineView<?> getDeadlineView();

    /// Expand this task view (e.g. show recurrence, notes, etc.)
    void expand();

    /// Collapse this task view (e.g. hide extra UI)
    void collapse();

    /// Trigger a full refresh / rerender of the visual layout
    void rerender();

}

