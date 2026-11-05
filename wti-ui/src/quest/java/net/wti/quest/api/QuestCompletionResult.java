package net.wti.quest.api;

import java.util.Objects;

/// Immutable envelope; acknowledged detached models belong to this operation, never a shared cache.
public final class QuestCompletionResult {
    private final QuestCompletionCommand command;
    private final LiveQuest live;
    private final QuestCompleted history;
    public QuestCompletionResult(final QuestCompletionCommand command, final LiveQuest live, final QuestCompleted history) {
        this.command = Objects.requireNonNull(command, "command");
        this.live = Objects.requireNonNull(live, "live");
        this.history = Objects.requireNonNull(history, "history");
    }
    public QuestCompletionCommand getCommand() { return command; }
    public LiveQuest getLive() { return live; }
    public QuestCompleted getHistory() { return history; }
}
