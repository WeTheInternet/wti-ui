package net.wti.ui.demo.ui.view;

import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.view.api.IsTaskView;
import net.wti.ui.gdx.theme.GdxTheme;

/// TaskTableActive:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 20:21
public class TaskTableActive extends AbstractTaskTable<ModelTask> {

    public TaskTableActive(final GdxTheme theme, final TaskController ctl) {
        super(theme, ctl);
    }

    @Override
    public IsTaskView<ModelTask> addTask(final ModelTask model) {
        TaskViewActive view = new TaskViewActive(model, getSkin(), controller);
        add(view).growX().row();
        return view;
    }

}
