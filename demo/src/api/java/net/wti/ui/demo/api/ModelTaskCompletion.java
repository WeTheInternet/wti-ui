package net.wti.ui.demo.api;

/// ModelTaskCompletion:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/04/2025 @ 20:46

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;

/// ModelTaskCompletion
///
/// A snapshot of a task completion event.
/// Used for rendering the "done" list.
/// Stores completion timestamp and summary text.
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
    String getSourceKey();
    ModelTaskCompletion setSourceKey(String key);

    /// Optional: status of the completion (e.g., completed, needs approval)
    CompletionStatus getStatus();
    ModelTaskCompletion setStatus(CompletionStatus status);

}
