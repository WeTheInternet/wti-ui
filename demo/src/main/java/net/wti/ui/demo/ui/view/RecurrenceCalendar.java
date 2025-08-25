package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.wti.ui.demo.api.DayOfWeek;
import net.wti.ui.demo.api.ModelRecurrence;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.RecurrenceUnit;

import java.util.*;

import static net.wti.ui.demo.api.ModelRecurrence.MINUTES_PER_DAY;

/// # RecurrenceCalendar
///
/// A visual calendar grid for displaying task recurrence rules.
///
/// ✅ **Features**
/// - Renders weekly, biweekly, triweekly, monthly, and yearly recurrence
/// - Uses square cells per day-of-week or day-of-month
/// - Colors and interaction styles from JSON skin
/// - Dynamically updates when `ModelTask` changes
///
/// ⚙️ **Skin Styling**
/// - Style class: `RecurrenceCalendarStyle`
/// - Supports: `cellStyle`, `activeStyle`, `hoverStyle`, `selectedStyle`
///
/// 🔄 **Usage**
/// ```java
/// RecurrenceCalendar calendar = new RecurrenceCalendar(skin);
/// calendar.update(task);  // re-render from task
/// ```
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 21/04/2025 @ 03:33 CST
public class RecurrenceCalendar extends Table {

    /// Style for calendar cells
    public static class RecurrenceCalendarStyle {
        public Label.LabelStyle cellStyle;
        public Label.LabelStyle activeStyle;
        public Label.LabelStyle hoverStyle;
        public Label.LabelStyle selectedStyle;
    }

    private final Skin skin;
    private final RecurrenceCalendarStyle style;
    private final Map<String, Label> cells = new LinkedHashMap<>();
    private final List<Actor> calendarRows = new ArrayList<>();

    public RecurrenceCalendar(Skin skin) {
        this.skin = skin;
        this.style = skin.has("recurrence-calendar", RecurrenceCalendarStyle.class)
                ? skin.get("recurrence-calendar", RecurrenceCalendarStyle.class)
                : new RecurrenceCalendarStyle();
    }

    /// Updates this calendar to reflect the recurrence entries of the given task.
    public void update(ModelTask task) {
        clear();
        calendarRows.clear();
        cells.clear();

        if (task == null || task.getRecurrence() == null || task.getRecurrence().isEmpty()) {
            return;
        }

        EnumMap<RecurrenceUnit, List<ModelRecurrence>> grouped = new EnumMap<>(RecurrenceUnit.class);
        for (ModelRecurrence r : task.getRecurrence()) {
            grouped.computeIfAbsent(r.getUnit(), k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<RecurrenceUnit, List<ModelRecurrence>> entry : grouped.entrySet()) {
            switch (entry.getKey()) {
                case WEEKLY:
                case BIWEEKLY:
                case TRIWEEKLY:
                    renderWeekGrid(entry.getValue(), entry.getKey());
                    break;
                case MONTHLY:
                    renderMonthGrid(entry.getValue());
                    break;
                case YEARLY:
                    renderYearText(entry.getValue());
                    break;
                case DAILY:
                    renderDailyText(entry.getValue());
                    break;
                case ONCE:
                    // ignore; handled in RecurrenceSummary
                    break;
            }
        }

        for (Actor row : calendarRows) {
            add(row).left().row();
        }
    }

    /// Render days of week (7-cell rows)
    private void renderWeekGrid(List<ModelRecurrence> recurs, RecurrenceUnit unit) {
        Table row = new Table();
        for (DayOfWeek dow : DayOfWeek.values()) {
            boolean active = recurs.stream().anyMatch(r -> r.dayOfWeek() == dow);
            row.add(makeCell(dow.name().substring(0, 1), active)).padRight(4);
        }
        calendarRows.add(row);
    }

    /// Render monthly view (row of 1–31 days)
    private void renderMonthGrid(List<ModelRecurrence> recurs) {
        Table row = new Table();
        Set<Integer> days = new TreeSet<>();
        for (ModelRecurrence r : recurs) {
            days.add((int)(r.getValue() / MINUTES_PER_DAY));
        }
        boolean hadActive = false;
        for (int i = 1; i <= 31; i++) {
            boolean active = days.contains(i);
            if (active) {
                hadActive = true;
            }
            row.add(makeCell(String.valueOf(i), active)).padRight(2).padBottom(2);
            if (i % 7 == 0) {
                if (hadActive) {
                    hadActive = false;
                    calendarRows.add(row);
                }
                row = new Table();
            }
        }
        if (hadActive && row.getChildren().size > 0) {
            calendarRows.add(row);
        }
    }

    /// Render text line for yearly entries
    private void renderYearText(List<ModelRecurrence> recurs) {
        Table yearRow = new Table();
        for (ModelRecurrence r : recurs) {
            int minutes = (int)r.getValue();
            int day = minutes / MINUTES_PER_DAY;
            int hour = (minutes % MINUTES_PER_DAY) / 60;
            int minute = minutes % 60;
            String text = "Yearly on " + TimeUtils.formatDate(day) + " at " + TimeUtils.timeString(hour, minute);
            yearRow.add(new Label(text, style.activeStyle)).left().row();
        }
        calendarRows.add(yearRow);
    }

    /// Render special marker for daily
    private void renderDailyText(List<ModelRecurrence> recurs) {
        for (ModelRecurrence r : recurs) {
            int hour = r.hour();
            int minute = r.minute();
            String time = TimeUtils.timeString(hour, minute);
            calendarRows.add(new Label("Every day at " + time, style.activeStyle));
        }
    }

    /// Make a single cell with optional style
    private Label makeCell(String text, boolean active) {
        Label label = new Label(text, active ? style.activeStyle : style.cellStyle);
        label.setName("recurrence-cell-" + text.toLowerCase());
        label.addListener(new HoverStyleListener(label, active ? style.hoverStyle : style.cellStyle));
        return label;
    }

    /// Simple hover effect
    private static class HoverStyleListener extends ClickListener {
        private final Label label;
        private final Label.LabelStyle originalStyle;

        public HoverStyleListener(Label label, Label.LabelStyle hoverStyle) {
            this.label = label;
            this.originalStyle = label.getStyle();
            label.addListener(this);
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            label.setStyle(originalStyle);
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            label.setStyle(originalStyle);
        }
    }

    /// Internal helper for time formatting
    private static class TimeUtils {

        private static final String[] MONTHS = {
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };

        public static String formatDate(int dayOfYear) {
            int[] daysInMonth = {31,28,31,30,31,30,31,31,30,31,30,31};
            int month = 0;
            while (month < 11 && dayOfYear >= daysInMonth[month]) {
                dayOfYear -= daysInMonth[month++];
            }
            return MONTHS[month] + " " + (dayOfYear + 1);
        }

        public static String timeString(int hour, int minute) {
            return (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute;
        }
    }
}