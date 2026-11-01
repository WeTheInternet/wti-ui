package net.wti.quest.api;

import java.util.Objects;

/// Captured wire identity. No client clock, actor or candidate model image is accepted.
public final class QuestCompletionCommand {
    public static final String PROTOCOL = "quest-complete-v1";
    private final String protocol, operationId, dayKey, liveKey;

    public QuestCompletionCommand(final String protocol, final String operationId, final String dayKey, final String liveKey) {
        if (!PROTOCOL.equals(protocol) || operationId == null || !operationId.matches("[A-Za-z0-9._~-]{1,128}")) {
            throw new IllegalArgumentException("Invalid completion protocol or operation identity");
        }
        if (dayKey == null || dayKey.isEmpty() || liveKey == null || liveKey.isEmpty()) {
            throw new IllegalArgumentException("Full completion keys are required");
        }
        this.protocol = protocol; this.operationId = operationId; this.dayKey = dayKey; this.liveKey = liveKey;
    }
    public String getProtocol() { return protocol; }
    public String getOperationId() { return operationId; }
    public String getDayKey() { return dayKey; }
    public String getLiveKey() { return liveKey; }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof QuestCompletionCommand)) return false;
        final QuestCompletionCommand c = (QuestCompletionCommand) other;
        return protocol.equals(c.protocol) && operationId.equals(c.operationId)
                && dayKey.equals(c.dayKey) && liveKey.equals(c.liveKey);
    }
    @Override public int hashCode() { return Objects.hash(protocol, operationId, dayKey, liveKey); }
}
