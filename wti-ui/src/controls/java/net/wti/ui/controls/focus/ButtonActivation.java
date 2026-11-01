package net.wti.ui.controls.focus;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Pools;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Keyboard user activation of registered actor click listeners, with ordinary left-button
 * acceptance and cancellation. No physical pointer or Stage touch focus is created. Button's
 * native user-change path preserves programmaticChangeEvents=false acknowledgement semantics. */
final class ButtonActivation {
    private ButtonActivation() { }

    static void activate(final Button button, final BooleanSupplier valid) {
        if (!valid.getAsBoolean()) return;
        final List<ClickListener> listeners = new ArrayList<>();
        collect(button.getCaptureListeners(), listeners);
        final int captures = listeners.size();
        collect(button.getListeners(), listeners);
        for (final ClickListener listener : listeners) if (listener.getPressedPointer() != -1) return;
        final Vector2 position = button.localToStageCoordinates(new Vector2(button.getWidth() / 2, button.getHeight() / 2));
        final InputEvent down = event(InputEvent.Type.touchDown, button, position);
        final InputEvent up = event(InputEvent.Type.touchUp, button, position);
        final List<ClickListener> accepted = new ArrayList<>();
        try {
            for (int i = 0; i < listeners.size(); i++) {
                if (i == captures && down.isStopped()) break;
                down.setCapture(i < captures);
                if (listeners.get(i).handle(down)) accepted.add(listeners.get(i));
            }
            if (down.isCancelled() || !valid.getAsBoolean()) for (final ClickListener listener : accepted) listener.cancel();
            boolean regular = false;
            for (final ClickListener listener : accepted) {
                final boolean capturing = listeners.indexOf(listener) < captures;
                if (!capturing && !regular) {
                    if (up.isStopped()) break;
                    regular = true;
                }
                up.setCapture(capturing);
                listener.handle(up);
            }
        } finally {
            // clicked() can throw before ClickListener has cleared its own press state.
            for (final ClickListener listener : listeners) {
                if (listener.getPressedPointer() == 0) { listener.cancel(); listener.handle(up); }
                listener.setVisualPressed(false);
            }
            Pools.free(down);
            Pools.free(up);
        }
    }

    private static InputEvent event(final InputEvent.Type type, final Button button, final Vector2 position) {
        final InputEvent event = Pools.obtain(InputEvent.class);
        event.setType(type);
        event.setStage(button.getStage());
        event.setTarget(button);
        event.setListenerActor(button);
        event.setStageX(position.x);
        event.setStageY(position.y);
        event.setPointer(0);
        event.setButton(Buttons.LEFT);
        event.setTouchFocus(false);
        event.setBubbles(false);
        return event;
    }

    private static void collect(final Iterable<EventListener> source, final List<ClickListener> result) {
        for (final EventListener listener : source) if (listener instanceof ClickListener) result.add((ClickListener) listener);
    }
}
