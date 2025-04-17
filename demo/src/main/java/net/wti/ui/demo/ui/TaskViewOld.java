package net.wti.ui.demo.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.gdx.theme.raeleus.sgx.MenuButton;
import net.wti.gdx.theme.raeleus.sgx.MenuButtonGroup;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.common.TaskFactory;
import net.wti.ui.view.DeadlineView;
import xapi.fu.In1;

/// TaskView:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 21:16
public class TaskViewOld {

    private final ModelTask task;
    private final Label label;
    private final Label description;
    private final DeadlineView deadline;
    private final MenuButtonGroup<MenuButton<String>> menuButtonGroup;
    private final Table menuBar;
    private final Skin skin;
    private Cell<Actor> labelRow;
    private Cell<Actor> buttonRow;

    public TaskViewOld(final ModelTask task, final Skin skin) {
        this.task = task;
        this.skin = skin;
        label = new Label(task.getName(), skin);
        description = new Label(task.getDescription(), skin);

        final Double nextTime = TaskFactory.nextTime(task);
        deadline = new DeadlineView(nextTime, skin, task.getAlarmMinutes());
        deadline.setDeadline(nextTime);
        menuButtonGroup = new MenuButtonGroup<>();
        menuBar = new Table(skin);
    }

    public Label getLabel() {
        return label;
    }

    public Label getDescription() {
        return description;
    }

    /**
     * @param table The table to which we should add our fields
     */
    public void addToTable(final Table table) {
        labelRow = table.row();
        // if you add() more items, make sure to update #getNumColumns!
        table.add(getLabel(), getDescription(), deadline);
        buttonRow = table.row();

        menuBar.setBackground("file-menu-bar");


        final MenuButton<String> finishMenuButton = new MenuButton<>("Finish", skin);
        finishMenuButton.getLabelCell().padLeft(5.0f).padRight(5.0f);
        menuBar.add(finishMenuButton);
        menuButtonGroup.add(finishMenuButton);
        finishMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent ce, Actor actor) {
                if (finishMenuButton.isChecked()) {
                    // TODO: call some method on controller to "mark this task as complete"
                    System.out.println("Finished: " + task.getKey() + " event: " + ce.getTarget() + " ; actor" + actor);
                }
            }
        });

        final MenuButton<String> cancelMenuButton = new MenuButton<>("Cancel", skin);
        cancelMenuButton.getLabelCell().padLeft(5.0f).padRight(5.0f);
        menuBar.add(cancelMenuButton);
        menuButtonGroup.add(cancelMenuButton);
        cancelMenuButton.setItems("Do Later", "Delete Task", "Finish then Delete");
        cancelMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent ce, Actor actor) {
                System.out.println("Canceled: " + cancelMenuButton.getSelectedIndex() + " " + cancelMenuButton.getSelectedItem());
            }
        });

        table.add(menuBar).colspan(getNumColumns()).growX();
    }

    /**
     * If you update this number, make sure to also update {@link TaskTableOld#getTaskColumnWidth()}
     *
     * @return The number of columns used by the task view
     */
    public int getNumColumns() {
        return 3;
    }

    public float getHeight() {
        if (buttonRow == null) {
            return -1;
        }
        return buttonRow.getActorHeight() + labelRow.getActorHeight();
    }

    public void addButton(final String title, final In1<ModelTask> onClicked) {
        final MenuButton<String> fileMenuButton = new MenuButton<>(title, skin);
        fileMenuButton.getLabelCell().padLeft(5.0f).padRight(5.0f);
        menuBar.add(fileMenuButton);
        fileMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(final ChangeEvent event, final Actor actor) {
                onClicked.in(task);
            }
        });

    }
}
