package net.wti.ui.demo.ui;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.api.IsDeadlineView;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.view.api.IsTaskView;
import net.wti.ui.view.DeadlineView;

/// TaskViewExpandable
///
/// A self-contained, toggleable visual block to render and interact with a single task.
/// Designed for compact display by default, with optional expansion to reveal additional fields.
///
/// ## Roadmap Checklist
///
/// ### 1. 📦 Compact Display Mode
/// 『 ✓ 』 Task name label, dynamic deadline
/// 『 ✓ 』 Icon-style buttons: finish ( ✓ ), defer ( → ), cancel ( ✕ )
///
/// ### 2. 🔄 Expandable State
/// 『 ✓ 』 Show task description if present
/// 『 ✓ 』 Show editable recurrence area placeholder
/// 『 ✓ 』 Allow toggle between compact / expanded
///
/// ### 3. ⚖️ Integration with Controller
/// 『 ✓ 』 All buttons dispatch to TaskController
/// 『   』 Add editable mode toggle (future)
///
/// ### 4. ✨ Polish and UX Features
/// 『   』 Keyboard nav / focus hooks
/// 『   』 Click-to-expand on row
/// 『   』 Rollover highlight
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:50 CST
public class TaskViewExpandable extends Table implements IsTaskView<ModelTask> {

    private final ModelTask task;
    private final Skin skin;
    private final TaskController controller;
    private final IsDeadlineView<Actor> deadlineView;
    private boolean expanded = false;
    private Label nameLabel;
    private Label descLabel;

    public TaskViewExpandable(ModelTask task, Skin skin, TaskController controller) {
        super(skin);
        this.task = task;
        this.skin = skin;
        this.controller = controller;
        this.deadlineView = new DeadlineView(task.getDeadline(), skin, task.getAlarmMinutes());

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                toggleExpanded();
                return true;
            }
        });

        rebuild();
    }

    /// Clears and redraws the table based on `expanded` state
    private void rebuild() {
        clear();
        if (expanded) {
            buildExpanded();
        } else {
            buildCompact();
        }
    }

    /// Renders a minimal compact task block (name, deadline, buttons)
    private void buildCompact() {
        nameLabel = new Label(task.getName(), skin);
        add(nameLabel).left().growX();
        add(deadlineView.uiSpecific()).right().padRight(10);
        addButtons();
    }

    /// Renders a full detailed block with name, desc, deadline, recurrence UI
    private void buildExpanded() {
        nameLabel = new Label(task.getName(), skin);
        add(nameLabel).left().growX().colspan(3).row();

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            descLabel = new Label(task.getDescription(), skin);
            add(descLabel).left().colspan(3).row();
        }

        add("Due: ").right();
        add(deadlineView.uiSpecific()).left().colspan(2).row();

        add("Recurs: ").right();
        add("[Recurring UI Here]").left().colspan(2).row();

        addButtons();
    }

    /// Adds the interactive buttons with event handlers
    private void addButtons() {
        Label finish = new Label("\u2713", skin);
        finish.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                controller.markAsDone(task);
                return true;
            }
        });

        Label defer = new Label("\u2192", skin);
        defer.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                controller.defer(task);
                return true;
            }
        });

        Label cancel = new Label("\u2715", skin);
        cancel.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                controller.cancel(task);
                return true;
            }
        });

        add(finish).pad(2).left();
        add(defer).pad(2).left();
        add(cancel).pad(2).left();
    }

    /// Toggle expanded/collapsed and rebuild layout
    public void toggleExpanded() {
        this.expanded = !expanded;
        rebuild();
    }

    @Override
    public ModelTask getTask() {
        return task;
    }

    @Override
    public IsDeadlineView<Actor> getDeadlineView() {
        return deadlineView;
    }

    @Override
    public void collapse() {
        if (expanded) {
            expanded = false;
            rebuild();
        }
    }

    @Override
    public void expand() {
        if (!expanded) {
            expanded = true;
            rebuild();
        }
    }

    @Override
    public void rerender() {
        rebuild();
    }
}