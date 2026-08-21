package net.wti.ui.components.browser;

/// Owns application-specific query semantics for an entity browser.
///
/// The browser preserves the query text supplied by the caller. Case folding,
/// tokenization, tag rules, and other matching policy remain outside the
/// reusable component.
@FunctionalInterface
public interface EntityBrowserMatcher<E> {

    /// Returns true when the entity belongs in the results for query.
    boolean matches(E entity, String query);
}
