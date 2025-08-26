package net.wti.ui.components;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.Align;

/// A `TextTooltip` variant that **forces horizontal wrapping** without
/// overriding any input/listener methods (compatible with libGDX 2.13.x).
///
/// How it works:
/// - Enables `Label.setWrap(true)`
/// - Assigns a concrete width using `TooltipManager.maxWidth` to both the
///   tooltip container and the label
/// - Invalidates layout so the size sticks before first show
///
/// Usage:
/// ```java
/// TooltipManager tm = TooltipManager.getInstance();
/// tm.maxWidth = 420f; // pick something sane (>= 360)
/// actor.addListener(new SizingTextTooltip("Help text", tm,
///     skin.get("tooltip-default", TextTooltip.TextTooltipStyle.class)));
/// ```
///
/// Created by James X. Nelson (James@WeTheInter.net) on 25/08/2025 @ 17:31
public class SizingTextTooltip extends TextTooltip {

    // Minimum useful width to avoid very narrow tooltips
    public static final float MIN_W = 80f;
    public static final float MAX_W = 320;
    // Inner padding applied by the container (L/R used when we clamp)
    private static final float PAD_TOP = 4f, PAD_LEFT = 6f, PAD_BOTTOM = 4f, PAD_RIGHT = 6f;

    public SizingTextTooltip(String text, TooltipManager manager, TextTooltipStyle style) {
        super(text, manager, style);

        final Label lbl = getActor();
        lbl.setWrap(true);
        lbl.setAlignment(Align.left);

        final Container<Label> c = getContainer();
        c.pad(PAD_TOP, PAD_LEFT, PAD_BOTTOM, PAD_RIGHT);

        // Measure natural (unwrapped) width using the label style’s font
        final BitmapFont font = lbl.getStyle().font;
        final GlyphLayout layout = new GlyphLayout(font, text);
        float natural = layout.width + PAD_LEFT + PAD_RIGHT;

        // Choose width = clamp(natural, MIN_W, manager.maxWidth)
        float maxW = (manager.maxWidth > 0f ? manager.maxWidth : MAX_W);
        float chosen = Math.max(MIN_W, Math.min(natural, maxW));

        // Apply chosen width to container; label wraps within container’s width
        c.minWidth(chosen);
        c.prefWidth(chosen);
        c.maxWidth(chosen);

        // Let the label know its available width (container minus padding)
        lbl.setWidth(chosen - PAD_LEFT - PAD_RIGHT);

        // Ensure the new constraints are honored before first show
        lbl.invalidateHierarchy();
        c.invalidateHierarchy();
    }
}