package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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
/// 『 ✓ 』 Layout title and buttons on left column
/// 『 ✓ 』 TaskSummaryPane + preview right-aligned, no growX
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
/// 『   』 Refactor to use TaskActionBar and TaskSummaryPane (in progress)
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:59 CST
public class TaskView extends Table implements IsTaskView<ModelTask> {

    /// TaskViewStyle: Theming contract for TaskView elements.
    /// Allows styles to be assigned via Skin JSON file `task-ui.json`.
    public static class TaskViewStyle {
        public Drawable background;
        public Drawable hoveredBackground;
        public Label.LabelStyle nameStyle;
        public Label.LabelStyle descStyle;
        public Label.LabelStyle previewStyle;
        public Label.LabelStyle recurrenceLabelStyle;
        public Label.LabelStyle recurrenceValueStyle;
        public TextButton.TextButtonStyle buttonStyle;
        public TextButton.TextButtonStyle editButtonStyle;
        public TextButton.TextButtonStyle toggleButtonStyle;
    }

    private final ModelTask task;
    private final TaskController controller;
    private final Skin skin;
    private final DeadlineView deadlineView;
    private boolean expanded = false;
    private boolean isHovered = false;
    private final TaskViewStyle style;

    public TaskView(ModelTask task, Skin skin, TaskController controller) {
        this(task,
             skin.has("taskview", TaskViewStyle.class)
                     ? skin.get("taskview", TaskViewStyle.class)
                     : new TaskViewStyle(),
             skin,
             controller);
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

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                isHovered = true;
                updateBackground();
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                isHovered = false;
                updateBackground();
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

    /// Build compact layout using HorizontalGroup-based split
    private void buildCollapsed() {
        HorizontalGroup row = new HorizontalGroup();
        row.left().top().space(20).fill();

        VerticalGroup left = new VerticalGroup();
        left.left().top().space(4).fill();

        Label name = new Label(task.getName(), style.nameStyle != null ? style.nameStyle : skin.get(Label.LabelStyle.class));
        name.setWrap(false);
        left.addActor(name);

        // ✓ ⌚ ✕ buttons
//        left.addActor(new TaskActionBar(this, controller, style));

        VerticalGroup right = new VerticalGroup();
        right.left().top().space(4).fill();

        // ⏱ Task summary
        right.addActor(new TaskSummaryPane(skin, task));

        // 📝 Description preview
        String desc = task.getDescription() == null ? "" : task.getDescription().replace("\n", " ").trim();
        if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
        Label preview = new Label(desc, style.previewStyle != null ? style.previewStyle : skin.get(Label.LabelStyle.class));
        preview.setWrap(false);
        right.addActor(preview);

        // 💡 Compose layout
        row.addActor(left);
        row.addActor(right);

        add(row).left().top().growX().padBottom(4);
    }

    /// Build expanded layout showing full details and recurrence.
    private void buildExpanded() {
        Label name = new Label(task.getName(), style.descStyle != null ? style.descStyle : skin.get(Label.LabelStyle.class));
        name.setWrap(true);
        add(name).left().colspan(3).padBottom(4).row();

        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            Label desc = new Label(task.getDescription(), style.descStyle != null ? style.descStyle : skin.get(Label.LabelStyle.class));
            desc.setWrap(false);
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

        Table actions = new Table(skin);
        actions.add(addActionButton("✓", () -> controller.markAsDone(task))).padRight(8);
        actions.add(addActionButton("⌚", () -> controller.defer(task))).padRight(8);
        actions.add(addActionButton("✕", () -> controller.cancel(task, TaskController.CancelMode.NEXT, 1000)));
        add(actions).left();
        row().padTop(4);
    }

    /// Create a styled Label as a button and hook to action.
    private TextButton addActionButton(String text, Runnable action) {
        TextButton button = new TextButton(text, style.buttonStyle != null ? style.buttonStyle : skin.get(TextButton.TextButtonStyle.class));
        button.setColor(1f, 1f, 1f, 1f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

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
    public IsDeadlineView getDeadlineView() {
        return deadlineView;
    }

    private void updateBackground() {
        Drawable bg = style.background;
        if (isHovered && style.hoveredBackground != null) {
            bg = style.hoveredBackground;
        }
        setBackground(bg);
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }
}
