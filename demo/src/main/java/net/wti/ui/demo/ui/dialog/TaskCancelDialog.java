package net.wti.ui.demo.ui.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;

import static net.wti.ui.demo.common.DemoConstants.MESSAGES;

/// TaskCancelDialog:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 17:10

public class TaskCancelDialog extends Dialog {

    private final TaskController controller;
    private final ModelTask task;
    private final CheckBox cancel;
    private final CheckBox skip;
    private final CheckBox doLater;
    private final CheckBox cancelAndDelete;
    private final TextField snoozeField;

    public TaskCancelDialog(Stage stage,
                            Skin skin,
                            TaskController controller,
                            net.wti.ui.demo.api.ModelTask task) {
        super(MESSAGES.buttonCancel(), skin, "tool");
        this.controller = controller;
        this.task = task;
        ButtonGroup<CheckBox> group = new ButtonGroup<>();

        this.doLater = new CheckBox(MESSAGES.doLater(), skin);
        group.add(doLater);

        if (task.hasRecurrence()) {
            this.cancel = new CheckBox(MESSAGES.cancelAll(), skin);
            this.skip = new CheckBox(MESSAGES.skip(), skin);
            group.add(this.skip);
        } else {
            this.cancel = new CheckBox(MESSAGES.cancel(), skin);
            this.skip = null;
        }
        group.add(cancel);

        this.cancelAndDelete = new CheckBox(MESSAGES.cancelAndDelete(), skin);
        group.add(cancelAndDelete);
        doLater.setChecked(true);

        getContentTable().pad(6);
        getContentTable().add(doLater).left().row();
        // TODO: a time selector field; integer number w/ minute|hour|day units
        snoozeField = new TextField("24h", skin);
        getContentTable().add(snoozeField).padLeft(12).width(120).row();

        if (skip != null) {
            getContentTable().add(skip).left().row();
        }
        getContentTable().add(cancel).left().row();
        getContentTable().add(cancelAndDelete).left().row();


        button("OK", true);
        button("Cancel", false);

        /** show near mouse */
        Vector2 pos = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        setPosition(pos.x, pos.y);
        show(stage);
    }

    @Override
    protected void result(final Object o) {
        if (Boolean.TRUE.equals(o)) {
            if (cancel.isChecked() || cancelAndDelete.isChecked()) {
                controller.cancel(task, TaskController.CancelMode.FOREVER, 0);
                if (cancelAndDelete.isChecked()) {
                    controller.deleteTask(task);
                }
            } else if (skip != null && skip.isChecked()) {
                controller.cancel(task, TaskController.CancelMode.NEXT, 0);
            } else {
                controller.cancel(task, TaskController.CancelMode.SNOOZE,
                        System.currentTimeMillis() + parseHours(snoozeField.getText()));
            }
        }
        super.result(o);
    }

    private long parseHours(String txt) {
        try { return Long.parseLong(txt.replaceAll("[^0-9]", "")) * 3_600_000L; }
        catch (NumberFormatException e) { return 86_400_000L; } // default 24h
    }
}
