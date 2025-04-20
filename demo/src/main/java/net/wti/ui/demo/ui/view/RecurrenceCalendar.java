package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.demo.api.Schedule;

/// RecurrenceCalendar
///
/// A compact visual day-of-week or monthly layout depending on recurrence types
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/04/2025 @ 20:02
public class RecurrenceCalendar extends Table {

    public RecurrenceCalendar(Schedule schedule) {
        left().top().pad(2);
        add(new Label("Calendar...", getSkin())).left(); // Placeholder until real impl
    }
}