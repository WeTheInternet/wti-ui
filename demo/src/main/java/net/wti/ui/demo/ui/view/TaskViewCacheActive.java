package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.tasks.api.view.AbstractTaskViewCache;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;

///
/// TaskViewCacheActive:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 28/08/2025 @ 05:20
public class TaskViewCacheActive extends AbstractTaskViewCache<ModelTask, TaskViewActive> {

    private final TaskController controller;

    public TaskViewCacheActive(Skin skin, TaskController controller) {
        super(skin, controller.getIndex());
        this.controller = controller;
    }

    @Override
    protected TaskViewActive create(ModelTask model) {
        return new TaskViewActive(model, skin, controller);
    }
}
