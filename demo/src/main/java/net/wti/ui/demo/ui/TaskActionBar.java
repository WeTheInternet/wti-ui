package net.wti.ui.demo.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;

/// TaskActionBar:
///
/// A top-level action bar that reflects the currently selected task
/// and provides controls for finish, defer, and cancel.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/04/2025 @ 20:26
public class TaskActionBar extends Table {

    private final TextButton finishBtn;
    private final TextButton deferBtn;
    private final TextButton cancelBtn;
    private final Label taskName;

    public TaskActionBar(Skin skin) {
        super(skin);
        top().left().padBottom(8).defaults().space(8);

        taskName = new Label("Select a task", skin, "small-white");
        finishBtn = new TextButton("\u2713", skin, "small");
        deferBtn = new TextButton("\u23F3", skin, "small");
        cancelBtn = new TextButton("\u2715", skin, "small");

        add(taskName).expandX().left();
        add(finishBtn).width(32);
        add(deferBtn).width(32);
        add(cancelBtn).width(32);
    }

    public void setTask(ModelTask task, TaskController controller) {
        taskName.setText(task.getName());

        finishBtn.clearListeners();
        deferBtn.clearListeners();
        cancelBtn.clearListeners();

        finishBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.markAsDone(task);
            }
        });

        deferBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.defer(task);
            }
        });

        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.cancel(task);
            }
        });
    }
}
