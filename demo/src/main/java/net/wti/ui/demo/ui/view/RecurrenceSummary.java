package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.components.SizingTextTooltip;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.Schedule;
import net.wti.ui.view.DeadlineView;
import xapi.time.X_Time;

/// RecurrenceSummary
///
/// Minimal, low-noise summary:
/// - Once-only with no deadline → render nothing (zero size)
/// - One-shot with deadline     → tiny countdown; tooltip shows full timestamp
/// - Recurring task             → “time until next due”; tooltip “Every <rule> …”
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/04/2025 @ 19:58
public class RecurrenceSummary extends Table {

    private final ModelTask task;
    private final Schedule schedule;
    private final Skin skin;
    private Label label;
    private boolean visibleContent;

    public RecurrenceSummary(ModelTask task, Skin skin, Schedule schedule) {
        this.task = task;
        this.skin = skin;
        this.schedule = schedule;

        top().left().pad(0);
        defaults().pad(0).space(4);

        rebuild();
    }

    private void rebuild() {
        clearChildren();
        visibleContent = false;

        final boolean onceOnly = schedule.isOnceOnly();
        final Double rawDeadline = task.getDeadline();
        final Double deadline = normalizeDeadline(rawDeadline);


        // Style: prefer a compact style; fallback to default
        final Label.LabelStyle tiny = skin.has("task-preview", Label.LabelStyle.class)
                ? skin.get("task-preview", Label.LabelStyle.class)
                : skin.get(Label.LabelStyle.class);

        if (onceOnly && deadline == null) {
            // Render nothing at all.
            setVisible(false);
            return;
        }

        if (deadline != null) {
            // One-shot with a concrete deadline: show countdown with full timestamp tooltip.
            label = new DeadlineView(deadline, skin, task.getAlarmMinutes());
            label.setStyle(tiny);
            label.setFontScale(0.8f);
            add(label).left();
            final String tipText = fullTimestamp(deadline);
            if (!tipText.isEmpty()) {
                SizingTextTooltip tip = new SizingTextTooltip(tipText, skin);
                label.addListener(tip);
            }
            setVisible(true);
            visibleContent = true;
            return;
        }

        // Recurring: show time-until-next-due (if available) with a human message tooltip.
        String hover = schedule.getShortDescription(); // e.g. "Every day at 5pm"
        if (hover == null || hover.isEmpty()) {
            hover = "Recurring";
        }

        // If Schedule exposes a next due instant, prefer that; otherwise, fall back to a neutral label.
        String compact = computeNextDueSummary();
        if (compact == null) {
            compact = hover; // fallback to short text
        }

        label = new Label(compact, tiny);
        label.setFontScale(0.9f);
        add(label).left();

        SizingTextTooltip tip = new SizingTextTooltip(hover, skin);
        label.addListener(tip);

        setVisible(true);
        visibleContent = true;
    }

    private static String fullTimestamp(Double millis) {
        if (millis == null || millis == 0d) {
            return "";
        }
        // Render an absolute timestamp for tooltip; format as you prefer
        long when = millis.longValue();
        return X_Time.timestampHuman(when); // assumes you have a formatter; otherwise substitute your own
    }

    private static Double normalizeDeadline(Double deadline) {
        return (deadline == null || deadline == 0d) ? null : deadline;
    }


    private String computeNextDueSummary() {
        // If Schedule has a next-occurrence API, compute “Due in …”
        // Otherwise return null and we’ll fall back to hover text.
        try {
            Long next = schedule.getNextDueMillis(); // if available
            if (next != null) {
                double delta = next - X_Time.nowMillis();
                return "Due in " + X_Time.print(delta);
            }
        } catch (Throwable ignore) {
            // No next-due API available; fall through to null
        }
        return null;
    }

    @Override public float getPrefWidth() {
        return isVisible() && visibleContent ? super.getPrefWidth() : 0f;
    }
    @Override public float getPrefHeight() {
        return isVisible() && visibleContent ? super.getPrefHeight() : 0f;
    }
}
