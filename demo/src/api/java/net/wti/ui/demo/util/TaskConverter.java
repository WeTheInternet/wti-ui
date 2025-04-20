package net.wti.ui.demo.util;

import net.wti.ui.demo.api.CompletionStatus;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskCompletion;
import xapi.model.X_Model;

/// TaskConverter
///
/// Utility class that transforms a live `ModelTask` into a
/// persisted `ModelTaskCompletion` snapshot.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 20:49
public final class TaskConverter {

    public static ModelTaskCompletion toCompletion(ModelTask src,
                                                   CompletionStatus status)
    {
        ModelTaskCompletion dst = X_Model.create(ModelTaskCompletion.class);

        dst.setName(src.getName())
                .setDescription(src.getDescription())
                .setPriority(src.getPriority())
                .setNote("")                        // no note for now
                .setCompleted(System.currentTimeMillis())
                .setSourceTask(src.getKey())
                .setStatus(status);

        return dst;
    }

    private TaskConverter() {}
}
