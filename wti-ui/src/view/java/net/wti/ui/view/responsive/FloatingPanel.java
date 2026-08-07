package net.wti.ui.view.responsive;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/// A generic titled floating panel whose title is its only drag handle.
///
/// The title and viewport remain in one Table hierarchy, so rendering, clipping, and hit
/// testing all use the same actor bounds. Dragging converts stage coordinates into the
/// panel parent's local coordinates on every move.
public final class FloatingPanel extends Table {

    private final Actor titleActor;
    private final Actor contentActor;
    private final ResponsiveContentScrollPane viewport;
    private final FloatingPanelStyle style;
    private final Vector2 dragOffset = new Vector2();
    private final Vector2 resizePoint = new Vector2();
    private final Vector2 resizeStartParent = new Vector2();
    private final Table resizeHandle = new Table();
    private final Cell<Table> resizeCell;
    private boolean resizable;
    private boolean lockedInsideParent = true;
    private boolean minimized;
    private float expandedWidth;
    private float expandedHeight;
    private float resizeStartWidth;
    private float resizeStartHeight;
    private Runnable redockAction;

    public FloatingPanel(
            final Actor titleActor,
            final Actor contentActor,
            final ScrollPane.ScrollPaneStyle scrollStyle,
            final FloatingPanelStyle style
    ) {
        if (titleActor == null) {
            throw new IllegalArgumentException("titleActor must not be null");
        }
        if (contentActor == null) {
            throw new IllegalArgumentException("contentActor must not be null");
        }
        if (scrollStyle == null) {
            throw new IllegalArgumentException("scrollStyle must not be null");
        }
        if (style == null) {
            throw new IllegalArgumentException("style must not be null");
        }
        this.titleActor = titleActor;
        this.contentActor = contentActor;
        this.style = style;
        viewport = new ResponsiveContentScrollPane(contentActor, scrollStyle);
        top().left();
        pad(nonNegative(style.edgePadding));
        add(titleActor).growX().fillX().top().left().minWidth(0f);
        row().padTop(nonNegative(style.titleContentGap));
        add(viewport).grow().fill().top().left().minWidth(0f).minHeight(0f);
        row();
        resizeHandle.setTouchable(Touchable.enabled);
        resizeCell = add(resizeHandle)
                .right()
                .size(nonNegative(style.resizeHandleSize))
                .minWidth(0f)
                .minHeight(0f);
        resizeHandle.setVisible(false);
        installDragHandle();
        installResizeHandle();
    }

    public Actor getTitleActor() {
        return titleActor;
    }

    public Actor getContentActor() {
        return contentActor;
    }

    public ResponsiveContentScrollPane getViewport() {
        return viewport;
    }

    public FloatingPanelStyle getFloatingStyle() {
        return style;
    }

    /// Enables or disables the small bottom-right resize handle.
    public void setResizable(final boolean resizable) {
        this.resizable = resizable;
        updateResizeHandle();
    }

    public boolean isResizable() {
        return resizable;
    }

    /// Returns the optional bottom-right resize handle for caller styling or inspection.
    public Actor getResizeHandle() {
        return resizeHandle;
    }

    /// Applies caller-owned visual styling to the optional resize affordance.
    public void setResizeHandleBackground(final Drawable background) {
        resizeHandle.setBackground(background);
    }

    /// Controls whether the containing layer clamps this panel inside its bounds.
    ///
    /// A layer assigned the stage viewport bounds therefore provides stage locking while
    /// keeping the panel implementation independent of any particular Stage or viewport.
    public void setLockedInsideParent(final boolean locked) {
        lockedInsideParent = locked;
        if (locked && getParent() instanceof FloatingPanelLayer) {
            ((FloatingPanelLayer) getParent()).clampPanels();
        }
    }

    public boolean isLockedInsideParent() {
        return lockedInsideParent;
    }

    /// Collapses the content and resize handle while retaining the expanded panel size.
    public void setMinimized(final boolean minimized) {
        if (this.minimized == minimized) {
            return;
        }
        if (minimized) {
            validate();
            expandedWidth = getWidth();
            expandedHeight = getHeight();
        }
        this.minimized = minimized;
        viewport.setVisible(!minimized);
        updateResizeHandle();
        invalidateHierarchy();
        if (minimized) {
            setHeight(minimizedHeight());
        } else {
            setSize(expandedWidth, expandedHeight);
        }
        if (getParent() instanceof FloatingPanelLayer) {
            ((FloatingPanelLayer) getParent()).clampPanels();
        }
    }

    public boolean isMinimized() {
        return minimized;
    }

    /// Installs a caller-owned background without coupling layout policy to a Skin.
    public void setPanelBackground(final Drawable background) {
        background(background);
    }

    /// Sets the caller action used to remove or redock this panel.
    public void setRedockAction(final Runnable action) {
        redockAction = action;
    }

    /// Invokes the caller's redock action, if one has been supplied.
    public void redock() {
        if (redockAction != null) {
            redockAction.run();
        }
    }

    @Override
    public float getMinWidth() {
        return Math.max(super.getMinWidth(), nonNegative(style.minimumWidth));
    }

    @Override
    public float getMinHeight() {
        if (minimized) {
            return minimizedHeight();
        }
        return Math.max(super.getMinHeight(), nonNegative(style.minimumHeight));
    }

    private void updateResizeHandle() {
        final boolean visible = resizable && !minimized;
        resizeHandle.setVisible(visible);
        resizeCell.size(visible ? nonNegative(style.resizeHandleSize) : 0f);
        invalidateHierarchy();
    }

    private void installDragHandle() {
        titleActor.addListener(new InputListener() {
            @Override
            public boolean touchDown(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer,
                    final int button
            ) {
                if (pointer != 0 || getParent() == null) {
                    return false;
                }
                final Vector2 parentPoint = getParent().stageToLocalCoordinates(
                        new Vector2(event.getStageX(), event.getStageY())
                );
                dragOffset.set(parentPoint.x - getX(), parentPoint.y - getY());
                return true;
            }

            @Override
            public void touchDragged(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer
            ) {
                if (pointer != 0 || getParent() == null) {
                    return;
                }
                final Vector2 parentPoint = getParent().stageToLocalCoordinates(
                        new Vector2(event.getStageX(), event.getStageY())
                );
                setPosition(parentPoint.x - dragOffset.x, parentPoint.y - dragOffset.y);
            }
        });
    }

    private void installResizeHandle() {
        resizeHandle.addListener(new InputListener() {
            @Override
            public boolean touchDown(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer,
                    final int button
            ) {
                if (!resizable || minimized || pointer != 0 || getParent() == null) {
                    return false;
                }
                resizePoint.set(event.getStageX(), event.getStageY());
                resizeStartParent.set(
                        getParent().stageToLocalCoordinates(
                                new Vector2(event.getStageX(), event.getStageY())
                        )
                );
                resizeStartWidth = getWidth();
                resizeStartHeight = getHeight();
                return true;
            }

            @Override
            public void touchDragged(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer
            ) {
                if (!resizable || minimized || pointer != 0) {
                    return;
                }
                final Vector2 parentPoint = getParent().stageToLocalCoordinates(
                        new Vector2(event.getStageX(), event.getStageY())
                );
                setSize(
                        Math.max(getMinWidth(), resizeStartWidth + parentPoint.x - resizeStartParent.x),
                        Math.max(getMinHeight(), resizeStartHeight + parentPoint.y - resizeStartParent.y)
                );
                invalidateHierarchy();
                resizePoint.set(event.getStageX(), event.getStageY());
                if (lockedInsideParent && getParent() instanceof FloatingPanelLayer) {
                    ((FloatingPanelLayer) getParent()).clampPanels();
                }
            }
        });
    }

    private float minimizedHeight() {
        float titleHeight = titleActor.getHeight();
        if (titleHeight <= 0f && titleActor instanceof com.badlogic.gdx.scenes.scene2d.utils.Layout) {
            titleHeight = ((com.badlogic.gdx.scenes.scene2d.utils.Layout) titleActor).getPrefHeight();
        }
        return Math.max(0f, titleHeight + nonNegative(style.edgePadding) * 2f);
    }

    private static float nonNegative(final float value) {
        return Math.max(0f, value);
    }
}
