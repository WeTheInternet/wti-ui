package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/// Skin-owned visual contract for EntityBrowserCard.
///
/// Drawables and label styles are supplied by the active Skin or caller. The
/// reusable component does not allocate textures or impose product colors.
public class EntityBrowserCardStyle {

    public Drawable normal;
    public Drawable hovered;
    public Drawable selected;
    public Drawable keyboardCurrent;
    public Label.LabelStyle primaryLabelStyle;
    public Label.LabelStyle secondaryLabelStyle;
    /// Optional style override; otherwise the Skin's tooltip-default is used.
    public TextTooltip.TextTooltipStyle tooltipStyle;
    public float padding = 8f;
    public float contentTextGap = 6f;
    public float textGap = 2f;
    public float minimumWidth;
    public float minimumHeight;

    public EntityBrowserCardStyle() {
    }

    /// Copies a style so callers can derive local variants without mutation.
    public EntityBrowserCardStyle(final EntityBrowserCardStyle other) {
        if (other == null) {
            throw new IllegalArgumentException("other cannot be null");
        }
        normal = other.normal;
        hovered = other.hovered;
        selected = other.selected;
        keyboardCurrent = other.keyboardCurrent;
        primaryLabelStyle = other.primaryLabelStyle;
        secondaryLabelStyle = other.secondaryLabelStyle;
        tooltipStyle = other.tooltipStyle;
        padding = other.padding;
        contentTextGap = other.contentTextGap;
        textGap = other.textGap;
        minimumWidth = other.minimumWidth;
        minimumHeight = other.minimumHeight;
    }
}
