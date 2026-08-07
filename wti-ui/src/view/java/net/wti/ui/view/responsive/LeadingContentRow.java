package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

/// A responsive row containing a leading actor and a body actor which owns the remaining
/// horizontal space.
///
/// The body cell has a zero minimum width and grows to the row's remaining width. Layout
/// actors such as wrapping `Label` instances therefore report their constrained preferred
/// height to `Table`; the row height becomes the taller child plus configured padding.
/// Drawing and hit testing use the resulting actor bounds—there is no parallel geometry
/// model for callers to maintain.
public class LeadingContentRow extends Table {

    private final Actor leadingActor;
    private final Actor bodyActor;
    private ResponsivePanelStyle style;

    public LeadingContentRow(
            final Actor leadingActor,
            final Actor bodyActor,
            final ResponsivePanelStyle style
    ) {
        if (leadingActor == null) {
            throw new IllegalArgumentException("leadingActor must not be null");
        }
        if (bodyActor == null) {
            throw new IllegalArgumentException("bodyActor must not be null");
        }
        this.leadingActor = leadingActor;
        this.bodyActor = bodyActor;
        setStyle(style);
    }

    /// Convenience factory for the common leading-actor plus wrapping-text case.
    public static LeadingContentRow wrappedLabel(
            final Actor leadingActor,
            final CharSequence text,
            final Label.LabelStyle labelStyle,
            final ResponsivePanelStyle style
    ) {
        final Label body = new Label(text, labelStyle);
        body.setWrap(true);
        body.setAlignment(Align.left, Align.top);
        return new LeadingContentRow(leadingActor, body, style);
    }

    public final void setStyle(final ResponsivePanelStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style must not be null");
        }
        this.style = style;
        rebuild();
    }

    public ResponsivePanelStyle getStyle() {
        return style;
    }

    public Actor getLeadingActor() {
        return leadingActor;
    }

    public Actor getBodyActor() {
        return bodyActor;
    }

    private void rebuild() {
        clearChildren();
        pad(
                nonNegative(style.rowPadTop),
                nonNegative(style.rowPadLeft),
                nonNegative(style.rowPadBottom),
                nonNegative(style.rowPadRight)
        );
        add(leadingActor).top().left();
        add(bodyActor)
                .growX()
                .fillX()
                .minWidth(0f)
                .top()
                .padLeft(nonNegative(style.leadingBodyGap));
        invalidateHierarchy();
    }

    private static float nonNegative(final float value) {
        return Math.max(0f, value);
    }
}
