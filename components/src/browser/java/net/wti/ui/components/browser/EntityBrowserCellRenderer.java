package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.Actor;

/// Creates caller-owned Scene2D content for generic browser cells.
///
/// The reusable browser owns layout and intent wiring. Product thumbnails,
/// text, colors, fonts, and domain metadata remain with the caller.
public interface EntityBrowserCellRenderer<E> {

    /// Creates the actor used for one entity on the current page.
    Actor createCell(E entity, String stableKey, EntityBrowserCellState state);

    /// Updates selection/focus state without requiring a full page rebuild.
    default void updateCell(
            final Actor cell,
            final E entity,
            final String stableKey,
            final EntityBrowserCellState state
    ) {
    }
}
