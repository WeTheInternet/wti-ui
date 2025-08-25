package net.wti.ui.view.api;

/// CollapsibleView:
///
/// Extension of View for components that support collapsing and expanding details.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 03:10
public interface IsCollapsibleView extends IsView {

    /// Expand the view to show additional details.
    void expand();

    /// Collapse the view to hide additional details.
    void collapse();

    /// @return true if the view is currently in expanded state.
    boolean isExpanded();
}