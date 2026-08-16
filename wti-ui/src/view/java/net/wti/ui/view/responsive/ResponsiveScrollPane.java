package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import net.wti.ui.controls.focus.HoverScrollFocus;
import net.wti.ui.view.api.HasScrollPane;

/// A vertically scrolling overflow boundary for `ResponsivePanel` content.
///
/// The pane keeps its caller-assigned bounds when content is replaced, clips drawing and
/// hit testing through the standard `ScrollPane` viewport, and attaches the repository's
/// hover scroll-focus behavior so wheel input is directed to this pane while hovered.
public class ResponsiveScrollPane extends ScrollPane implements HasScrollPane {

    private ResponsivePanel content;

    public ResponsiveScrollPane(
            final ResponsivePanel content,
            final ScrollPaneStyle style
    ) {
        super(requireContent(content), style);
        this.content = content;
        setFadeScrollBars(false);
        setScrollingDisabled(true, false);
        HoverScrollFocus.attach(this);
    }

    /// Replaces the content widget without changing this pane's assigned bounds.
    public void setContent(final ResponsivePanel content) {
        this.content = requireContent(content);
        setActor(content);
        invalidateHierarchy();
    }

    public ResponsivePanel getContent() {
        return content;
    }

    private static ResponsivePanel requireContent(final ResponsivePanel content) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        return content;
    }

}
