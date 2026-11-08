package net.wti.quest.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Immutable rendering values for one definition and its direct checklist members.
/// Encoded keys retain full namespace, ancestry and key type; no mutable models escape.
public final class ChecklistSnapshot {
    private final Entry root;
    private final List<Entry> children;
    private final boolean canStart;

    public ChecklistSnapshot(final Entry root, final List<Entry> children, final boolean canStart) {
        this.root = root;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
        this.canStart = canStart;
    }

    public Entry getRoot() { return root; }
    public List<Entry> getChildren() { return children; }
    public boolean canStart() { return canStart; }

    /// One immutable row. A null live key means no owner/day was supplied.
    public static final class Entry {
        private final String definitionKey, liveKey, title, description;
        private final int quantity;
        private final QuestStatus status;
        private final boolean started, canComplete;

        public Entry(final String definitionKey, final String liveKey, final String title,
                     final String description, final int quantity, final QuestStatus status,
                     final boolean started, final boolean canComplete) {
            this.definitionKey = definitionKey;
            this.liveKey = liveKey;
            this.title = title;
            this.description = description;
            this.quantity = quantity;
            this.status = status;
            this.started = started;
            this.canComplete = canComplete;
        }

        public String getDefinitionKey() { return definitionKey; }
        public String getLiveKey() { return liveKey; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getQuantity() { return quantity; }
        public QuestStatus getStatus() { return status; }
        public boolean isStarted() { return started; }
        public boolean isComplete() { return status == QuestStatus.FINISHED; }
        public boolean canComplete() { return canComplete; }
    }
}
