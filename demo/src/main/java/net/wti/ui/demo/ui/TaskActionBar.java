package net.wti.ui.demo.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.components.SymbolButton;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.common.DemoConstants;
import net.wti.ui.demo.i18n.Messages;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.ui.view.AbstractTaskView;
import net.wti.ui.demo.view.api.IsTaskView;
import xapi.time.X_Time;

import static net.wti.ui.api.TimeConstants.ONE_DAY;

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
    private static final String GLYPH_EXPAND   = "+"; // down arrows; consider ▼ or ⯯
    private static final String GLYPH_COLLAPSE = "-"; // down arrows; consider ▲ or ⯯

    // ---------------------------------------------------------------------
    // Style names
    // ---------------------------------------------------------------------
    final String STYLE_NORMAL = "actionbar";
    final String STYLE_PRIMARY = "actionbar-emphasis";

    // ---------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------

    /// Reference to the parent TaskView (or any IsTaskView implementation).
    private final IsTaskView<ModelTask> parentView;
    private final SymbolButton edit;
    private final SymbolButton defer;
    private final SymbolButton cancel;
    private final SymbolButton finish;

    /// Cached reference so we can flip text instead of searching the scene graph.
    private SymbolButton toggleButton;

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
        defaults().space(2).pad(0f);

        if (style.background != null) {
            setBackground(style.background);
        }
        /* build individual buttons */
        Messages msgs = DemoConstants.MESSAGES;


        /* expand / collapse button (initial state follows view) */
        edit = new SymbolButton(GLYPH_EDIT, STYLE_NORMAL, skin, msgs.buttonEdit());
        defer = new SymbolButton(GLYPH_DEFER, STYLE_NORMAL, skin, msgs.buttonReschedule());
        cancel = new SymbolButton(GLYPH_CANCEL, STYLE_NORMAL, skin, msgs.buttonCancel());
        finish = new SymbolButton(GLYPH_FINISH, STYLE_PRIMARY, skin, msgs.buttonFinish());

        toggleButton = new SymbolButton(
                view.isExpanded() ? GLYPH_COLLAPSE : GLYPH_EXPAND,
                STYLE_NORMAL,
                skin,
                view.isExpanded() ? msgs.buttonMinimize() : msgs.buttonMaximize()
        );
        toggleButton.setName("task-btn-toggle");
        float targetH = 29f; // try 26–30 until it feels right on your font/monitor

        float hEdit   = edit.clampedSquare(targetH);
        float hDefer  = defer.clampedSquare(targetH);
        float hCancel = cancel.clampedSquare(targetH);
        float hFinish = finish.clampedSquare(targetH);
        float hToggle = toggleButton.clampedSquare(targetH);
        float tallest = Math.min(targetH, Math.max(hEdit, Math.max(hDefer, Math.max(hCancel, Math.max(hFinish, hToggle)))));

        // Wire actions
        edit.addListener(click(() -> controller.edit(view)));
        defer.addListener(click(() -> controller.defer(view.getTask())));
        cancel.addListener(click(() -> controller.cancel(view.getTask(), TaskController.CancelMode.NEXT, X_Time.nowMillis() + ONE_DAY)));
        finish.addListener(click(() -> controller.markAsDone(view.getTask())));
        toggleButton.addListener(click(this::toggleExpand));

        // Add to table with runtime sizes

        add(toggleButton).size(hToggle, tallest).pad(0);
        divider(targetH);
        add(edit  ).size(hEdit,   tallest).pad(0);
        divider(targetH);
        add(defer ).size(hDefer,  tallest).pad(0);
        divider(targetH);
        add(cancel).size(hCancel, tallest).pad(0);
        divider(targetH);
        add(finish).size(hFinish, tallest).pad(0);

        // Let the whole row stretch horizontally if you want fill behavior:
        getCells().forEach(c -> c.expandX().fillX());
        // label alignments for crisp glyph placement
        for (Cell<?> c : getCells()) {
            c = c.expandX().fillX();
            if (c.getActor() instanceof TextButton) {
                TextButton b = (TextButton) c.getActor();
                b.align(Align.center);
                b.getLabel().setAlignment(Align.center);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------

    private static ClickListener click(Runnable r) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { r.run(); }
        };
    }

    /// Toggles the parent view’s expanded state and flips the glyph.
    private void toggleExpand() {
        parentView.toggleExpanded();
        toggleButton.setText(
                parentView.isExpanded() ? GLYPH_COLLAPSE : GLYPH_EXPAND
        );
    }

    // After each button (except the last), drop a short divider
    private Cell<Image> divider(float barHeight) {
        Drawable d = getSkin().getDrawable("divider-vert");
        Image img = new Image(d);
        float h = barHeight * 0.66f; // not full height
        return add(img).width(2f).height(h).padLeft(2f).padRight(2f);
    }
}