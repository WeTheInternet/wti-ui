package net.wti.ui.view.responsive;

/// Measured layout policy shared by floating panels and their host layer.
///
/// Values are expressed in Scene2D units. This class intentionally contains no screen,
/// game, persistence, or domain coordinates.
public class FloatingPanelStyle {

    public float edgePadding;
    public float cascadeGap;
    public float minimumWidth;
    public float minimumHeight;
    public float titleContentGap;
    public float resizeHandleSize = 14f;

    public FloatingPanelStyle() {
    }

    /// Copies layout policy without sharing mutable configuration between consumers.
    public FloatingPanelStyle(final FloatingPanelStyle other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        edgePadding = other.edgePadding;
        cascadeGap = other.cascadeGap;
        minimumWidth = other.minimumWidth;
        minimumHeight = other.minimumHeight;
        titleContentGap = other.titleContentGap;
        resizeHandleSize = other.resizeHandleSize;
    }
}
