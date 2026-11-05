package net.wti.ui.controls.focus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntSet;
import java.util.ArrayList;
import java.util.List;

/** Render-thread keyboard navigation for ordinary Scene2D forms/boards. Tab wraps through visible,
 * enabled controls; Enter/Space activates a button once on release. Modal windows own traversal
 * while open. No domain action runs on focus, traversal or pointer-down. */
public final class KeyboardFocusScope implements Disposable {
    private final Stage stage;
    private final FocusStyles styles = new FocusStyles();
    private final IntSet heldKeys = new IntSet(3);
    private Actor armed;
    private int armedKey;
    private boolean shiftLeft, shiftRight, disposed;
    private final InputListener input = new InputListener() {
        @Override public boolean keyDown(final InputEvent event, final int key) {
            if (key == Keys.SHIFT_LEFT) shiftLeft = true;
            if (key == Keys.SHIFT_RIGHT) shiftRight = true;
            if (disposed) return false;
            if (key == Keys.TAB) {
                final boolean backwards = Gdx.input == null ? shiftLeft || shiftRight
                        : Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
                if (traverse(backwards)) { event.stop(); return true; }
            }
            if (key == Keys.ENTER || key == Keys.NUMPAD_ENTER || key == Keys.SPACE) {
                final boolean fresh = heldKeys.add(key);
                final Actor focus = stage.getKeyboardFocus();
                if (focus instanceof SelectBox && ((SelectBox<?>) focus).getScrollPane().getStage() == stage
                        && ((SelectBox<?>) focus).getList().isTouchable()) return false; // Native popup commits with Enter/Escape.
                if ((focus instanceof Button || focus instanceof SelectBox)
                        && enabled(focus) && inside(focus, activeRoot())) {
                    if (fresh && armed == null) { armed = focus; armedKey = key; }
                    event.stop(); return true;
                }
            }
            return false;
        }
        @Override public boolean keyTyped(final InputEvent event, final char character) {
            // Tab traversal belongs to keyDown; TextField would otherwise traverse again on keyTyped.
            if (disposed || character != '\t') return false;
            event.stop(); return true;
        }
        @Override public boolean keyUp(final InputEvent event, final int key) {
            if (key == Keys.SHIFT_LEFT) shiftLeft = false;
            if (key == Keys.SHIFT_RIGHT) shiftRight = false;
            final boolean held = heldKeys.remove(key);
            if (armed == null || key != armedKey) return false;
            final Actor target = armed; armed = null;
            if (held && valid(target)) {
                if (target instanceof Button) {
                    final Button button = (Button) target;
                    ButtonActivation.activate(button, () -> valid(target));
                } else ((SelectBox<?>) target).showList();
            }
            event.stop(); return true;
        }
        @Override public boolean touchDown(final InputEvent event, final float x, final float y,
                                           final int pointer, final int button) {
            final Group root = activeRoot();
            for (Actor actor = event.getTarget(); actor != null && inside(actor, root); actor = actor.getParent()) {
                if (control(actor) && enabled(actor)) { stage.setKeyboardFocus(actor); break; }
            }
            return false;
        }
    };
    private final FocusListener focus = new FocusListener() {
        @Override public void keyboardFocusChanged(final FocusEvent event, final Actor actor, final boolean focused) {
            if (focused) { styles.show(actor); reveal(actor); }
            else { armed = null; styles.clear(); }
        }
    };

    public KeyboardFocusScope(final Stage stage) {
        this.stage = stage;
        stage.getRoot().addCaptureListener(input);
        stage.getRoot().addCaptureListener(focus);
    }

    private boolean valid(final Actor target) {
        return !disposed && stage.getKeyboardFocus() == target && enabled(target) && inside(target, activeRoot());
    }

    private Group activeRoot() {
        final Group root = stage.getRoot();
        for (int i = root.getChildren().size - 1; i >= 0; i--) {
            final Actor actor = root.getChildren().get(i);
            if (actor.isVisible() && actor instanceof Window && ((Window) actor).isModal()) {
                return (Window) actor;
            }
        }
        return root;
    }

    private boolean traverse(final boolean backwards) {
        armed = null;
        final Group root = activeRoot();
        Actor selected = stage.getKeyboardFocus();
        final List<Actor> controls = new ArrayList<>(); collect(root, controls);
        // A SelectBox popup owns normal arrow/Enter/Escape handling. Tab closes it before leaving.
        for (final Actor actor : controls) if (actor instanceof SelectBox) {
            final SelectBox<?> box = (SelectBox<?>) actor;
            if (box.getScrollPane().getStage() == stage && (selected == box || selected == box.getList())) {
                box.hideList(); selected = box; break;
            }
        }
        final int previous = controls.indexOf(selected), count = controls.size();
        for (int step = 1; step <= count; step++) {
            final int index = Math.floorMod((previous < 0 ? (backwards ? 0 : -1) : previous)
                    + (backwards ? -step : step), count);
            final Actor actor = controls.get(index);
            if (enabled(actor) && stage.setKeyboardFocus(actor)) { reveal(actor); return true; }
        }
        return false;
    }

    private static boolean control(final Actor actor) {
        return actor instanceof Button || actor instanceof SelectBox || actor instanceof TextField;
    }
    private static void collect(final Group group, final List<Actor> result) {
        for (final Actor actor : group.getChildren()) {
            if (!actor.isVisible()) continue;
            if (control(actor)) result.add(actor);
            else if (actor instanceof Group) collect((Group) actor, result);
        }
    }
    private static boolean enabled(final Actor actor) {
        if (actor.getStage() == null) return false;
        for (Actor part = actor; part != null; part = part.getParent()) {
            if (!part.isVisible() || part.getTouchable() == Touchable.disabled) return false;
        }
        return !(actor instanceof Button && ((Button) actor).isDisabled())
                && !(actor instanceof SelectBox && ((SelectBox<?>) actor).isDisabled())
                && !(actor instanceof TextField && ((TextField) actor).isDisabled());
    }
    private static boolean inside(final Actor actor, final Group root) {
        return actor != null && (actor == root || actor.isDescendantOf(root));
    }
    private static void reveal(final Actor actor) {
        for (Group parent = actor.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane) {
                final ScrollPane pane = (ScrollPane) parent;
                pane.validate();
                final Vector2 position = actor.localToAscendantCoordinates(pane.getActor(), new Vector2());
                pane.scrollTo(position.x, position.y, actor.getWidth(), actor.getHeight());
                pane.updateVisualScroll();
            }
        }
    }

    /** Keep a named control focused when an acknowledged projection replaces its actors.
     * A disabled replacement may retain focus for context; activation still checks enabled(). */
    public static void preserveFocus(final Group root, final Runnable update) {
        final Stage stage = root.getStage();
        final Actor old = stage == null ? null : stage.getKeyboardFocus();
        final boolean owned = inside(old, root);
        final String name = owned ? old.getName() : null;
        update.run();
        if (!owned || root.getStage() != stage) return;
        Actor replacement = name == null ? null : root.findActor(name);
        if (replacement == null) {
            final List<Actor> controls = new ArrayList<>(); collect(root, controls);
            for (final Actor candidate : controls) if (enabled(candidate)) { replacement = candidate; break; }
        }
        stage.setKeyboardFocus(replacement);
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true; armed = null; heldKeys.clear(); styles.clear();
        stage.getRoot().removeCaptureListener(input);
        stage.getRoot().removeCaptureListener(focus);
    }
}
