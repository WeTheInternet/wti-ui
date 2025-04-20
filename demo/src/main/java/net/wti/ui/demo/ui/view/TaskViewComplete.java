package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.view.DeadlineView;


/// TaskViewCompleted
///
/// Read‑only view for a `ModelTaskCompletion` entry rendered in the **Done**
/// tab.  Supports a compact line and an optional expanded panel of historical
/// stats.
///
/// ### 『 Status Checklist 』
///
/// ## 📦 Compact View
/// - 『 ✓ 』 Render task name + description
/// - 『 ✓ 』 Show completion timestamp
///
/// ## 🔄 Expandable Layout
/// - 『 ✓ 』 Toggle to reveal “stats” panel
/// - 『 ○ 』 Compute real stats (times late, total lateness, etc.)
///
/// ## ✨ UX Polish
/// - 『 ○ 』 Animated expand/collapse  *(UI‑7)*
/// - 『 ○ 』 Approval badge & tool‑tip
/// - 『 ○ 』 Keyboard accessibility
///
/// Created 2025‑04‑18 by ChatGPT‑4o & JXN
public final class TaskViewComplete
        extends AbstractTaskView<ModelTaskCompletion> {

    public TaskViewComplete(ModelTaskCompletion cmp,
                             Skin skin,
                             TaskController ctl) {
        super(cmp,
                skin.has("taskview", TaskViewStyle.class)
                        ? skin.get("taskview", TaskViewStyle.class)
                        : new TaskViewStyle(),
                skin, ctl);
        rebuild();
    }

    // ------------------------------------------------------------------ //
    // AbstractTaskView hooks                                             //
    // ------------------------------------------------------------------ //

    /// Completed tasks no longer track deadlines.
    @Override protected DeadlineView createDeadlineView(ModelTaskCompletion m) { return null; }

    /// Collapsed row: simple summary.
    @Override protected void buildCollapsed() {
        add(label(model.getName(), style.nameStyle)).left().row();
        if (notEmpty(model.getDescription())) {
            add(label(model.getDescription(), style.previewStyle)).left().row();
        }
        add(label("Completed @ " + model.getCompleted(), style.previewStyle)).left();
    }

    /// Expanded row: compact + rudimentary stats.
    @Override protected void buildExpanded() {
        buildCollapsed();
        row();
        add(label("Completion stats:", style.recurrenceLabelStyle)).left().row();
        add(label("　　・Times completed in a row: (todo)", style.recurrenceValueStyle)).left().row();
        add(label("　　・Times completed late: (todo)", style.recurrenceValueStyle)).left().row();
        add(label("　　・Total lateness: (todo)", style.recurrenceValueStyle)).left().row();
        add(label("　　・Last completed: " + model.getCompleted(), style.recurrenceValueStyle)).left().row();
        add(label("　　・Status: " + model.getStatus(), style.recurrenceValueStyle)).left().row();
    }

    // ------------------------------------------------------------------ //
    // Helper utilities                                                   //
    // ------------------------------------------------------------------ //
    private Label label(String txt, Label.LabelStyle ls) {
        return new Label(txt, ls != null ? ls : skin.get(Label.LabelStyle.class));
    }
    private boolean notEmpty(String s) { return s != null && !s.isEmpty(); }

}
