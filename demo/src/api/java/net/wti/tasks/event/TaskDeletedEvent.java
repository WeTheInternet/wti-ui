package net.wti.tasks.event;

import xapi.model.api.ModelKey;

///
/// TaskDeletedEvent:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 27/08/2025 @ 07:03
public final class TaskDeletedEvent extends TaskEvent {
    public final ModelKey taskId;

    public TaskDeletedEvent(ModelKey taskId) {
        this.taskId = taskId;
    }
}
