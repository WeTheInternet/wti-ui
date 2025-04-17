package net.wti.ui.demo.ui;


import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.ui.view.TaskCompletionView;
import net.wti.ui.demo.view.api.IsTaskView;
import net.wti.ui.gdx.theme.GdxTheme;

/// TaskTable
///
/// A scrollable list container to show a vertical list of task-like views.
/// Supports both active and completed tasks.
///
/// Responsibilities:
/// 『 ✓ 』 Display a titled list of tasks
/// 『 ✓ 』 Handle both `ModelTask` and `ModelTaskCompletion` rendering
/// 『 ✓ 』 Grow-fill layout for responsive display
/// 『   』 Supports dynamic list sorting or filtering
/// 『   』 Visual style improvements for completed items
/// 『   』 Inline edit buttons or quick completion
/// 『   』 Future: sorting and filtering by deadline, status, etc.
/// 『   』 Future: animations for task movement
/// 『   』 Future: Drag-and-drop task reordering
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:10 CST
public class TaskTable extends ScrollPane {

    private final Table body;
    private final GdxTheme theme;
    private final TaskController controller;

    public TaskTable(GdxTheme theme, TaskController controller) {
        this(theme, new Table(), controller);
    }

    public TaskTable(GdxTheme theme, Table body, TaskController controller) {
        super(body, theme.getSkin(), "no-bg");
        this.theme = theme;
        this.controller = controller;
        this.body = body;
        body.align(Align.center);
        setScrollingDisabled(true, false);
    }

    /// Adds a header label above the task list
    public void setHeader(String header) {
        body.add(new Label(header, theme.getSkin())).colspan(getTaskColumnWidth()).row();
    }

    /// Adds a task item to the table in active mode (TaskViewExpandable)
    public IsTaskView<?> addTask(ModelTask task) {
        final TaskView view = new TaskView(task, theme.getSkin(), controller);
        body.add(view).expandX().fillX().pad(5).row();
        return view;
    }

    /// Adds a completed task item to the table (TaskCompletionView)
    public IsTaskView<?> addTask(ModelTaskCompletion completion) {
        final TaskCompletionView view = new TaskCompletionView(completion, theme.getSkin());
        body.add(view).expandX().fillX().pad(5).row();
        return view;
    }

    /// @return The number of columns used for layout by each task view
    public int getTaskColumnWidth() {
        return 3;
    }
}
