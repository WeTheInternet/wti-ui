package net.wti.ui.demo.ui.view;

import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.view.api.IsTaskView;
import net.wti.ui.gdx.theme.GdxTheme;

/// TaskTableComplete:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 20:40
public class TaskTableComplete extends AbstractTaskTable<ModelTaskCompletion> {

    public TaskTableComplete(final GdxTheme theme, final TaskController ctl) {
        super(theme, ctl);
    }

    @Override
    public IsTaskView<ModelTaskCompletion> addTask(final ModelTaskCompletion model) {
        TaskViewComplete view = new TaskViewComplete(model, getSkin(), controller);
        add(view).growX().row();
        return view;
    }
}
