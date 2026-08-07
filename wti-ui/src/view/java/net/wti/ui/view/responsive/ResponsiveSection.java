package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;

/// A titled, ordered collection of `LeadingContentRow` actors.
///
/// The section owns heading-to-row and row-to-row spacing. It does not know the semantic
/// meaning of the supplied actors and performs no drawing outside normal Scene2D actor
/// layout.
public class ResponsiveSection extends Table {

    private final Actor headingActor;
    private final Array<LeadingContentRow> rows = new Array<LeadingContentRow>();
    private ResponsivePanelStyle style;

    public ResponsiveSection(
            final Actor headingActor,
            final ResponsivePanelStyle style
    ) {
        if (headingActor == null) {
            throw new IllegalArgumentException("headingActor must not be null");
        }
        this.headingActor = headingActor;
        setStyle(style);
    }

    public LeadingContentRow addRow(final Actor leadingActor, final Actor bodyActor) {
        final LeadingContentRow row = new LeadingContentRow(leadingActor, bodyActor, style);
        addRow(row);
        return row;
    }

    public LeadingContentRow addWrappedLabelRow(
            final Actor leadingActor,
            final CharSequence text,
            final Label.LabelStyle labelStyle
    ) {
        final LeadingContentRow row = LeadingContentRow.wrappedLabel(
                leadingActor,
                text,
                labelStyle,
                style
        );
        addRow(row);
        return row;
    }

    public void addRow(final LeadingContentRow row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        row.setStyle(style);
        rows.add(row);
        rebuild();
    }

    public void clearRows() {
        rows.clear();
        rebuild();
    }

    public Actor getHeadingActor() {
        return headingActor;
    }

    public Array<LeadingContentRow> getContentRows() {
        return new Array<LeadingContentRow>(rows);
    }

    public final void setStyle(final ResponsivePanelStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style must not be null");
        }
        this.style = style;
        for (final LeadingContentRow row : rows) {
            row.setStyle(style);
        }
        rebuild();
    }

    public ResponsivePanelStyle getStyle() {
        return style;
    }

    private void rebuild() {
        clearChildren();
        top().left();
        add(headingActor).growX().fillX().top().left().minWidth(0f);
        if (rows.size == 0) {
            invalidateHierarchy();
            return;
        }
        row();
        for (int i = 0; i < rows.size; i++) {
            final float topPad = i == 0 ? style.headingGap : style.rowGap;
            add(rows.get(i))
                    .growX()
                    .fillX()
                    .top()
                    .minWidth(0f)
                    .padTop(nonNegative(topPad));
            if (i + 1 < rows.size) {
                row();
            }
        }
        invalidateHierarchy();
    }

    private static float nonNegative(final float value) {
        return Math.max(0f, value);
    }
}
