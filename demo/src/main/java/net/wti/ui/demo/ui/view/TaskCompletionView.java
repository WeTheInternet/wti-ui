package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.api.IsDeadlineView;
import net.wti.ui.demo.api.ModelTaskCompletion;
import net.wti.ui.demo.view.api.IsTaskView;

/// TaskCompletionView
///
/// A non-interactive, expandable visual component to render a completed task summary.
/// Designed for compact display with optional toggle to reveal historical stats.
///
/// Responsibilities:
/// 『 ✓ 』 Render task name and basic metadata (completion time, etc.)
/// 『 ✓ 』 Support expansion/collapse to toggle stats view
/// 『   』 Compute and show historical stats:
///     『   』Number of times completed in a row
///     『   』Number of times completed late
///     『   』Total time spent late (accumulated)
///     『   』Most recent completed timestamp
///     『   』Average time until completion (future)
///     『   』Approval or verification status
/// 『   』 Hook up animated transitions (future polish)
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:10:28 CST
public class TaskCompletionView extends Table implements IsTaskView<ModelTaskCompletion> {

    private final ModelTaskCompletion completion;
    private final Skin skin;
    private boolean expanded = false;

    public TaskCompletionView(ModelTaskCompletion completion, Skin skin) {
        super(skin);
        this.completion = completion;
        this.skin = skin;
//        setBackground("default-pane");
        buildCompact();
    }

    /// Builds the compact (default) view of the completed task.
    private void buildCompact() {
        clear();
        add(new Label(completion.getName(), skin)).left().row();
        if (completion.getDescription() != null && !completion.getDescription().isEmpty()) {
            add(new Label(completion.getDescription(), skin)).left().row();
        }
        add(new Label("Completed @ " + completion.getCompleted(), skin)).left();
    }

    /// Builds the expanded view, showing additional metadata and statistics.
    private void buildExpanded() {
        buildCompact();
        row();
        // Placeholder stats; these will be computed from real data in future versions.
        add(new Label("Completion stats:", skin)).left().row();
        add(new Label("　　・Times completed in a row: (todo)", skin)).left().row();
        add(new Label("　　・Times completed late: (todo)", skin)).left().row();
        add(new Label("　　・Total lateness: (todo)", skin)).left().row();
        add(new Label("　　・Last completed: " + completion.getCompleted(), skin)).left().row();
        add(new Label("　　・Status: " + completion.getStatus(), skin)).left().row();
    }

    /// Expand the view to reveal stats.
    @Override
    public void expand() {
        this.expanded = true;
        buildExpanded();
    }

    /// Collapse the view to compact mode.
    @Override
    public void collapse() {
        this.expanded = false;
        buildCompact();
    }

    /// Redraw the view in its current expanded or collapsed state.
    @Override
    public void rerender() {
        if (expanded) {
            buildExpanded();
        } else {
            buildCompact();
        }
    }

    @Override
    public ModelTaskCompletion getTask() {
        return completion;
    }

    /// Completed tasks do not track deadlines
    @Override
    public IsDeadlineView<Table> getDeadlineView() {
        return null;
    }

}

