package net.wti.ui.demo.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.common.DemoConstants;
import net.wti.ui.demo.i18n.Messages;
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
    private static final String GLYPH_EXPAND   = "+"; // down arrows
    private static final String GLYPH_COLLAPSE = "-"; // up arrows
//    private static final String GLYPH_EXPAND   = "⯯"; // down arrows
//    private static final String GLYPH_COLLAPSE = "⯭"; // up arrows

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
        final Skin skin = view.getSkin();
        this.parentView = view;
        pad(0);
        defaults().space(4, 4, 4, 4).pad(0);

        if (style.background != null) {
            setBackground(style.background);
        }
        /* build individual buttons */
        final int size = 28;
        Messages msgs = DemoConstants.MESSAGES;
        // edit button: to make changes to this task
        add(buildButton(GLYPH_EDIT,
                msgs.buttonEdit(),
                () -> controller.edit(view),
                style, skin)).width(size).height(size).pad(0);
        // for active tasks that support deferral, render a snooze button
        add(buildButton(GLYPH_DEFER,
                msgs.buttonReschedule(),
                () -> controller.defer(view.getTask()),
                style, skin)).width(size).height(size).pad(0);

        // for tasks that can be cancelled, put in a button to cancel
        add(buildButton(GLYPH_CANCEL,
                msgs.buttonCancel(),
                () -> {
                    controller.cancel(view.getTask(), TaskController.CancelMode.NEXT,
                            X_Time.nowMillis() + ONE_DAY);
                },
                style, skin)).width(size).height(size).pad(0);

        // put in a button to complete the task.
        add(buildButton(GLYPH_FINISH,
                msgs.buttonFinish(),
                () -> controller.markAsDone(view.getTask()),
                style, skin)).width(size).height(size).pad(0);

        /* expand / collapse button (initial state follows view) */
        toggleButton = buildButton(
                view.isExpanded() ? GLYPH_COLLAPSE : GLYPH_EXPAND,
                view.isExpanded() ? msgs.buttonMinimize() : msgs.buttonMaximize(),
                this::toggleExpand,
                style, skin
        );
        toggleButton.setName("task-btn-toggle");
        add(toggleButton).width(size).height(size).pad(0);
    }

    // ---------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------

    /// Factory for a styled `TextButton`.
    ///
    /// @param glyph    unicode character to display.
    /// @param helpText helpful tooltip / accessibility message.
    /// @param action   runnable executed on click.
    /// @param style    common TaskView style containing the `buttonStyle`.
    /// @return a fully wired TextButton ready to be inserted into the table.
    private TextButton buildButton(
            final String glyph,
            final String helpText,
            final Runnable action,
            final AbstractTaskView.TaskViewStyle style,
            final Skin skin
    ) {
        TextButton btn = new TextButton(glyph, style.buttonStyle);
        btn.setTouchable(Touchable.enabled);
        btn.align(Align.center | Align.top);
        btn.getLabel().setAlignment(Align.top | Align.center);
//        btn.setPosition(0, 20);
        btn.getLabel().moveBy(0, -20);
        btn.addListener(new ClickListener() {
            @Override public void clicked(
                    final InputEvent event,
                    final float x,
                    final float y
            ) {
                action.run();
            }
        });
        if (helpText != null && !helpText.isEmpty()) {
            // TooltipManager is global; you can configure delays here if you want
            TooltipManager manager = TooltipManager.getInstance();
            // Make tooltips feel snappy & reliable
            manager.initialTime = 0.25f;     // delay before first show
            manager.subsequentTime = 0.1f;   // delay when moving between actors
//            manager.hideTime = 0.2f;         // fade out
            manager.resetTime = 0.3f;        // how long before it treats as "first show" again
            manager.maxWidth = 420f;         // avoid super-wide tooltips
            manager.edgeDistance = 8f;       // don't hug the edge of the window
            TextTooltip tooltip = new TextTooltip(helpText, manager, skin);
            tooltip.getActor().setWrap(true); // honor wrapWidth
            btn.addListener(tooltip);
        }

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