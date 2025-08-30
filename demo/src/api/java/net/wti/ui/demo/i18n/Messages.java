package net.wti.ui.demo.i18n;

/// Messages:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/08/2025 @ 21:51
public interface Messages {

    default String taskName() {
        return "task"; // I may want to call them "quests" later on
    }
    default String buttonCancel() {
        return "Cancel " + taskName();
    }
    default String buttonDelete() {
        return "Delete " + taskName();
    }
    default String buttonEdit() {
        return "Edit " + taskName();
    }
    default String buttonFinish() {
        return "Finish " + taskName();
    }
    default String buttonStart() {
        return "Start " + taskName();
    }
    default String buttonMaximize() {
        return "Show details";
    }
    default String buttonMinimize() {
        return "Hide details";
    }
    default String buttonReschedule() {
        return "Reschedule " + taskName();
    }

    default String doLater() {
        return "Do later";
    }

    default String skip() {
        return "Skip";
    }

    default String cancel() {
        return "Cancel";
    }

    default String cancelAll() {
        return "Cancel all";
    }

    default String cancelAndDelete() {
        return "Cancel and delete";
    }
}
