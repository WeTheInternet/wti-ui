package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.ModelKey;

/// ModelTaskCompletion
///
/// A snapshot of a task completion event.
/// Used for rendering the "done" list.
/// Stores completion timestamp and summary text.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/04/2025 @ 20:46
@IsModel(
        modelType = ModelTaskCompletion.MODEL_TASK_COMPLETION,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ModelTaskCompletion extends BasicModelTask<ModelTaskCompletion> {

    String MODEL_TASK_COMPLETION = "tskCmp";

    /// Timestamp (epoch millis) of when this task was completed
    long getCompleted();
    ModelTaskCompletion setCompleted(long completed);

    /// Optional extra comment about this completion
    String getNote();
    ModelTaskCompletion setNote(String note);

    /// Original key of the ModelTask this completion came from
    ModelKey getSourceTask();
    ModelTaskCompletion setSourceTask(ModelKey key);

    /// The key of the user who approved the task, if it was approved
    ModelKey getApprovedBy();
    ModelTaskCompletion setApprovedBy(ModelKey key);

    /// The time this task was approved, or null if not approved
    Double getApprovalTime();
    ModelTaskCompletion setApprovalTime(Double time);

    /// Optional: status of the completion (e.g., completed, cancelled, needs approval, rejected)
    CompletionStatus getStatus();
    ModelTaskCompletion setStatus(CompletionStatus status);

    @Override
    default ModelTaskCompletion self() {
        return this;
    }
}
