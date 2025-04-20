package net.wti.ui.demo.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.ui.view.AbstractTaskView;
import net.wti.ui.demo.view.api.IsTaskView;
import xapi.time.X_Time;

import static xapi.time.X_Time.ONE_DAY;

/// TaskActionBar
///
/// A compact row of symbolic buttons that operate on a single `ModelTask`.
/// All glyphs come from **Noto Sans Symbols 2** and are assumed to exist in
/// the pre‑baked bitmap font.
///
/// Current button set:
/// * **Finish**   ✓   — `TaskController.markAsDone()`
/// * **Defer**    ⌚   — `TaskController.defer()`
/// * **Cancel**   ✕   — `TaskController.cancel()`
/// * **Edit**     🛠  — `TaskController.edit()`
/// * **Expand**   ▶ / ▼ — toggles parent view compact ↔ expanded
///
/// ### Checklist
/// * 『 ✓ 』 Fully wired click actions
/// * 『 ✓ 』 Toggle glyph updates live
/// * 『 ○ 』 Accessibility descriptors for screen readers
/// * 『 ○ 』 Long‑press tool‑tips
/// * 『 ○ 』 Theming override per‑button (future)
///
public final class TaskActionBar extends Table {

    // ---------------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------------

//    private static final String GLYPH_INFINITY = "\uD800\uDD85";
    public static final String GLYPH_INFINITY = "∞";
    private static final String GLYPH_FINISH   = "✓";
    private static final String GLYPH_DEFER    = "⌚";
    private static final String GLYPH_CANCEL   = "✕";
    private static final String GLYPH_EDIT     = "🛠";
    private static final String GLYPH_EXPAND   = "⯯"; // down arrows
    private static final String GLYPH_COLLAPSE = "⯭"; // up arrows

    // ---------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------

    /// Reference to the parent TaskView (or any IsTaskView implementation).
    private final IsTaskView<ModelTask> parentView;

    /// Cached reference so we can flip text instead of searching the scene graph.
    private TextButton toggleButton;

    // ---------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------

    /// Creates an action bar wired to a single task.
    ///
    /// @param view        the task view that owns this bar; used for expansion
    ///                    toggling and for retrieving task & skin.
    /// @param controller  mediator that performs persistence‑layer actions.
    /// @param style       visual style bundle coming from `task‑ui.json`.
    public TaskActionBar(
            final IsTaskView<ModelTask> view,
            final TaskController controller,
            final AbstractTaskView.TaskViewStyle style
    ) {
        super(view.getSkin());
        this.parentView = view;

        top().left().padTop(4).defaults().space(6);

        if (style.background != null) {
            setBackground(style.background);
        }

        /* build individual buttons */
        add(buildButton(GLYPH_FINISH,
                () -> controller.markAsDone(view.getTask()),
                style)).width(28).height(28);
        add(buildButton(GLYPH_DEFER,
                () -> controller.defer(view.getTask()),
                style)).width(28).height(28);
        add(buildButton(GLYPH_CANCEL,
                () -> {
                    controller.cancel(view.getTask(), TaskController.CancelMode.NEXT,
                            X_Time.nowMillis() + ONE_DAY);
                },
                style)).width(28).height(28);
        add(buildButton(GLYPH_EDIT,
                () -> controller.edit(view),
                style)).width(28).height(28);

        /* expand / collapse button (initial state follows view) */
        toggleButton = buildButton(
                view.isExpanded() ? GLYPH_COLLAPSE : GLYPH_EXPAND,
                this::toggleExpand,
                style
        );
        toggleButton.setName("task-btn-toggle");
        add(toggleButton).width(28).height(28);
    }

    // ---------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------

    /// Factory for a styled `TextButton`.
    ///
    /// @param glyph   unicode character to display.
    /// @param action  runnable executed on click.
    /// @param style   common TaskView style containing the `buttonStyle`.
    /// @return a fully wired TextButton ready to be inserted into the table.
    private TextButton buildButton(
            final String glyph,
            final Runnable action,
            final AbstractTaskView.TaskViewStyle style
    ) {
        TextButton btn = new TextButton(glyph, style.buttonStyle);
        btn.align(Align.center);
        btn.addListener(new ClickListener() {
            @Override public void clicked(
                    final InputEvent event,
                    final float x,
                    final float y
            ) {
                action.run();
            }
        });
        return btn;
    }

    /// Toggles the parent view’s expanded state and flips the glyph.
    private void toggleExpand() {
        parentView.toggleExpanded();
        toggleButton.setText(
                parentView.isExpanded() ? GLYPH_COLLAPSE : GLYPH_EXPAND
        );
    }
}