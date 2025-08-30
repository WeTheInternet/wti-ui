package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import net.wti.tasks.event.*;
import net.wti.tasks.index.TaskIndex;
import net.wti.ui.controls.focus.HoverScrollFocus;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.view.api.IsView;
import xapi.fu.Do;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// ScheduleView
///
/// A generic, scrollable schedule container that wires together specific
/// time-granularity views (DayView, WeekView, etc.).
///
/// Initial mode: Day scroller
/// - Starts with (yesterday, today, tomorrow)
/// - When scrolled near the top/bottom, lazily inserts the previous/next day
/// - Rebuilds each DayView from a live task supplier on refresh()
///
/// Usage:
///   ScheduleView schedule = new ScheduleView(skin, () -> index.getActive());
///   container.add(schedule).grow();
///
/// Created by James X. Nelson (James@WeTheInter.net) and GPT-5 on 30/08/2025 @ 07:11
public class ScheduleView extends Table implements IsView {

    /// Live index (async)
    private final TaskIndex index;
    private Do unsubscribeAll = Do.NOTHING;

    private final Skin skin;

    private final Table dayStack = new Table();
    private final ScrollPane scroller;

    // Keep mounted days in order. We append/prepend as we scroll.
    private final LinkedHashMap<LocalDate, DayView> mountedDays = new LinkedHashMap<>();

    // How close to an edge (in pixels) before we load a new day.
    private static final float LOAD_THRESHOLD_PX = 80f;

    public ScheduleView(final Skin skin, final TaskIndex index) {
        super(skin);
        this.skin = skin;
        this.index = Objects.requireNonNull(index, "index");

        align(Align.topLeft);
        defaults().growX();

        dayStack.top().defaults().growX().pad(6, 6, 6, 6);

        scroller = new ScrollPane(dayStack, skin, "no-bg");
        scroller.setFadeScrollBars(false);
        scroller.setScrollingDisabled(true, false); // vertical only
        add(scroller).grow();

        // Prime with yesterday, today, tomorrow.
        LocalDate today = LocalDate.now();
        mountDay(today.minusDays(1), false);
        mountDay(today, false);
        mountDay(today.plusDays(1), false);
        layoutDayStack();

        // Subscriptions: keep the view hot while data arrives asynchronously.
        wireIndexSubscriptions();

        // If everything is empty, extend outward to find the nearest matches.
        ensureSomeContent(today, 7); // probe up to ±7 days initially

        // Lazy edge loading
        scroller.addListener(event -> {
            if (!(event instanceof com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent)) {
                return false;
            }
            maybeLoadEdges();
            return false;
        });

        HoverScrollFocus.attach(scroller);
    }

    private void wireIndexSubscriptions() {
        // 1) Listen for refresh-complete → full rebuild from index snapshot
        Do a = index.subscribeEvents(evt -> {
            refresh();
        }, RefreshFinishedEvent.class);

        // 2) Listen for incremental updates (created/started/updated) that should be visible in the schedule
        Do b = index.subscribe(taskEvent -> {
            if (taskEvent instanceof TaskCreatedEvent
                    || taskEvent instanceof TaskStartedEvent
                    || taskEvent instanceof TaskUpdatedEvent
                    || taskEvent instanceof TaskFinishedEvent
                    || taskEvent instanceof TaskCancelledEvent
                    || taskEvent instanceof TaskLoadedEvent
                    || taskEvent instanceof TaskDeletedEvent) {
                // Minimal approach: just refresh. Index posts on GL thread, so this is UI-safe.
                refresh();
            }
        });

        unsubscribeAll = () -> {
            try { a.done(); } catch (Throwable ignored) {}
            try { b.done(); } catch (Throwable ignored) {}
        };
    }


    @Override
    public void refresh() {
        // Rebuild all mounted days from the index’s current snapshot of ACTIVE tasks.
        // If your definition changes, swap to getAll().
        Iterable<ModelTask> active = index.getActive();

        for (Map.Entry<LocalDate, DayView> e : mountedDays.entrySet()) {
            e.getValue().setTasks(active);
            e.getValue().refresh();
        }

        // If still empty, extend outward a bit more to find items (non-destructive).
        LocalDate pivot = LocalDate.now();
        ensureSomeContent(pivot, 3);

        invalidateHierarchy();
        layout();
    }

    @Override
    public void dispose() {
        unsubscribeAll.done();
        unsubscribeAll = Do.NOTHING;
    }

    // ---- Internals -------------------------------------------------------

    private void maybeLoadEdges() {
        float y = scroller.getScrollY();
        float max = scroller.getMaxY();

        if (y <= LOAD_THRESHOLD_PX) {
            // Near top: add previous day
            LocalDate first = mountedDays.keySet().iterator().next();
            LocalDate prev = first.minusDays(1);
            if (!mountedDays.containsKey(prev)) {
                mountDay(prev, true);
            }
        } else if (max - y <= LOAD_THRESHOLD_PX) {
            // Near bottom: add next day
            LocalDate last = null;
            for (LocalDate d : mountedDays.keySet()) last = d;
            LocalDate next = last.plusDays(1);
            if (!mountedDays.containsKey(next)) {
                mountDay(next, false);
            }
        }
    }

    private void mountDay(LocalDate date, boolean prepend) {
        DayView view = new DayView(skin, date, index.getActive());
        view.refresh();
        if (prepend) {
            // rebuild ordered map: new first entry
            LinkedHashMap<LocalDate, DayView> tmp = new LinkedHashMap<>();
            tmp.put(date, view);
            tmp.putAll(mountedDays);
            mountedDays.clear();
            mountedDays.putAll(tmp);
        } else {
            mountedDays.put(date, view);
        }
        layoutDayStack();
        invalidateHierarchy();
    }

    // If all mounted days are empty, expand outward around a pivot date
    // until we find any items or exhaust the probe limit.
    private void ensureSomeContent(LocalDate pivot, int maxRadiusDays) {
        if (mountedDays.values().stream().anyMatch(DayView::hasItems)) return;

        for (int r = 1; r <= maxRadiusDays; r++) {
            LocalDate prev = pivot.minusDays(r);
            LocalDate next = pivot.plusDays(r);

            if (!mountedDays.containsKey(prev)) mountDay(prev, true);
            if (mountedDays.values().stream().anyMatch(DayView::hasItems)) break;

            if (!mountedDays.containsKey(next)) mountDay(next, false);
            if (mountedDays.values().stream().anyMatch(DayView::hasItems)) break;
        }
    }

    private void layoutDayStack() {
        dayStack.clearChildren();
        dayStack.top();
        for (DayView v : mountedDays.values()) {
            dayStack.add(v).growX().row();
        }
    }
}

