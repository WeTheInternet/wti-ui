package net.wti.tasks.event;

import net.wti.ui.demo.api.ModelTask;

///
/// TaskCreatedEvent:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 27/08/2025 @ 07:03
public final class TaskCreatedEvent extends TaskEvent {
    public final ModelTask task;

    public TaskCreatedEvent(ModelTask task) {
        this.task = task;
    }
}
