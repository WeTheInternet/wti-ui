package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Null;
import net.wti.ui.demo.api.BasicModelTask;
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
public abstract class AbstractTaskTable<M extends BasicModelTask<M>, V extends Actor & IsTaskView<M>> extends Table {

    protected final TaskController controller;
    protected final TaskCache<M, V> cache;

    protected AbstractTaskTable(GdxTheme theme, TaskController ctl) {
        super(theme.getSkin());
        this.controller = ctl;
        top().center().padTop(4).padBottom(4);
        defaults().spaceBottom(8);
        cache = new TaskCache<>();
    }

    /** add a task and return its rendered view */
    public abstract V addTask(M model);

    public void setHeader(final String header) {
        add(new Label(header, getSkin())).colspan(getTaskColumnWidth()).row();
    }

    protected final int getTaskColumnWidth() {
        return 3;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Actor> Cell<T> add(@Null T actor) {
        final Cell<T> cell = super.add(actor);
        if (actor instanceof IsTaskView) {
            cache.add((V)actor, (Cell)cell);
        }
        return cell;
    }
}
