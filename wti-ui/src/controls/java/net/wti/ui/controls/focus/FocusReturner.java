package net.wti.ui.controls.focus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

///
/// FocusReturner: A focus management utility that remembers and restores keyboard and scroll focus.
/// Extends ClickListener to intercept click events and manage focus state.
///
/// When a click occurs, it can optionally save and restore both scroll focus and keyboard focus,
/// making it useful for UI components that temporarily steal focus but need to return it
/// to the previous focus target.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 30/08/2025 @ 03:34
public class FocusReturner extends ClickListener {

    private Actor prevScrollFocus;
    private Actor prevKeyboardFocus;

    private final boolean restoreScroll;
    private final boolean restoreKeyboard;

    public FocusReturner() {
        this(true, true);
    }

    public FocusReturner(boolean restoreScroll, boolean restoreKeyboard) {
        this.restoreScroll = restoreScroll;
        this.restoreKeyboard = restoreKeyboard;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        Stage stage = event.getStage();
        if (stage != null) {
            if (restoreScroll)   prevScrollFocus   = stage.getScrollFocus();
            if (restoreKeyboard) prevKeyboardFocus = stage.getKeyboardFocus();
        }
        return super.touchDown(event, x, y, pointer, button);
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        final Stage stage = event.getStage();
        final Actor self = event.getListenerActor();
        if (stage == null) return;

        // Defer restoring focus to the end of this frame so other listeners finish first
        Gdx.app.postRunnable(() -> {
            if (restoreScroll) {
                Actor target = isInStage(prevScrollFocus, stage) ? prevScrollFocus : nearestScrollPane(self);
                if (target != null && target != stage.getScrollFocus()) {
                    stage.setScrollFocus(target);
                }
            }
            if (restoreKeyboard) {
                if (isInStage(prevKeyboardFocus, stage) && prevKeyboardFocus != stage.getKeyboardFocus()) {
                    stage.setKeyboardFocus(prevKeyboardFocus);
                }
            }
        });
    }

    private static boolean isInStage(Actor a, Stage stage) {
        if (a == null || stage == null) return false;
        // Walk up parents to ensure this actor is still in this stage
        Actor cur = a;
        while (cur != null) {
            if (cur == stage.getRoot()) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private static ScrollPane nearestScrollPane(Actor a) {
        Actor cur = a;
        while (cur != null) {
            if (cur instanceof ScrollPane) return (ScrollPane) cur;
            cur = cur.getParent();
        }
        return null;
    }

    // Convenience
    public static void attach(Actor actor) {
        actor.addListener(new FocusReturner());
    }

    public static void attach(Actor actor, boolean restoreScroll, boolean restoreKeyboard) {
        actor.addListener(new FocusReturner(restoreScroll, restoreKeyboard));
    }
}

