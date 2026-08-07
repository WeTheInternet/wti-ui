package net.wti.ui.view.responsive;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import net.wti.ui.controls.focus.HoverScrollFocus;

/// A vertically scrolling viewport for arbitrary Scene2D content.
///
/// This is the generic counterpart to `ResponsiveScrollPane`, whose typed
/// `ResponsivePanel` API remains available for existing consumers. The pane owns only
/// scrolling and clipping; callers continue to own its assigned bounds.
public class ResponsiveContentScrollPane extends ScrollPane {

    private Actor content;

    public ResponsiveContentScrollPane(
            final Actor content,
            final ScrollPaneStyle style
    ) {
        super(requireContent(content), requireStyle(style));
        this.content = content;
        setFadeScrollBars(false);
        setScrollingDisabled(true, false);
        HoverScrollFocus.attach(this);
    }

    /// Replaces content without changing this pane's caller-assigned bounds.
    public void setContent(final Actor content) {
        this.content = requireContent(content);
        setActor(content);
        invalidateHierarchy();
    }

    /// Returns the exact actor supplied by the caller.
    public Actor getContent() {
        return content;
    }

    private static Actor requireContent(final Actor content) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        return content;
    }

    private static ScrollPaneStyle requireStyle(final ScrollPaneStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style must not be null");
        }
        return style;
    }
}
