package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.TaskActionBar;
import net.wti.ui.view.DeadlineView;

/// RecurrenceSummary
///
/// A summary row for recurrence:
/// - Shows deadline if present
/// - Infinity symbol if ONCE and no deadline
/// - Tiny RecurrenceCalendar otherwise
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/04/2025 @ 19:58
public class RecurrenceSummary extends Table {

    public RecurrenceSummary(ModelTask task, Skin skin, Schedule schedule) {
        top().left().pad(4);

        if (task.getDeadline() != null) {
            add(new DeadlineView(task.getDeadline(), skin, task.getAlarmMinutes())).left();
        } else if (schedule.isOnceOnly()) {
            add(new Label(TaskActionBar.GLYPH_INFINITY, skin)).left(); // Or ∞, ♾, ⧞, etc.
        } else {
            add(new RecurrenceCalendar(schedule)).left();
        }
    }
}