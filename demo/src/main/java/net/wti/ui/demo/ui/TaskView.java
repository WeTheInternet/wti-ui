package net.wti.ui.demo.ui;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.wti.ui.api.IsDeadlineView;
import net.wti.ui.demo.api.ModelRecurrence;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.RecurrenceUnit;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.view.api.IsTaskView;
import net.wti.ui.view.DeadlineView;
import xapi.model.api.ModelList;

/// # TaskView
///
/// Visual component for representing a task in compact or expanded format.
/// Used in task tables to render the current state of each ModelTask.
///
/// ### ✅ Status Checklist (as of 2025-04-17 @ 03:33 CST)
///
/// ## 📦 Compact View
/// 『 ✓ 』 Render task name using prominent font
/// 『 ✓ 』 Display truncated task description preview
/// 『 ✓ 』 Show deadline view aligned to the right
///
/// ## 🔄 Expandable Layout
/// 『 ✓ 』 Toggle between collapsed/expanded on row click
/// 『 ✓ 』 Show full description and recurrence details
/// 『 ✓ 』 Display deadline label and action buttons
///
/// ## 🧠 Controller Integration
/// 『 ✓ 』 Button interactions route to TaskController
/// 『   』 Inline editing support (planned)
///
/// ## ✨ UX and Theme Polish
/// 『 ✓ 』 Hover/Click affordance support (via style config)
/// 『 ✓ 』 Use `TaskViewStyle` to theme labels, paddings, background
/// 『 ✓ 』 Extract reusable DeadlineView integration
/// 『   』 Animations and transitions (future)
/// 『   』 Editable recurrence control (future)
/// 『   』 More elegant empty-state handling (future)
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:59 CST
public class TaskView extends Table implements IsTaskView<ModelTask> {

    /// TaskViewStyle: Theming contract for TaskView elements.
    /// Allows styles to be assigned via Skin JSON file `task-ui.json`.
    public static class TaskViewStyle {
        public Drawable background;
        public Label.LabelStyle nameStyle;
        public Label.LabelStyle descStyle;
        public Label.LabelStyle previewStyle;
        public Label.LabelStyle recurrenceLabelStyle;
        public Label.LabelStyle recurrenceValueStyle;
        public Label.LabelStyle buttonStyle;
    }

    private final ModelTask task;
    private final TaskController controller;
    private final Skin skin;
    private final DeadlineView deadlineView;
    private boolean expanded = false;
    private final TaskViewStyle style;

    public TaskView(ModelTask task, Skin skin, TaskController controller) {
        this(task, skin.has("taskview", TaskViewStyle.class) ? skin.get("taskview", TaskViewStyle.class) : new TaskViewStyle(), skin, controller);
    }

    public TaskView(ModelTask task, TaskViewStyle style, Skin skin, TaskController controller) {
        super(skin);
        this.task = task;
        this.controller = controller;
        this.skin = skin;
        this.style = style;
        this.deadlineView = new DeadlineView(task.getDeadline(), skin, task.getAlarmMinutes());

        if (style.background != null) {
            setBackground(style.background);
        }

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleExpanded();
            }
        });

        rebuild();
    }

    /// Clears and rebuilds the current layout depending on expanded state.
    private void rebuild() {
        clear();
        pad(8).top().left();
        if (expanded) {
            buildExpanded();
        } else {
            buildCollapsed();
        }
    }

    /// Build compact task layout with name, preview, and deadline.
    private void buildCollapsed() {
        Label name = new Label(task.getName(), style.nameStyle != null ? style.nameStyle : skin.get(Label.LabelStyle.class));
        name.setWrap(true);

        String desc = task.getDescription();
        if (desc == null) desc = "";
        desc = desc.replace("\n", " ").replaceAll(" +", " ").trim();
        int maxLen = 100;
        if (desc.length() > maxLen) {
            desc = desc.substring(0, maxLen - 3) + "...";
        }
        Label preview = new Label(desc, style.previewStyle != null ? style.previewStyle : skin.get(Label.LabelStyle.class));
        preview.setWrap(true);

        add(name).left().growX().colspan(2).padBottom(4).row();
        add(preview).left().growX().padRight(10);
        add(deadlineView).right().growX().padLeft(20).row();
    }

    /// Build expanded layout showing full details and recurrence.
    private void buildExpanded() {
        Label name = new Label(task.getName(), style.descStyle != null ? style.descStyle : skin.get(Label.LabelStyle.class));
        name.setWrap(true);
        add(name).left().colspan(3).padBottom(4).row();

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            Label desc = new Label(task.getDescription(), style.descStyle != null ? style.descStyle : skin.get(Label.LabelStyle.class));
            desc.setWrap(true);
            add(desc).left().colspan(3).padBottom(8).row();
        }

        add(new Label("Due:", style.descStyle != null ? style.descStyle : skin.get(Label.LabelStyle.class))).right().padRight(6);
        add(deadlineView).left().colspan(2).padBottom(6).row();

        ModelList<ModelRecurrence> recurrences = task.getRecurrence();
        if (recurrences != null && !recurrences.isEmpty()) {
            add(new Label("Recurs:", style.recurrenceLabelStyle != null ? style.recurrenceLabelStyle : skin.get(Label.LabelStyle.class))).top().right().padRight(6);
            Table recurTable = new Table(skin);
            for (ModelRecurrence recur : recurrences) {
                String label;
                if (recur.getUnit() == RecurrenceUnit.ONCE) {
                    label = "Once";
                } else {
                    label = recur.dayOfWeek().name() + " @ " + recur.hour() + ":" + String.format("%02d", recur.minute());
                }
                recurTable.add(new Label(label, style.recurrenceValueStyle != null ? style.recurrenceValueStyle : skin.get(Label.LabelStyle.class))).left().row();
            }
            add(recurTable).left().colspan(2).padBottom(10).row();
        }

        addActionButton("✓", () -> controller.markAsDone(task));
        addActionButton("→", () -> controller.defer(task));
        addActionButton("✕", () -> controller.cancel(task));
        row().padTop(4);
    }

    /// Create a styled Label as a button and hook to action.
    private void addActionButton(String text, Runnable action) {
        Label button = new Label(text, style.buttonStyle != null ? style.buttonStyle : skin.get(Label.LabelStyle.class));
        button.setColor(1f, 1f, 1f, 1f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        add(button).left().padRight(8);
    }

    /// markdown
    /// Switch between compact and expanded view states.
    public void toggleExpanded() {
        expanded = !expanded;
        rebuild();
    }

    @Override
    public void expand() {
        if (!expanded) toggleExpanded();
    }

    @Override
    public void collapse() {
        if (expanded) toggleExpanded();
    }

    @Override
    public void rerender() {
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
}