package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import net.wti.ui.demo.api.ModelTaskDescription;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.gdx.theme.GdxTheme;

/// TaskTableDefinitions:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 19:51
public class TaskTableDefinitions extends AbstractTaskTable<ModelTaskDescription, TaskViewDescription> {

    public TaskTableDefinitions(final GdxTheme theme, final TaskController ctl) {
        super(theme, ctl);
    }

    @Override
    public TaskViewDescription addTask(final ModelTaskDescription model) {
        TaskViewDescription view = new TaskViewDescription(model, getSkin(), controller);
        final Cell<TaskViewDescription> cell = add(view);
        cell.growX().row();
        view.setCell(cell);
        return view;
    }
}
