package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;

import java.util.LinkedHashMap;
import java.util.Map;

/// A responsive, ordered Scene2D accordion with caller-owned headers and bodies.
///
/// Sections remain in one Table hierarchy. Closed bodies stay attached to the accordion,
/// but are invisible and touch-disabled, so normal Table layout gives them no effective
/// height and Scene2D hit testing cannot reach them. Bodies are never cloned or recreated.
public class ResponsiveAccordion extends Table {

    private final Map<String, Section> sections = new LinkedHashMap<String, Section>();
    private final Skin skin;
    private String headerStyle = "emphasis";
    private boolean exclusive;

    public ResponsiveAccordion() {
        this(null);
    }

    public ResponsiveAccordion(final Skin skin) {
        super(skin);
        this.skin = skin;
        top().left();
        defaults().growX();
    }

    /// Adds a section with an independent caller-owned header and body actor.
    public void addSection(
            final String key,
            final Actor headerActor,
            final Actor bodyActor
    ) {
        addSection(key, headerActor, bodyActor, null);
    }

    /// Adds a section with a separate trailing header action actor.
    public void addSection(
            final String key,
            final Actor headerActor,
            final Actor bodyActor,
            final Actor headerAction
    ) {
        requireKey(key);
        if (headerActor == null) {
            throw new IllegalArgumentException("headerActor must not be null");
        }
        if (bodyActor == null) {
            throw new IllegalArgumentException("bodyActor must not be null");
        }
        if (sections.containsKey(key)) {
            throw new IllegalArgumentException("section key already exists: " + key);
        }
        final Section section = new Section(key, headerActor, bodyActor, headerAction);
        headerActor.addListener(new ClickListener() {
            @Override
            public void clicked(final InputEvent event, final float x, final float y) {
                toggle(key);
            }
        });
        sections.put(key, section);
        rebuild();
    }

    /// Convenience overload for theme-backed accordions retaining the legacy API shape.
    public void addSection(final String key, final Actor bodyActor) {
        if (skin == null) {
            throw new IllegalStateException("a Skin is required for generated headers");
        }
        final TextButton header = new TextButton(key, skin, headerStyle);
        header.getLabel().setAlignment(Align.center);
        addSection(key, header, bodyActor);
    }

    /// Adds or replaces a trailing action without making it part of the toggle actor.
    public void setHeaderAction(final String key, final Actor headerAction) {
        section(key).headerAction = headerAction;
        rebuild();
    }

    /// Enables exclusive mode, which matches the legacy `openOnly` behavior.
    public void setExclusive(final boolean exclusive) {
        this.exclusive = exclusive;
        if (exclusive) {
            String firstOpen = null;
            for (final Section section : sections.values()) {
                if (section.expanded) {
                    if (firstOpen == null) {
                        firstOpen = section.key;
                    } else {
                        section.expanded = false;
                    }
                }
            }
        }
        rebuild();
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void expand(final String key) {
        final Section target = section(key);
        if (exclusive) {
            openOnly(key);
            return;
        }
        if (!target.expanded) {
            target.expanded = true;
            rebuild();
        }
    }

    public void collapse(final String key) {
        final Section target = section(key);
        if (target.expanded) {
            target.expanded = false;
            rebuild();
        }
    }

    public void toggle(final String key) {
        final Section target = section(key);
        if (target.expanded) {
            collapse(key);
        } else {
            expand(key);
        }
    }

    public boolean isExpanded(final String key) {
        return section(key).expanded;
    }

    public void expandAll() {
        for (final Section section : sections.values()) {
            section.expanded = true;
        }
        rebuild();
    }

    public void collapseAll() {
        for (final Section section : sections.values()) {
            section.expanded = false;
        }
        rebuild();
    }

    /// Legacy-compatible exclusive operation.
    public void openOnly(final String key) {
        section(key);
        for (final Section section : sections.values()) {
            section.expanded = section.key.equals(key);
        }
        rebuild();
    }

    public int getSectionCount() {
        return sections.size();
    }

    /// Changes generated TextButton headers without replacing caller-provided actors.
    public void setHeaderStyle(final String styleName) {
        if (styleName == null) {
            throw new IllegalArgumentException("styleName must not be null");
        }
        headerStyle = styleName;
        if (skin != null) {
            for (final Section section : sections.values()) {
                if (section.headerActor instanceof TextButton) {
                    ((TextButton) section.headerActor).setStyle(
                            skin.get(styleName, TextButton.TextButtonStyle.class)
                    );
                }
            }
        }
        invalidateHierarchy();
    }

    private void rebuild() {
        clearChildren();
        for (final Section section : sections.values()) {
            final Table headerRow = new Table();
            headerRow.top().left();
            headerRow.add(section.headerActor).growX().fillX().minWidth(0f);
            if (section.headerAction != null) {
                headerRow.add(section.headerAction).right();
            }
            add(headerRow).growX().fillX().minWidth(0f).row();

            section.bodyActor.setVisible(section.expanded);
            section.bodyActor.setTouchable(
                    section.expanded ? Touchable.enabled : Touchable.disabled
            );
            final Cell<Actor> bodyCell = add(section.bodyActor)
                    .growX()
                    .fillX()
                    .minWidth(0f)
                    .minHeight(0f);
            if (!section.expanded) {
                // Invisible actors can still contribute preferred size through their cell;
                // explicitly collapse the cell so the Table has no hidden body gap.
                bodyCell.height(0f).minHeight(0f).maxHeight(0f);
            }
            bodyCell.row();
        }
        invalidateHierarchy();
    }

    private Section section(final String key) {
        requireKey(key);
        final Section section = sections.get(key);
        if (section == null) {
            throw new IllegalArgumentException("unknown section key: " + key);
        }
        return section;
    }

    private static void requireKey(final String key) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("key must not be empty");
        }
    }

    private static final class Section {
        private final String key;
        private final Actor headerActor;
        private final Actor bodyActor;
        private Actor headerAction;
        private boolean expanded;

        private Section(
                final String key,
                final Actor headerActor,
                final Actor bodyActor,
                final Actor headerAction
        ) {
            this.key = key;
            this.headerActor = headerActor;
            this.bodyActor = bodyActor;
            this.headerAction = headerAction;
        }
    }
}
