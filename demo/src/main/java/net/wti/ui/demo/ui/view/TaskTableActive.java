package net.wti.ui.demo.ui.view;

import net.wti.tasks.event.TaskCreatedEvent;
import net.wti.tasks.event.TaskLoadedEvent;
import net.wti.tasks.event.TaskUpdatedEvent;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.gdx.theme.GdxTheme;

/// TaskTableActive:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 20:21
public class TaskTableActive extends AbstractTaskTable<ModelTask, TaskViewActive> {

    public TaskTableActive(final GdxTheme theme, final TaskController controller) {
        super(theme, controller);
        controller.getIndex().subscribe(evt -> {
            if (evt instanceof TaskCreatedEvent) {
                addTask(((TaskCreatedEvent) evt).task);
            } else if (evt instanceof TaskLoadedEvent) {
                addTask(((TaskLoadedEvent) evt).task);
            } else if (evt instanceof TaskUpdatedEvent) {

            }
        });
    }

    @Override
    public TaskViewActive addTask(final ModelTask model) {
        TaskViewActive view = new TaskViewActive(model, getSkin(), controller);
        add(view).growX().row();
        return view;
    }

}
