package net.wti.ui.view.api;

/// IsView:
///
/// Base interface for any UI component with basic lifecycle operations.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 03:09
public interface IsView {

    /// Redraw or refresh the component's contents (e.g., after model changes).
    void refresh();

    /// Dispose of any resources (listeners, textures, etc.) when no longer needed.
    void dispose();
}