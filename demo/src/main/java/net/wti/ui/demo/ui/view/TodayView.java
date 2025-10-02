package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.wti.tasks.event.RefreshFinishedEvent;
import net.wti.tasks.index.TaskIndex;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.Schedule;
import net.wti.ui.demo.theme.TaskUiTheme;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.gdx.view.AccordionPane;
import net.wti.ui.view.api.HasScrollPane;
import net.wti.ui.view.panes.ListView;
import xapi.fu.Do;
import xapi.fu.itr.MappedIterable;
import xapi.fu.log.Log;

import java.util.ArrayList;
import java.util.List;

/// TodayView
///
/// A responsive Scene2D container that gives a **unified “what’s going on now”**
/// snapshot of tasks:
///
/// - **Landscape (width ≥ height):** side-by-side split
///   - **Deadlines** (left): tasks with a concrete deadline, sorted chronologically
///   - **Goals** (right): tasks without a concrete deadline, sorted by priority
/// - **Portrait (height > width):** accordion stack with the same two lists;
///   only one panel is visible at a time (tap titles to switch)
///
/// ### Integration
/// ```java
/// TodayView view = new TodayView(skin,() -> taskRepo.getAllTasks());
/// root.add(view).grow();
///
///// whenever tasks change:
/// view.refresh();
///```
///
/// ### Skin references (all optional/fallback-friendly)
/// - background: `"panel-actionbar"` (falls back to `"button"` or null)
/// - headers: label style `"task-recurrence-value"`
/// - row title: `"task-name"`
/// - row subtitle / time / chips: `"task-preview"`
///
/// ### Extending
/// - Replace the `safe*` accessors if your `ModelTask` API names differ
/// - Put `TaskActionBar` into `makeRow(...)` to add inline actions
///
/// ### Performance
/// - `refresh()` rebuilds the lists; call when your dataset changes
/// - Layout switches between landscape/portrait in `layout()` based on size
///
/// Created by James X. Nelson (James@WeTheInter.net) and chatgpt on 27/08/2025 @ 04:59
public class TodayView extends Table implements HasScrollPane {

    // ---- task data ----
    private final TaskIndex index;

    // ---- content tables (ONE copy each) ----
    private final ScheduleView deadlinesList;
    private final TaskListView goalsList;

    // ---- placeholder slots (two sets) ----
    private final Container<Table> leftSlotLandscape  = new Container<>();
    private final Container<Table> rightSlotLandscape = new Container<>();
    private final Container<Table> leftSlotPortrait   = new Container<>();
    private final Container<Table> rightSlotPortrait  = new Container<>();

    // ---- layouts ----
    private final SplitPane landscapeSplit;
    private final AccordionPane portraitAccordion;

    private final TaskViewCacheActive taskCache;


    // ---- state / lifecycle ----
    private boolean portrait;
    private boolean disposed;
    private Do cleanup;

    public TodayView(final TaskUiTheme theme, final TaskController controller) {
        super(theme.getSkin());
        this.taskCache = new TaskViewCacheActive(theme.getSkin(), controller);
        this.index = controller.getIndex();
        final Skin skin = theme.getSkin();

        index.subscribeActiveTasks(task -> {
            Log.tryLog(TodayView.class, this, "Subscribed to task", task.getName());
        });

        setBackground(optionalDrawable(skin, "panel-actionbar", "button"));

        // Build the single instances of each list
        deadlinesList = new ScheduleView(skin, index);
        goalsList     = new TaskListView(skin, "Goals");

        // Put content into the landscape slots initially
        leftSlotLandscape.setActor(deadlinesList);
        rightSlotLandscape.setActor(goalsList.container());

        // Let the split shrink either side regardless of the content's min width

        final float minWidth = 400f;
        leftSlotLandscape.minWidth(minWidth);
        rightSlotLandscape.minWidth(minWidth);
        leftSlotPortrait.minWidth(minWidth);
        rightSlotPortrait.minWidth(minWidth);

        // ---- Landscape: SplitPane that holds the landscape slots ----
        landscapeSplit = new SplitPane(leftSlotLandscape, rightSlotLandscape, false, skin);
        landscapeSplit.setSplitAmount(0.5f);
        landscapeSplit.setMinSplitAmount(0.25f);
        landscapeSplit.setMaxSplitAmount(0.75f);

        // ---- Portrait: Accordion that holds the portrait slots ----
        portraitAccordion = new AccordionPane(theme);
        // We add the *portrait* slots as the section contents
        portraitAccordion.addSection("Deadlines", leftSlotPortrait);
        portraitAccordion.addSection("Goals",     rightSlotPortrait);
        portraitAccordion.openOnly("Deadlines");

        leftSlotLandscape.fill();
        rightSlotLandscape.fill();
        leftSlotPortrait.fill();
        rightSlotPortrait.fill();

        defaults().grow();
        add(landscapeSplit).grow();
        portrait = false; // layout() will verify on first pass

        // External orientation change hook (if your theme emits one)
        cleanup = theme.onOrientationChanged(viewMode -> refresh());

        // TODO: show a "Loading..." spinner
        index.subscribeEvents(evt-> {
            // TODO: make this auto-refresh conditional to avoid churn
            refresh();
        }, RefreshFinishedEvent.class);

    }

    /// Re-query tasks and rebuild both lists (chronological + priority).
    public void refresh() {
        final MappedIterable<Schedule> all = index.getActive();

        final List<ModelTask> withDeadline = new ArrayList<>();
        final List<ModelTask> withoutDeadline = new ArrayList<>();

        for (Schedule s : all) {
            final ModelTask t = s.getTask();
            Long due = getDeadlineMillis(t);
            if (due != null) withDeadline.add(t);
            else withoutDeadline.add(t);
        }

        // Left is event-driven; just trigger a refresh to re-pull from index snapshot.
        deadlinesList.refresh();

        // Right: priority desc (higher first). Tie-breaker: title A→Z
        withoutDeadline.sort((a, b) -> {
            int p = Integer.compare(b.getPriority(), a.getPriority());
            if (p != 0) return p;
            return safeTitle(a).compareToIgnoreCase(safeTitle(b));
        });

        goalsList.rebuild(withoutDeadline, false);

        invalidateHierarchy();
        layout();
    }

    /// Orientation switch without duplicate-parent errors:
    /// We **move** the same content tables between the slot pairs.
    @Override public void layout() {
        boolean nowPortrait = Gdx.graphics.getHeight() > Gdx.graphics.getWidth();
        if (nowPortrait != portrait) {
            portrait = nowPortrait;

            if (portrait) {
                // Move content from landscape slots → portrait slots
                move(deadlinesList, leftSlotLandscape,  leftSlotPortrait);
                move(goalsList.container(),     rightSlotLandscape, rightSlotPortrait);

                clearChildren();
                add(portraitAccordion).grow();

                // Accordion UX: only one open at a time
                portraitAccordion.openOnly("Deadlines");
                portraitAccordion.invalidateHierarchy();
            } else {
                // Move content from portrait slots → landscape slots
                move(deadlinesList, leftSlotPortrait,  leftSlotLandscape);
                move(goalsList.container(),     rightSlotPortrait, rightSlotLandscape);

                clearChildren();
                add(landscapeSplit).grow();

                landscapeSplit.invalidateHierarchy();
            }
        }
        super.layout();
    }

    // --- Moves `content` from `fromSlot` to `toSlot` safely (no rebuild) ---
    private static void move(Table content, Container<Table> fromSlot, Container<Table> toSlot) {
        if (fromSlot.getActor() == content) fromSlot.setActor(null);
        toSlot.setActor(content);
        // ensure content gets laid out in the new container
        content.invalidateHierarchy();
    }

    // ---------------------------------------------------------------------
    // Helpers (data access + theming fallbacks)
    // ---------------------------------------------------------------------

    private static Long getDeadlineMillis(ModelTask t) {
        try {
            Double d = t.getDeadline(); // adapt: return null when no deadline
            return d == null || d == 0 ? null : d.longValue();
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static String safeTitle(ModelTask t) {
        try {
            String s = t.getName();
            return s == null ? "" : s;
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static Drawable optionalDrawable(Skin skin, String... names) {
        for (String n : names) if (skin.has(n, Drawable.class)) return skin.getDrawable(n);
        return null;
    }

    // ---------------------------------------------------------------------
    // Inner: TaskListView (adds a header + scrollable content)
    // ---------------------------------------------------------------------

    private class TaskListView extends ListView {
        private final Skin skin;
        private final Table root = new Table();

        TaskListView(Skin skin, String title) {
            super(skin, title);
            this.skin = skin;
        }

        void rebuild(List<ModelTask> tasks, boolean showTime) {
            content.clearChildren();
            content.top().defaults().growX().pad(2, 4, 2, 4);
            for (ModelTask t : tasks) {
                content.add(makeRow(t, showTime)).growX().row();
            }
        }

        private Table makeRow(ModelTask t, boolean showTime) {
            Table row = new Table(skin);
            row.setBackground(safeBg(skin));
            row.pad(4);
            row.defaults().growX(); // ensure cells in the row can take full width
            row.add(taskCache.create(t)).expandX().fillX();
            return row;
        }

        private Drawable safeBg(Skin skin) {
            if (skin.has("button", Drawable.class)) return skin.getDrawable("button");
            if (skin.has("panel-actionbar", Drawable.class)) return skin.getDrawable("panel-actionbar");
            return null;
        }

    }

    // ---- cleanup when detached ----
    @Override protected void setStage(Stage stage) {
        Stage prev = getStage();
        super.setStage(stage);
        if (!disposed && prev != null && stage == null) {
            disposed = true;
            if (cleanup != null) cleanup.done();
        }
    }

    @Override public boolean remove() {
        boolean r = super.remove();
        if (r && !disposed) {
            disposed = true;
            if (cleanup != null) cleanup.done();
        }
        return r;
    }
}