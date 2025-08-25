package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.Schedule;

/// RecurrenceView
///
/// A composite view to visualize recurrence rules from a ModelTask.
/// - In collapsed mode: shows a RecurrenceSummary (DeadlineView, Calendar dots, or Infinity)
/// - In expanded mode: shows full descriptions of recurrence schedule
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/04/2025 @ 19:57
public class RecurrenceView extends Table {

    private final ModelTask task;
    private final Skin skin;
    private final Schedule schedule;
    private final RecurrenceCalendar calendar;
    private boolean expanded = false;

    private final RecurrenceSummary summary;
    private final Table detail = new Table(); // reused between collapse/expand

    public RecurrenceView(ModelTask task, Skin skin) {
        this.task = task;
        this.skin = skin;
        this.schedule = new Schedule(task);

        this.summary = new RecurrenceSummary(task, skin, schedule);
        add(summary).left().row();

        calendar = new RecurrenceCalendar(skin);
        add(calendar).padTop(6).left().row();

        rebuild();

        task.onChange("updated", (before, after) -> {
            schedule.invalidate();
            rebuild();
        });
    }

    private void rebuild() {
        detail.clearChildren();
        calendar.update(task);
        if (expanded) {
            // show expanded calendar view
            for (String line : schedule.getLongDescriptions()) {
                detail.add(new Label(line, skin)).left().row();
            }
            add(detail).padTop(8).left().row();
        } else {
            // show collapsed calendar view
        }

    }

    public void expand() {
        if (!expanded) {
            expanded = true;
            rebuild();
        }
    }

    public void collapse() {
        if (expanded) {
            expanded = false;
            rebuild();
        }
    }

    public boolean isExpanded() {
        return expanded;
    }
}
