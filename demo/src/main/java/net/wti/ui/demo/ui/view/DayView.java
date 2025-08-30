package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.view.api.IsView;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/// DayView
///
/// Renders a single day’s schedule grouped by hour.
/// - Collapses consecutive empty hours into a single header (e.g. “0–6 am (no items)”)
/// - For non-empty hours, shows an hour header followed by time‑stamped rows
/// - Uses system default time zone and a “4am rule” to bucket very-early items
///
/// Customization:
/// - Provide a rowFactory to render each task; default is a tiny time + title label.
///
/// Created by James X. Nelson (James@WeTheInter.net) and GPT-5 on 30/08/2025 @ 07:11
public class DayView extends Table implements IsView {

    private final Skin skin;
    private final ZoneId zone = ZoneId.systemDefault();
    private final DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("h a");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mm a");

    private LocalDate date;
    private List<ModelTask> tasks = Collections.emptyList();
    private Function<ModelTask, Table> rowFactory;

    // “4am rule”: deadlines before 4am count toward the previous day
    private int rolloverHour = 4;

    // Track whether this day currently renders any items
    private boolean hasItems;

    public DayView(Skin skin, LocalDate date, Iterable<ModelTask> tasks) {
        this(skin, date, tasks, null);
    }

    public DayView(Skin skin, LocalDate date, Iterable<ModelTask> tasks, Function<ModelTask, Table> rowFactory) {
        super(skin);
        this.skin = skin;
        this.date = date;
        setTasks(tasks);
        this.rowFactory = rowFactory != null ? rowFactory : this::defaultRow;

        defaults().growX().pad(2, 6, 2, 6);
        top().left();
    }

    /// Replace the task source for this day (call refresh() afterward).
    public void setTasks(Iterable<ModelTask> tasks) {
        List<ModelTask> list = new ArrayList<>();
        if (tasks != null) for (ModelTask t : tasks) list.add(t);
        this.tasks = list;
    }

    /// Adjust the “rollover” hour used for day bucketing (default 4).
    public void setRolloverHour(int hour0to23) {
        this.rolloverHour = Math.max(0, Math.min(23, hour0to23));
    }

    /// Rebuild the hour-grouped layout.
    @Override
    public void refresh() {
        clearChildren();
        add(headerLabel(dateTitle(date))).left().row();

        // Map tasks to hours for this specific date.
        Map<Integer, List<ModelTask>> byHour = tasks.stream()
                .filter(t -> {
                    Double d = t.getDeadline();
                    if (d == null || d == 0d) return false;
                    LocalDate bucket = bucketDate(d.longValue());
                    return bucket.equals(date);
                })
                .collect(Collectors.groupingBy(t -> {
                    Double d = t.getDeadline();
                    ZonedDateTime zdt = Instant.ofEpochMilli(d.longValue()).atZone(zone);
                    return zdt.getHour();
                }));

        hasItems = !byHour.isEmpty();

        // Walk 0..23 collapsing empty runs.
        int h = 0;
        while (h < 24) {
            if (!byHour.containsKey(h)) {
                int start = h;
                while (h < 24 && !byHour.containsKey(h)) h++;
                int end = h - 1;
                add(emptyHourLabel(collapseTitle(start, end))).left().row();
            } else {
                List<ModelTask> items = sortedByTime(byHour.get(h));
                add(hourLabel(hourFmt(h))).left().row();
                for (ModelTask t : items) {
                    add(rowFactory.apply(t)).left().row();
                }
                h++;
            }
        }
        invalidateHierarchy();
    }

    @Override
    public void dispose() {
        // no-op for now
    }

    // ---- helpers ---------------------------------------------------------

    private LocalDate bucketDate(long millis) {
        ZonedDateTime zdt = Instant.ofEpochMilli(millis).atZone(zone);
        if (zdt.getHour() < rolloverHour) {
            zdt = zdt.minusHours(rolloverHour);
        }
        return zdt.toLocalDate();
    }

    private static List<ModelTask> sortedByTime(List<ModelTask> in) {
        in.sort(Comparator.comparingLong(t -> t.getDeadline().longValue()));
        return in;
    }

    private String dateTitle(LocalDate d) {
        LocalDate today = LocalDate.now(zone);
        if (d.equals(today)) return "Today";
        if (d.equals(today.minusDays(1))) return "Yesterday";
        if (d.equals(today.plusDays(1))) return "Tomorrow";
        return d.getDayOfWeek() + ", " + d;
    }

    private String collapseTitle(int start, int end) {
        if (start > end) return ""; // guard
        if (start == end) return hourFmt(start);
        return hourFmt(start) + " – " + hourFmt(end) + " (no items)";
    }

    private String hourFmt(int hour24) {
        return LocalTime.of(hour24, 0).format(hourFmt).toLowerCase();
    }

    private Label headerLabel(String t) {
        Label.LabelStyle ls = skin.has("task-recurrence-value", Label.LabelStyle.class)
                ? skin.get("task-recurrence-value", Label.LabelStyle.class)
                : skin.get(Label.LabelStyle.class);
        Label lbl = new Label(t, ls);
        lbl.setFontScale(1.05f);
        return lbl;
    }

    private Label hourLabel(String t) {
        Label lbl = new Label(t, skin.get(Label.LabelStyle.class));
        lbl.setColor(0.8f, 0.8f, 1f, 1f);
        lbl.setFontScale(0.98f);
        return lbl;
    }

    private Label emptyHourLabel(String t) {
        Label lbl = new Label(t, skin.get(Label.LabelStyle.class));
        lbl.setColor(0.7f, 0.7f, 0.8f, 1f);
        lbl.setFontScale(0.92f);
        return lbl;
    }

    private Table defaultRow(ModelTask t) {
        // Minimal row: “h:mm — Task Name”
        Double d = t.getDeadline();
        String time = d == null ? "" : Instant.ofEpochMilli(d.longValue()).atZone(zone).toLocalTime().format(timeFmt);
        String title = t.getName() == null ? "" : t.getName();

        Table row = new Table(skin);
        row.defaults().pad(1, 4, 1, 4).left();
        row.add(new Label(time, skin.get(Label.LabelStyle.class))).left().padRight(8);
        row.add(new Label(title, skin.get(Label.LabelStyle.class))).left().growX();
        return row;
    }

    /// @return true if this day currently contains any renderable items.
    public boolean hasItems() {
        return hasItems;
    }

}
