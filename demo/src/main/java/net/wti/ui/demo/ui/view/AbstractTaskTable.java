package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.demo.api.BasicModelTask;
import net.wti.ui.demo.common.DemoApp;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.view.api.IsTaskView;
import net.wti.ui.gdx.theme.GdxTheme;

/// AbstractTaskTable
///
/// Responsibilities:
/// 『 ✓ 』 Display a titled list of tasks
/// 『 ✓ 』 Grow-fill layout for responsive display
/// 『   』 Supports dynamic list sorting or filtering
/// 『   』 Visual style improvements based on task status
/// 『   』 Inline edit buttons or quick completion
/// 『   』 Future: sorting and filtering by deadline, status, etc.
/// 『   』 Future: animations for task movement
/// 『   』 Future: Drag-and-drop task reordering
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 19:46
public abstract class AbstractTaskTable<M extends BasicModelTask<M>> extends Table {

    protected final TaskController controller;

    protected AbstractTaskTable(GdxTheme theme, TaskController ctl) {
        super(theme.getSkin());
        this.controller = ctl;
        top().center().padTop(4).padBottom(4);
    }

    /** add a task and return its rendered view */
    public abstract IsTaskView<M> addTask(M model);

    public void setHeader(final String header) {
        add(new Label(header, getSkin())).colspan(getTaskColumnWidth()).row();
    }

    protected final int getTaskColumnWidth() {
        return 3;
    }
}
