package net.wti.ui.demo.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.gdx.theme.GdxTheme;

/// TaskTable:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 21:59
public class TaskTableOld extends ScrollPane {

    private final GdxTheme theme;
    private final Table body;

    public TaskTableOld(final GdxTheme theme) {
        this(theme, new Table());
    }
    public TaskTableOld(final GdxTheme theme, final Table body) {
        super(body, theme.getSkin(), "no-bg");
        this.theme = theme;
        this.body = body;
        body.align(Align.center);
        setScrollingDisabled(true, false);
//        setFadeScrollBars(false);


    }

    public TaskViewOld addTask(ModelTask task) {
        final TaskViewOld view = new TaskViewOld(task, theme.getSkin());
        view.addToTable(body);
        return view;
    }

    public void setHeader(final String header) {
        body.add(new Label(header, theme.getSkin())).colspan(getTaskColumnWidth());
    }

    /**
     * @return The number of columns added for each task.
     * <p>
     * Update this whenever adding more elements in {@link TaskViewOld#getNumColumns()}}
     */
    int getTaskColumnWidth() {
        return 3;
    }

}
