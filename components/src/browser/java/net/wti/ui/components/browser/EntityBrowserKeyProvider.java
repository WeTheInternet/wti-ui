package net.wti.ui.components.browser;

/// Supplies the stable identity used by an EntityBrowserModel.
///
/// Keys must be non-empty and unique within one browser source snapshot. The
/// browser intentionally does not use object identity so callers may replace
/// immutable entity snapshots without losing selection.
@FunctionalInterface
public interface EntityBrowserKeyProvider<E> {

    /// Returns the durable key for one entity.
    String keyOf(E entity);
}
