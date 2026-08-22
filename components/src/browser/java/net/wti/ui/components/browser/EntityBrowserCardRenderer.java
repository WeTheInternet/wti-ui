package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/// Adapts generic entities into reusable EntityBrowserCard instances.
public final class EntityBrowserCardRenderer<E>
        implements EntityBrowserCellRenderer<E> {

    private final Skin skin;
    private final EntityBrowserCardStyle style;
    private final EntityBrowserCardContentProvider<? super E> contentProvider;

    /// Creates a renderer from caller Skin/style and entity presentation data.
    public EntityBrowserCardRenderer(
            final Skin skin,
            final EntityBrowserCardStyle style,
            final EntityBrowserCardContentProvider<? super E> contentProvider
    ) {
        if (skin == null) {
            throw new IllegalArgumentException("skin cannot be null");
        }
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null");
        }
        if (contentProvider == null) {
            throw new IllegalArgumentException("contentProvider cannot be null");
        }
        this.skin = skin;
        this.style = style;
        this.contentProvider = contentProvider;
    }

    /// Creates a renderer by resolving EntityBrowserCardStyle from a Skin.
    public EntityBrowserCardRenderer(
            final Skin skin,
            final String styleName,
            final EntityBrowserCardContentProvider<? super E> contentProvider
    ) {
        this(
                skin,
                skin.get(styleName, EntityBrowserCardStyle.class),
                contentProvider
        );
    }

    @Override
    public Actor createCell(
            final E entity,
            final String stableKey,
            final EntityBrowserCellState state
    ) {
        return new EntityBrowserCard(
                skin,
                style,
                contentProvider.createContent(entity, skin),
                contentProvider.primaryText(entity),
                contentProvider.secondaryText(entity),
                contentProvider.tooltipText(entity),
                state
        );
    }

    @Override
    public void updateCell(
            final Actor cell,
            final E entity,
            final String stableKey,
            final EntityBrowserCellState state
    ) {
        if (!(cell instanceof EntityBrowserCard)) {
            throw new IllegalArgumentException(
                    "EntityBrowserCardRenderer can update only EntityBrowserCard cells"
            );
        }
        ((EntityBrowserCard) cell).setBrowserState(state);
    }

    /// Returns the caller Skin used for card construction.
    public Skin getSkin() {
        return skin;
    }

    /// Returns the shared Skin-driven card style.
    public EntityBrowserCardStyle getStyle() {
        return style;
    }
}
