package net.wti.tasks.event;

import net.wti.ui.demo.api.ModelTask;

///
/// TaskUpdatedEvent:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 27/08/2025 @ 07:03
public final class TaskUpdatedEvent extends TaskEvent {
    public final ModelTask task;

    public TaskUpdatedEvent(ModelTask task) {
        this.task = task;
    }
}
