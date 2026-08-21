package net.wti.ui.components.browser;

/// Receives intent-level browser state and activation events.
///
/// Selection is navigation state. Activation is a separate caller intent and
/// is emitted only by an explicit activation request.
public interface EntityBrowserListener<E> {

    /// Called after filtering, paging, or source content changes.
    default void contentsChanged(final EntityBrowserModel<E> model) {
    }

    /// Called when the selected stable key changes.
    default void selectionChanged(
            final EntityBrowserModel<E> model,
            final String previousKey,
            final String selectedKey,
            final E selectedEntity
    ) {
    }

    /// Called for an explicit activation of the currently selected entity.
    default void activated(
            final EntityBrowserModel<E> model,
            final String stableKey,
            final E entity
    ) {
    }
}
