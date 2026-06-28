package net.wti.ui.api;

///
/// IsExpandable:
///
/// An interface for UI elements that are expandable/collapsible
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 03:50
public interface IsExpandable {

    /// Toggle expanded/collapsed and rebuild layout
    void toggleExpanded();

    /// Expand this task view (e.g. show recurrence, notes, etc.)
    void expand();

    /// Collapse this task view (e.g. hide extra UI)
    void collapse();

    /// Allow querying the expanded state
    boolean isExpanded();

}
