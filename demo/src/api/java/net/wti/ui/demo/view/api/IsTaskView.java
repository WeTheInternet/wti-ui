package net.wti.ui.demo.view.api;

import com.badlogic.gdx.scenes.scene2d.Stage;
import net.wti.ui.components.IsSkinnable;
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
public interface IsTaskView<Model extends BasicModelTask<Model>> extends net.wti.ui.api.IsExpandable, IsSkinnable {

    /// @return The underlying task model being rendered
    Model getTask();

    /// Trigger a full refresh / rerender of the visual layout
    void rerender();

    /// Supply the Stage, so we don't have to pass it around in methods
    Stage getStage();
}

