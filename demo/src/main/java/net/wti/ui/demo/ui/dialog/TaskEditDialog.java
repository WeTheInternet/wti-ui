package net.wti.ui.demo.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;

///
/// TaskEditDialog
///
/// Very small dialog that lets the user change name + description of a task.
/// (Future work: recurrence, alarms, etc.)
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 15:57
public class TaskEditDialog extends Dialog {

    private final TextField nameField;
    private final TextArea descArea;
    private final ModelTask task;
    private final TaskController controller;

    public TaskEditDialog(
            Stage stage,
            Skin skin,
            ModelTask task,
            TaskController controller
    ) {
        super("Edit Task", skin, "tool");           /// uses WindowStyle 'tool'

        this.task = task;
        this.controller = controller;
        /// Name field
        nameField = new TextField(task.getName(), skin);
        getContentTable().add(new Label("Name:", skin)).padRight(6);
        getContentTable().add(nameField).width(240).row();

        /// Description field
        descArea = new TextArea(
                task.getDescription() == null ? "" : task.getDescription(), skin);
        descArea.setPrefRows(4);

        getContentTable().add(new Label("Description:", skin)).top().padTop(8).padRight(6);
        getContentTable().add(descArea).width(240).height(96).row();

        /// Buttons
        button("Cancel", false);
        button("Save", true);

        /// Show centred
        show(stage);
        setMovable(true);
        setResizable(false);
        setKeepWithinStage(true);
        setModal(true);

    }

    @Override
    protected void result(final Object obj) {
        if (Boolean.TRUE.equals(obj)) {
            task.setName(nameField.getText());
            task.setDescription(descArea.getText());
            controller.save(task);
        }
        super.result(obj);
    }
}
