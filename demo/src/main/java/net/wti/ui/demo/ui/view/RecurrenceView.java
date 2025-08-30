package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.ui.components.SizingTextTooltip;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.Schedule;
import net.wti.ui.view.panes.ClipGroup;
import xapi.time.X_Time;

/// RecurrenceView
///
/// Quiet-by-default recurrence display.
/// - Collapsed: concise summary (nothing for once-only, deadline countdown, or “Due in …” for recurring)
/// - Expanded: full sentence for deadline; then either “repeats every …” for simple rules,
///             or a “View Recurrence” button for complex rules (no inline calendar).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/04/2025 @ 19:57
public class RecurrenceView extends Table {

    private final ModelTask task;
    private final Skin skin;
    private final Schedule schedule;
    private boolean expanded = false;

    private final RecurrenceSummary summary;
    private final Table detail = new Table();
    // Single, reusable, clip-animated slot:
    private final ClipGroup<Table> detailWrapper;
    private final Cell<ClipGroup<Table>> detailCell;


    public RecurrenceView(ModelTask task, Skin skin) {
        this.task = task;
        this.skin = skin;
        this.schedule = new Schedule(task);

        summary = new RecurrenceSummary(task, skin, schedule);
        add(summary).left().row();

        // Mount one wrapper row; start collapsed at zero height.
        detailWrapper = new net.wti.ui.view.panes.ClipGroup<>(detail);
        detailWrapper.setMeasureByCurrentSize(false);
        detailCell = add(detailWrapper).left().growX().minHeight(0).prefHeight(0).maxHeight(0);
        row();

        rebuild();

        task.onChange("updated", (before, after) -> {
            schedule.invalidate();
            rebuild();
        });
    }

    private void rebuild() {
        // Rebuild the detail contents only; the row and wrapper stay mounted.
        detail.clearChildren();

        if (expanded) {
            buildExpandedDetail();
            // Let expand() control animation; if caller only rebuilt while open,
            // ensure wrapper is sized to its natural height without re-adding rows.
            float target = Math.max(0f, detail.getPrefHeight());
            detailCell.height(target);
            detailCell.maxHeight(target);
            detailWrapper.setHeight(target);
        } else {
            // Keep collapsed size; do not remove the row.
            detailCell.height(0f);
            detailCell.maxHeight(0f);
            detailWrapper.setHeight(0f);
        }
        invalidateHierarchy();
    }

    private void buildExpandedDetail() {
        final Double deadline = task.getDeadline();
        final boolean onceOnly = schedule.isOnceOnly();

        // If once-only and no deadline, expanded view should also be empty.
        if (onceOnly && deadline == null) {
            return;
        }

        final Label.LabelStyle bodyStyle =
                skin.has("task-preview", Label.LabelStyle.class)
                        ? skin.get("task-preview", Label.LabelStyle.class)
                        : skin.get(Label.LabelStyle.class);

        // Sentence for deadline, if present
        if (deadline != null) {
            final Label due;
            if (deadline == 0) {
                due = new Label("Does not recur", bodyStyle);
            } else {
                double delta = deadline.longValue() - X_Time.nowMillis();
                String sentence = "Due in " + X_Time.print(delta) + ", on " + X_Time.timestampHuman(deadline.longValue());
                due = new Label(sentence, bodyStyle);
            }
            due.setFontScale(0.95f);
            detail.add(due).left().row();
        }

        // If once-only (with deadline already handled above), nothing more to say.
        if (onceOnly) {
            return;
        }

        // Recurring rules — simple vs. complex
        boolean simple = isSingleSimpleRule();

        if (simple) {
            String shortDesc = schedule.getShortDescription(); // e.g., "Every day at 5pm"
            if (shortDesc == null || shortDesc.isEmpty()) shortDesc = "Repeats";
            Label repeats = new Label("Repeats: " + shortDesc, bodyStyle);
            repeats.setFontScale(0.95f);
            detail.add(repeats).left().row();
        } else {
            TextButton btn = new TextButton("View Recurrence", skin);
            btn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    // TODO: open a popup/modal that renders the full recurrence (calendar or advanced renderer)
                    // For now, no-op.
                }
            });
            detail.add(btn).left().row();

            String hover = schedule.getLongDescriptions().isEmpty()
                    ? "Complex recurrence rules"
                    : String.join("\n", schedule.getLongDescriptions());
            SizingTextTooltip tip = new SizingTextTooltip(hover, skin);
            btn.addListener(tip);
        }
    }

    private boolean isSingleSimpleRule() {
        try {
            // Heuristic: if schedule yields exactly one long description, treat as simple.
            return schedule.getLongDescriptions() != null
                    && schedule.getLongDescriptions().size() == 1;
        } catch (Throwable ignore) {
            return false;
        }
    }

    public void expand() {
        if (!expanded) {
            expanded = true;
            // Build content first so we can measure the natural height.
            detail.clearChildren();
            buildExpandedDetail();
            float target = Math.max(0f, detail.getPrefHeight());
            animateDetailHeight(target, 0.25f);
        }
    }

    public void collapse() {
        if (expanded) {
            expanded = false;
            animateDetailHeight(0f, 0.20f);
        }
    }

    private void animateDetailHeight(float toHeight, float durationSec) {
        // Drive both the wrapper height and the Table cell constraints.
        com.badlogic.gdx.scenes.scene2d.actions.TemporalAction a =
                new com.badlogic.gdx.scenes.scene2d.actions.TemporalAction(durationSec) {
                    float startH;
                    @Override protected void begin() {
                        startH = detailWrapper.getHeight();
                        detailCell.minHeight(0f);
                        detailCell.prefHeight(startH);
                        detailCell.maxHeight(startH);
                        detailCell.height(startH);
                        detailWrapper.setHeight(startH);
                        detailWrapper.invalidateHierarchy();
                        invalidateHierarchy();
                    }
                    @Override protected void update(float percent) {
                        float h = startH + (toHeight - startH) * percent;
                        detailCell.height(h);
                        detailCell.maxHeight(h);
                        detailWrapper.setHeight(h);
                        detailWrapper.invalidateHierarchy();
                        invalidateHierarchy();
                    }
                    @Override protected void end() {
                        detailCell.height(toHeight);
                        detailCell.maxHeight(toHeight);
                        detailWrapper.setHeight(toHeight);
                        detailWrapper.invalidateHierarchy();
                        invalidateHierarchy();
                    }
                };
        // Attach the animation to the wrapper so ClipGroup reports current height during the animation.
        detailWrapper.addAction(a);
    }

    public boolean isExpanded() {
        return expanded;
    }
}
