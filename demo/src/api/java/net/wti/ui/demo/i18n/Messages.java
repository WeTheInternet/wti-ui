package net.wti.ui.demo.i18n;

/// Messages:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/08/2025 @ 21:51
public interface Messages {

    default String buttonCancel() {
        return "Cancel task";
    }
    default String buttonEdit() {
        return "Edit task";
    }
    default String buttonFinish() {
        return "Finish task";
    }
    default String buttonMaximize() {
        return "Show task details";
    }
    default String buttonMinimize() {
        return "Hide task details";
    }
    default String buttonReschedule() {
        return "Reschedule task";
    }
}
