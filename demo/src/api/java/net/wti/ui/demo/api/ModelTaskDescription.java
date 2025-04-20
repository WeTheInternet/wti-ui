package net.wti.ui.demo.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;

/// ModelTaskCompletion
///
/// A snapshot of a task completion event.
/// Used for rendering the "done" list.
/// Stores completion timestamp and summary text.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/04/2025 @ 20:46
@IsModel(
        modelType = ModelTaskDescription.MODEL_TASK_DESCRIPTION,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ModelTaskDescription extends BasicModelTask<ModelTaskDescription> {

    String MODEL_TASK_DESCRIPTION = "tskDsc";

    @Override
    default ModelTaskDescription self() {
        return this;
    }
}
