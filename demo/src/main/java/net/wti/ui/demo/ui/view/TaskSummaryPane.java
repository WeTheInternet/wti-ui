package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.demo.api.ModelRecurrence;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.api.ModelTimeRecord;
import net.wti.ui.view.panes.ClipGroup;
import xapi.model.X_Model;
import xapi.model.api.ModelList;
import xapi.model.api.ModelQuery;
import xapi.time.X_Time;
import xapi.util.api.SuccessHandler;

/// # TaskSummaryPane
///
/// A compact, horizontally-aligned row of summary fields for a task.
/// Used in the collapsed view of a `TaskView`, rendered at the bottom right.
///
/// This widget shows dynamic data like the deadline, streak status, task state, and placeholders for time tracking.
///
/// ### 📊 Planned Summary Fields
///
/// | Element           | Displayed When                  | Style              | Notes / Future Enhancements                  |
/// |------------------|----------------------------------|--------------------|-----------------------------------------------|
/// | ✅ Deadline       | `task.getDeadline() != null`     | `DeadlineView`     | Uses color-coded alarms and hover info       |
/// | ✅ Streak Count   | `task.getRecurrence().size() > 0`| `task-summary`     | Eventually reflect # of on-time completions  |
/// | ✅ Time Logged    | always (placeholder for now)     | `task-summary`     | Will reflect actual time tracked vs. expected|
/// | ⏳ Snoozed Tag    | `task.isSnoozed()` (future flag) | `task-summary-tag` | Indicates temporary deferral by user         |
/// | ⛔ Paused Tag     | `task.isPaused()`  (future flag) | `task-summary-tag` | Denotes task is inactive but not archived    |
/// | 🗂 Archived Tag   | `task.isArchived()` (future flag)| `task-summary-tag` | Will hide task from active views             |
///
/// ### 👷‍♂️ Implementation Roadmap
/// - 『 ✓ 』 Integrate with `DeadlineView`
/// - 『 ✓ 』 Show recurrence summary (streak placeholder)
/// - 『 ✓ 』 Render "⏱ 0h logged" placeholder
/// - 『 ☐ 』 Hook up actual time tracking API
/// - 『 ☐ 』 Support dynamic tags with visibility logic
///
/// Created by ChatGPT 4o and James X. Nelson on 2025-04-17
public class TaskSummaryPane extends Table {

    private final RecurrenceView recurrenceView;
    private final Label description;
    private final ClipGroup<Label> descriptionLabel;
    private final Cell<ClipGroup<Label>> descriptionCell;
    private boolean expanded = false;  /// true when expanded; starts collapsed
    private boolean initialized = false; /// one-time post-construction init

    private static final String fireEmoji = "\uD83D\uDD25";
    private static final String clockEmoji = "\uD83D\uDD53";

    public TaskSummaryPane(Skin skin, ModelTask task) {
        this(skin, task, true);
    }

    public TaskSummaryPane(Skin skin, ModelTask task, boolean expanded) {
        super(skin);
        align(Align.center);
        setClip(true);
        this.expanded = expanded;

        description = new Label(task.getDescription(), skin.get(Label.LabelStyle.class));
        // Wrap description in a ClipGroup so overflow is clipped (and animatable)
        descriptionLabel = new ClipGroup<>(description);

        recurrenceView = new RecurrenceView(task, skin);
        /// Keep a handle to the cell so we can animate its maxHeight
        descriptionCell = add(descriptionLabel).center().growX().minHeight(0).maxHeight(0);
        row();
        add(recurrenceView).left().row();

        // Recurrence Streak Placeholder
        final ModelList<ModelRecurrence> recurrences = task.getRecurrence();
        final ModelList<ModelTimeRecord> timeRecord = task.getTimeRecord();
        final boolean hasRecurrence = recurrences != null && !recurrences.isEmpty();
        final boolean hasTimeRecord = timeRecord != null && !timeRecord.isEmpty();

        if (hasRecurrence) {
            // Placeholder streak logic — replace with actual task completion streak
            final Label streak = new Label(fireEmoji + clockEmoji, skin, "task-emoji");
            // Load the streak info from the completed tasks
            final ModelQuery<ModelTaskCompletion> q = new ModelQuery<>();
            X_Model.query(ModelTaskCompletion.class, q, SuccessHandler.handler(
                    success -> {
                    }, failure -> {
                        // TODO: better error handling
                        streak.setText(failure.getMessage());
                    }
            ));
            add(streak);
        }

        if (hasTimeRecord) {
            double total = 0;
            for (ModelTimeRecord record : timeRecord) {
                total += record.getSeconds();
                // TODO: something that can expand/contract the time record, to include the notes.
            }

            // Time Logged (stub for future duration tracking system)
            Label timeLogged = new Label(clockEmoji + " " + X_Time.print(total), skin, "task-emoji");
            add(timeLogged);
        }
        if (expanded) {
            final float targetH = Math.max(0f, description.getPrefHeight());
            descriptionCell.minHeight(0f).maxHeight(targetH).height(targetH);
            descriptionLabel.setHeight(targetH);
            invalidateHierarchy();
        } else {
            /// Start in collapsed visual state without animating
            descriptionLabel.setHeight(0f);
            descriptionCell.minHeight(0f).prefHeight(0f).maxHeight(0f).height(0f);
            invalidateHierarchy();
        }
        initialized = true;

    }

    /// Collapse the summary description with clipping-aware animation.
    public void collapse() {
        final boolean wasExpanded = expanded;
        if (!expanded && descriptionCell != null && descriptionCell.getMaxHeight() == 0f) {
            /// already collapsed
            return;
        }
        expanded = false;

        clearAllActions();

        // Animate cell maxHeight down to 0 for real clipping
        animateCellHeight(descriptionCell, descriptionLabel, 0f, 0.25f);

        recurrenceView.collapse();
    }

    private void clearAllActions() {
        // Clear any running animations to prevent growth on repeated toggles
        clearActions();
        description.clearActions();
        descriptionLabel.clearActions();
        recurrenceView.clearActions();
    }

    /// Expand the summary description to its natural height.
    public void expand() {
        final boolean wasExpanded = expanded;
        expanded = true;
        if (!initialized) return;
        if (wasExpanded) {
            /// already expanded
            return;
        }

        clearAllActions();

        // Measure the natural height from the label's preferred height
        final float targetH = Math.max(0f, description.getPrefHeight());
        animateCellHeight(descriptionCell, descriptionLabel, targetH, 0.25f);

        recurrenceView.expand();
    }

    /// Animate a Table Cell's height using both height() and maxHeight(),
    /// while keeping a clip-enabled wrapper sized appropriately.
    ///
    /// We attach the TemporalAction to the wrapper so its auto
    /// "measureByCurrentSize" mode engages and reports the current animating height.
    private void animateCellHeight(Cell<ClipGroup<Label>> cell, ClipGroup<Label> wrapper, float toHeight, float durationSec) {
        final float pref = Math.max(0f, description.getPrefHeight());
        final float currentCell = cell.getMaxHeight();
        final float currentWrapper = wrapper.getHeight();
        final float fromHeight = currentCell > 0f ? currentCell : (currentWrapper > 0f ? currentWrapper : (expanded ? 0f : pref));

        TemporalAction a = new TemporalAction(durationSec) {
            float startH;
            @Override protected void begin() {
                startH = fromHeight;
                /// Pin current constraints so Table honors the animation
                cell.minHeight(0f);
                cell.prefHeight(startH);
                cell.maxHeight(startH);
                cell.height(startH);
                wrapper.setHeight(startH);
                wrapper.invalidateHierarchy();
                invalidateHierarchy();
            }
            @Override protected void update(float percent) {
                float h = startH + (toHeight - startH) * percent;
                /// Drive all three constraints; height() sets min/pref/max at once,
                /// but we also set max explicitly for clarity.
                cell.height(h);
                cell.maxHeight(h);
                wrapper.setHeight(h);
                wrapper.invalidateHierarchy();
                invalidateHierarchy();
            }
            @Override protected void end() {
                /// Snap to final height and keep it (prevents rebound)
                cell.height(toHeight);
                cell.maxHeight(toHeight);
                wrapper.setHeight(toHeight);
                wrapper.invalidateHierarchy();
                invalidateHierarchy();
            }
        };
        /// Add the animation to the wrapper so ClipGroup reports getPrefHeight() as current size during animation
        wrapper.addAction(a);
    }

}
