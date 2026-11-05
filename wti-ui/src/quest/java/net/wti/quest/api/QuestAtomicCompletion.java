package net.wti.quest.api;

import xapi.model.api.ModelKey;
import xapi.util.api.SuccessHandler;

/// Optional acknowledged atomic history/live completion; capability is configuration, not health.
public interface QuestAtomicCompletion {
    boolean supportsAtomicCompletion();
    void complete(ModelKey day, ModelKey live, SuccessHandler<QuestCompletionResult> callback);
}
