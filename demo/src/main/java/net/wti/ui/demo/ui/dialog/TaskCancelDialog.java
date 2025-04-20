package net.wti.ui.demo.ui.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;
/// TaskCancelDialog:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 17:10

public class TaskCancelDialog extends Dialog {

    private final TaskController controller;
    private final ModelTask task;
    private final CheckBox forever;
    private final CheckBox next;
    private final CheckBox snooze;
    private final TextField snoozeField;

    public TaskCancelDialog(Stage stage,
                            Skin skin,
                            TaskController controller,
                            net.wti.ui.demo.api.ModelTask task) {

        super("Cancel Task", skin, "tool");
        this.controller = controller;
        this.task = task;
        ButtonGroup<CheckBox> group = new ButtonGroup<>();

        this.forever = new CheckBox("Cancel forever", skin);
        this.next    = new CheckBox("Do next time", skin);
        this.snooze  = new CheckBox("Snooze until...", skin);

        group.add(forever, next, snooze);
        forever.setChecked(true);

        getContentTable().pad(6);
        getContentTable().add(forever).left().row();
        getContentTable().add(next).left().row();
        getContentTable().add(snooze).left().row();

        snoozeField = new TextField("24h", skin);
        getContentTable().add(snoozeField).padLeft(12).width(120).row();

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
            if (forever.isChecked()) controller.cancel(task, TaskController.CancelMode.FOREVER, 0);
            else if (next.isChecked()) controller.cancel(task, TaskController.CancelMode.NEXT, 0);
            else controller.cancel(task, TaskController.CancelMode.SNOOZE,
                        System.currentTimeMillis() + parseHours(snoozeField.getText()));
        }
        super.result(o);
    }

    private long parseHours(String txt) {
        try { return Long.parseLong(txt.replaceAll("[^0-9]", "")) * 3_600_000L; }
        catch (NumberFormatException e) { return 86_400_000L; } // default 24h
    }
}
