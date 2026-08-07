package net.wti.ui.gdx.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.gdx.theme.GdxTheme;
import net.wti.ui.view.responsive.ResponsiveAccordion;

/// Compatibility adapter for the legacy theme-backed, exclusive accordion.
///
/// New reusable code should use `ResponsiveAccordion` directly. This adapter preserves the
/// old `GdxTheme`, `addSection(key, body)`, and `openOnly(key)` integration used by
/// `OldTodayView`.
public class AccordionPane extends ResponsiveAccordion {

    public AccordionPane(final GdxTheme theme) {
        super(requireTheme(theme).getSkin());
        setExclusive(true);
    }

    @Override
    public void addSection(final String key, final Actor content) {
        super.addSection(key, content);
    }

    private static GdxTheme requireTheme(final GdxTheme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        return theme;
    }
}
