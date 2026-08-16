package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Array;

/// A layout-owned host for multiple floating panels.
///
/// The layer has no input listener or background and is touchable only through its
/// children, so unused space remains available to actors beneath it.
public final class FloatingPanelLayer extends WidgetGroup {

    private final Array<FloatingPanel> panels = new Array<FloatingPanel>();
    private final FloatingPanelStyle style;

    public FloatingPanelLayer(final FloatingPanelStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style must not be null");
        }
        this.style = style;
        setTouchable(Touchable.childrenOnly);
    }

    public void addPanel(final FloatingPanel panel) {
        if (panel == null) {
            throw new IllegalArgumentException("panel must not be null");
        }
        if (panel.getParent() != null && panel.getParent() != this) {
            throw new IllegalArgumentException("panel already has a different parent");
        }
        if (panels.contains(panel, true)) {
            return;
        }
        panels.add(panel);
        addActor(panel);
        panel.validate();
        if (panel.getWidth() <= 0f) {
            panel.setWidth(Math.max(panel.getPrefWidth(), panel.getMinWidth()));
        }
        if (panel.getHeight() <= 0f) {
            panel.setHeight(Math.max(panel.getPrefHeight(), panel.getMinHeight()));
        }
        placeCascade(panel);
        invalidateHierarchy();
    }

    public void removePanel(final FloatingPanel panel) {
        if (panel == null) {
            return;
        }
        panels.removeValue(panel, true);
        if (panel.getParent() == this) {
            panel.remove();
        }
    }

    /// Places a panel at a deterministic offset derived from its current order.
    public void placeCascade(final FloatingPanel panel) {
        if (panel == null) {
            throw new IllegalArgumentException("panel must not be null");
        }
        final int index = Math.max(0, panels.indexOf(panel, true));
        final float offset = nonNegative(style.edgePadding) +
                index * nonNegative(style.cascadeGap);
        panel.setPosition(offset, getHeight() - offset - panel.getHeight());
        clampPanel(panel);
    }

    /// Clamps every panel using the layer's current parent-local bounds.
    public void clampPanels() {
        for (final FloatingPanel panel : panels) {
            clampPanel(panel);
        }
    }

    public Array<FloatingPanel> getPanels() {
        return new Array<FloatingPanel>(panels);
    }

    @Override
    public void layout() {
        clampPanels();
    }

    private void clampPanel(final FloatingPanel panel) {
        if (!panel.isLockedInsideParent()) {
            return;
        }
        final float padding = nonNegative(style.edgePadding);
        final float availableWidth = Math.max(0f, getWidth() - padding * 2f);
        final float availableHeight = Math.max(0f, getHeight() - padding * 2f);
        if (panel.getWidth() > availableWidth) {
            panel.setWidth(availableWidth);
        }
        if (panel.getHeight() > availableHeight) {
            panel.setHeight(availableHeight);
        }
        final float maxX = Math.max(padding, getWidth() - padding - panel.getWidth());
        final float maxY = Math.max(padding, getHeight() - padding - panel.getHeight());
        panel.setPosition(
                clamp(panel.getX(), padding, maxX),
                clamp(panel.getY(), padding, maxY)
        );
    }

    private static float clamp(final float value, final float minimum, final float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float nonNegative(final float value) {
        return Math.max(0f, value);
    }
}
