package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/// Numeric spacing and presentation policy for a responsive panel hierarchy.
///
/// Values are expressed in Scene2D units. Like other libGDX style objects, fields may be
/// populated from a `Skin` or configured directly before the style is installed on a
/// `ResponsivePanel`. Call `ResponsivePanel.setStyle(...)` after changing a live style so
/// the hierarchy is invalidated and rebuilt.
public class ResponsivePanelStyle {

    public Drawable background;
    public float minimumColumnWidth = 320f;
    public float panelPadTop;
    public float panelPadLeft;
    public float panelPadBottom;
    public float panelPadRight;
    public float columnGap;
    public float sectionGap;
    public float headingGap;
    public float rowGap;
    public float rowPadTop;
    public float rowPadLeft;
    public float rowPadBottom;
    public float rowPadRight;
    public float leadingBodyGap;

    public ResponsivePanelStyle() {
    }

    /// Copies a style so a consumer can derive local spacing without mutating a shared
    /// `Skin` style instance.
    public ResponsivePanelStyle(final ResponsivePanelStyle other) {
        background = other.background;
        minimumColumnWidth = other.minimumColumnWidth;
        panelPadTop = other.panelPadTop;
        panelPadLeft = other.panelPadLeft;
        panelPadBottom = other.panelPadBottom;
        panelPadRight = other.panelPadRight;
        columnGap = other.columnGap;
        sectionGap = other.sectionGap;
        headingGap = other.headingGap;
        rowGap = other.rowGap;
        rowPadTop = other.rowPadTop;
        rowPadLeft = other.rowPadLeft;
        rowPadBottom = other.rowPadBottom;
        rowPadRight = other.rowPadRight;
        leadingBodyGap = other.leadingBodyGap;
    }
}
