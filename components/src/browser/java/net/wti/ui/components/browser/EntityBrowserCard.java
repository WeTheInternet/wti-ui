package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.components.IsSkinnable;
import net.wti.ui.components.SizingTextTooltip;

/// Reusable Skin-driven card chrome for browser content.
///
/// Hover is local UI state. Selection and keyboard-current state are supplied
/// by the browser model/actor and never mutate the browsed entity.
public class EntityBrowserCard extends Table implements IsSkinnable {

    private final Skin skin;
    private final EntityBrowserCardStyle style;
    private final Actor contentActor;
    private final Label primaryLabel;
    private final Label secondaryLabel;

    private EntityBrowserCellState browserState;
    private boolean hovered;

    /// Creates one card with optional body, labels, and tooltip.
    public EntityBrowserCard(
            final Skin skin,
            final EntityBrowserCardStyle style,
            final Actor contentActor,
            final String primaryText,
            final String secondaryText,
            final String tooltipText,
            final EntityBrowserCellState browserState
    ) {
        if (skin == null) {
            throw new IllegalArgumentException("skin cannot be null");
        }
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null");
        }
        if (browserState == null) {
            throw new IllegalArgumentException("browserState cannot be null");
        }
        this.skin = skin;
        this.style = style;
        this.contentActor = contentActor;
        this.browserState = browserState;
        this.primaryLabel = label(primaryText, style.primaryLabelStyle);
        this.secondaryLabel = label(secondaryText, style.secondaryLabelStyle);

        pad(style.padding);
        defaults().growX();
        if (contentActor != null) {
            add(contentActor).grow();
            row();
        }
        if (primaryLabel != null) {
            if (contentActor != null) {
                add(primaryLabel).padTop(style.contentTextGap).left();
            } else {
                add(primaryLabel).left();
            }
            row();
        }
        if (secondaryLabel != null) {
            add(secondaryLabel)
                    .padTop(primaryLabel == null ? style.contentTextGap : style.textGap)
                    .left();
            row();
        }
        installHover();
        installTooltip(tooltipText);
        applyBackground();
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    /// Returns the caller-provided body/thumbnail actor, if any.
    public Actor getContentActor() {
        return contentActor;
    }

    /// Returns the optional primary label.
    public Label getPrimaryLabel() {
        return primaryLabel;
    }

    /// Returns the optional secondary label.
    public Label getSecondaryLabel() {
        return secondaryLabel;
    }

    /// Returns true while pointer hover chrome is active.
    public boolean isHovered() {
        return hovered;
    }

    /// Applies model-owned selection/focus state.
    public void setBrowserState(final EntityBrowserCellState browserState) {
        if (browserState == null) {
            throw new IllegalArgumentException("browserState cannot be null");
        }
        this.browserState = browserState;
        applyBackground();
    }

    /// Returns the latest model-owned cell state.
    public EntityBrowserCellState getBrowserState() {
        return browserState;
    }

    @Override
    public float getPrefWidth() {
        return Math.max(super.getPrefWidth(), style.minimumWidth);
    }

    @Override
    public float getPrefHeight() {
        return Math.max(super.getPrefHeight(), style.minimumHeight);
    }

    private Label label(final String text, final Label.LabelStyle labelStyle) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        final Label label = labelStyle == null
                ? new Label(text, skin)
                : new Label(text, labelStyle);
        label.setWrap(true);
        label.setAlignment(Align.left);
        return label;
    }

    private void installHover() {
        addListener(new InputListener() {
            @Override
            public void enter(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer,
                    final Actor fromActor
            ) {
                hovered = true;
                applyBackground();
            }

            @Override
            public void exit(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer,
                    final Actor toActor
            ) {
                hovered = false;
                applyBackground();
            }
        });
    }

    private void installTooltip(final String tooltipText) {
        if (tooltipText == null || tooltipText.isEmpty()) {
            return;
        }
        if (style.tooltipStyle != null) {
            addListener(new SizingTextTooltip(
                    tooltipText,
                    TooltipManager.getInstance(),
                    style.tooltipStyle
            ));
        } else if (skin.has("tooltip-default", TextTooltip.TextTooltipStyle.class)) {
            addListener(new SizingTextTooltip(tooltipText, skin));
        }
    }

    private void applyBackground() {
        final Drawable background;
        if (browserState.isKeyboardCurrent() && style.keyboardCurrent != null) {
            background = style.keyboardCurrent;
        } else if (browserState.isSelected() && style.selected != null) {
            background = style.selected;
        } else if (hovered && style.hovered != null) {
            background = style.hovered;
        } else {
            background = style.normal;
        }
        setBackground(background);
    }
}
