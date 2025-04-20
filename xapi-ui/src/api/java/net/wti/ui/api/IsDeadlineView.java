package net.wti.ui.api;

/// IsDeadlineView
///
/// Describes the logic contract for views that visually represent a task deadline.
/// UI classes (like `DeadlineView`) may implement this interface for generic handling.
///
/// Responsibilities:
/// 『 ✓ 』 Report whether a deadline exists
/// 『 ✓ 』 Check if the deadline has passed or is near
/// 『 ✓ 』 Allow dynamic deadline updates
/// 『 ✓ 』 Abstract deadline-aware logic from UI rendering
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:10:28 CST
public interface IsDeadlineView {

    /// @return true if this view is tracking a non-null deadline
    boolean hasDeadline();

    /// @return true if the deadline has passed (now > deadline)
    boolean isPastDeadline();

    /// @return true if the deadline is within the alarm threshold
    boolean isNearDeadline();

    /// Assigns a new deadline value (epoch millis)
    void setDeadline(double deadline);

    Double getDeadline();

    Integer getAlarmMinutes();

}
