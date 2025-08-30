package net.wti.tasks.event;

import net.wti.ui.demo.api.ModelTask;

///
/// TaskStartedEvent:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 27/08/2025 @ 07:03
public final class TaskStartedEvent extends TaskEvent {
    public final ModelTask task;

    public TaskStartedEvent(ModelTask task) {
        this.task = task;
    }
}
