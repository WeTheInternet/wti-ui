package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/// Maps one generic entity into caller-owned card content and metadata.
///
/// Every method is optional so image-heavy, text-heavy, and custom-body cards
/// can share the same renderer without placeholder domain concepts.
public interface EntityBrowserCardContentProvider<E> {

    /// Creates optional thumbnail/body content for one card.
    default Actor createContent(final E entity, final Skin skin) {
        return null;
    }

    /// Returns optional primary card text.
    default String primaryText(final E entity) {
        return null;
    }

    /// Returns optional secondary card text.
    default String secondaryText(final E entity) {
        return null;
    }

    /// Returns optional tooltip text.
    default String tooltipText(final E entity) {
        return null;
    }
}
