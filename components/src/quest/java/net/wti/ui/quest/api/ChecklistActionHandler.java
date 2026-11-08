package net.wti.ui.quest.api;

/// Intent events from the pure checklist view; the application owns persistence and UI dispatch.
public interface ChecklistActionHandler {
    void startRequested();
    void completeRequested(String encodedLiveKey);
}
