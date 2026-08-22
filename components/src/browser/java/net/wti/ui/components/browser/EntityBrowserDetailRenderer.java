package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.Actor;

/// Creates optional inspection content for the currently selected entity.
@FunctionalInterface
public interface EntityBrowserDetailRenderer<E> {

    /// Creates caller-owned detail content for one selected entity.
    Actor createDetail(E entity, String stableKey);
}
