package net.wti.ui.components;


import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;

/// SymbolButton:
///
/// A tiny wrapper around TextButton that uses an emoji glyph,
/// caches its preferred square size after first pack(), and applies a tooltip.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 25/08/2025 @ 14:45
/** Button that displays a single symbol/emoji and sizes itself square based on atlas+font. */
public class SymbolButton extends TextButton {
    // ---------------------------------------------------------------------
    // Style names
    // ---------------------------------------------------------------------
    public static final String STYLE_NORMAL = "actionbar";
    public static final String STYLE_PRIMARY = "actionbar-emphasis";
    private static final ObjectMap<String, Float> SIZE_CACHE = new ObjectMap<>();
    private static float BASELINE_NUDGE_Y = 3.5f; // tune per font; -1..-2 works well for NotoSymbols 24pt

    public static void setBaselineNudgeY(float pixels) { BASELINE_NUDGE_Y = pixels; }

    private final String cacheKey; // style+glyph+font


    public SymbolButton(final String glyph, final String styleName, final Skin skin, final String tooltipText) {
        super(glyph, skin.get(styleName, TextButtonStyle.class));
        // Build a stable cache key
        final TextButtonStyle s = getStyle();
        final String fontName = s.font != null ? s.font.toString() : "<nofont>";
        this.cacheKey = styleName + "|" + glyph + "|" + fontName;


// inside constructor, after super(...)
        getLabel().setAlignment(Align.center);
        align(Align.center);
// make sure the label won't wrap (it's a single glyph)
        getLabel().setWrap(false);
        // nudge baseline a bit so the glyph sits optically centered
        getLabel().moveBy(0f, BASELINE_NUDGE_Y);

        // TOOLTIP: use sizing tooltip so it wraps horizontally
        if (tooltipText != null && !tooltipText.isEmpty() && skin.has("tooltip-default", TextTooltip.TextTooltipStyle.class)) {
            final SizingTextTooltip tt = new SizingTextTooltip(tooltipText, skin);
            addListener(tt);
        }
    }

    public float clampedSquare(float target) {
        float s = preferredSquareSize();
        return Math.min(s, target);
    }

    /** Return a square size derived from cached atlas+font metrics. */
    public float preferredSquareSize() {
        Float cached = SIZE_CACHE.get(cacheKey);
        if (cached != null) return cached;


        // Use drawables min sizes + label size; prefer current state, fallback to 'up' drawable
        TextButtonStyle st = getStyle();
        Drawable d = st.up != null ? st.up : (st.down != null ? st.down : st.over);
        float minW = d != null ? d.getMinWidth() : 0f;
        float minH = d != null ? d.getMinHeight() : 0f;


        // Ask Scene2D for content-driven size
        pack();
        float w = Math.max(getPrefWidth(), minW);
        float h = Math.max(getPrefHeight(), minH);
        float s = Math.max(w, h);
        s = MathUtils.ceil(s); // snap to pixel grid
        SIZE_CACHE.put(cacheKey, s);
        return s;
    }

    @Override public void layout() {
        super.layout();
        if (BASELINE_NUDGE_Y != 0f) {
            getLabel().setY( (getHeight() - getLabel().getHeight()) * 0.5f + BASELINE_NUDGE_Y );
        } else {
            // ensure true center if nudge is zero
            getLabel().setY( (getHeight() - getLabel().getHeight()) * 0.5f );
        }
        getLabel().setX( (getWidth() - getLabel().getWidth()) * 0.5f );
    }
}