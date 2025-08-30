package net.wti.tasks.event;

import net.wti.ui.demo.api.ModelTask;

///
/// TaskFinishedEvent:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 27/08/2025 @ 07:03
public final class TaskFinishedEvent extends TaskEvent {
    public final ModelTask task;

    public TaskFinishedEvent(ModelTask task) {
        this.task = task;
    }
}
