package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;

/// A padded panel which reflows titled sections into one or two equal-width columns.
///
/// Callers own this actor's actual bounds. The panel derives its composition from the
/// current width during validation: two columns are used only when both columns meet the
/// configured minimum after panel padding and the column gap. Bounds are never rewritten
/// when sections are replaced. Add or replace content through this class so normal
/// `invalidateHierarchy()` and `validate()` propagation remains authoritative.
public class ResponsivePanel extends Table {

    private final Array<ResponsiveSection> sections = new Array<ResponsiveSection>();
    private ResponsivePanelStyle style;
    private int columnCount;
    private boolean compositionDirty = true;

    public ResponsivePanel(final ResponsivePanelStyle style) {
        top().left();
        setStyle(style);
    }

    public ResponsiveSection addSection(final Actor headingActor) {
        final ResponsiveSection section = new ResponsiveSection(headingActor, style);
        addSection(section);
        return section;
    }

    public void addSection(final ResponsiveSection section) {
        if (section == null) {
            throw new IllegalArgumentException("section must not be null");
        }
        section.setStyle(style);
        sections.add(section);
        compositionDirty = true;
        invalidateHierarchy();
    }

    /// Replaces page/content actors without altering the explicitly assigned panel bounds.
    public void setSections(final Iterable<ResponsiveSection> replacement) {
        sections.clear();
        if (replacement != null) {
            for (final ResponsiveSection section : replacement) {
                if (section == null) {
                    throw new IllegalArgumentException("sections must not contain null");
                }
                section.setStyle(style);
                sections.add(section);
            }
        }
        compositionDirty = true;
        invalidateHierarchy();
    }

    public void clearSections() {
        sections.clear();
        compositionDirty = true;
        invalidateHierarchy();
    }

    public Array<ResponsiveSection> getSections() {
        return new Array<ResponsiveSection>(sections);
    }

    /// Returns the column count selected during the latest layout validation.
    public int getColumnCount() {
        return columnCount;
    }

    public final void setStyle(final ResponsivePanelStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style must not be null");
        }
        this.style = style;
        background(style.background);
        pad(
                nonNegative(style.panelPadTop),
                nonNegative(style.panelPadLeft),
                nonNegative(style.panelPadBottom),
                nonNegative(style.panelPadRight)
        );
        for (final ResponsiveSection section : sections) {
            section.setStyle(style);
        }
        compositionDirty = true;
        invalidateHierarchy();
    }

    public ResponsivePanelStyle getStyle() {
        return style;
    }

    @Override
    public void layout() {
        final int desiredColumns = chooseColumnCount();
        if (compositionDirty || columnCount != desiredColumns) {
            rebuild(desiredColumns);
        }
        super.layout();
    }

    private int chooseColumnCount() {
        if (sections.size < 2) {
            return sections.size == 0 ? 0 : 1;
        }
        final float contentWidth = Math.max(
                0f,
                getWidth() - getPadLeft() - getPadRight()
        );
        final float gap = nonNegative(style.columnGap);
        final float widthPerColumn = Math.max(0f, contentWidth - gap) / 2f;
        return widthPerColumn >= nonNegative(style.minimumColumnWidth) ? 2 : 1;
    }

    private void rebuild(final int desiredColumns) {
        clearChildren();
        columnCount = desiredColumns;
        compositionDirty = false;
        if (desiredColumns == 0) {
            return;
        }
        for (int i = 0; i < sections.size; i++) {
            final boolean firstColumn = i % desiredColumns == 0;
            final boolean firstRow = i < desiredColumns;
            final float halfColumnGap = nonNegative(style.columnGap) / 2f;
            add(sections.get(i))
                    .growX()
                    .fillX()
                    .top()
                    .minWidth(0f)
                    .padTop(firstRow ? 0f : nonNegative(style.sectionGap))
                    .padLeft(desiredColumns == 2 && !firstColumn ? halfColumnGap : 0f)
                    .padRight(desiredColumns == 2 && firstColumn ? halfColumnGap : 0f);
            final boolean rowComplete = i % desiredColumns == desiredColumns - 1;
            final boolean finalSection = i + 1 == sections.size;
            if (rowComplete || finalSection) {
                if (finalSection && !rowComplete && desiredColumns == 2) {
                    add().growX().minWidth(0f);
                }
                if (!finalSection) {
                    row();
                }
            }
        }
    }

    private static float nonNegative(final float value) {
        return Math.max(0f, value);
    }
}
