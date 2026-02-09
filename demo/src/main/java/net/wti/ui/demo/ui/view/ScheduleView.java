package net.wti.ui.demo.ui.view;


import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import net.wti.tasks.event.RefreshFinishedEvent;
import net.wti.tasks.index.DateKey;
import net.wti.tasks.index.TaskIndex;
import net.wti.ui.controls.focus.HoverScrollFocus;
import net.wti.ui.demo.api.ModelSettings;
import net.wti.ui.view.api.IsView;
import xapi.fu.Do;
import xapi.fu.log.Log;
import xapi.time.X_Time;
import xapi.time.api.TimeComponents;

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
    private boolean initialized;

    private final Table dayStack = new Table();
    private final ScrollPane scroller;

    // Keep mounted days in order. We append/prepend as we scroll.
    private final LinkedHashMap<DateKey, DayView> mountedDays = new LinkedHashMap<>();

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

        // Subscriptions: keep the view hot while data arrives asynchronously.
        wireIndexSubscriptions();

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
        // 1) First refresh finished -> initialize days
        Do a = index.subscribeEvents(evt -> {
            if (!initialized) {
                initialized = true;
                initDays();
            } else {
                refresh();
            }
        }, RefreshFinishedEvent.class);

        // 2) Incremental updates -> refresh (can be optimized later)
        Do b = index.subscribe(taskEvent -> {
            if (initialized) {
                Log.tryLog(ScheduleView.class, this, "Refreshing due to", taskEvent);
                refresh();
            }
        });

        unsubscribeAll = () -> {
            try { a.done(); } catch (Throwable ignored) {}
            try { b.done(); } catch (Throwable ignored) {}
        };
    }

    private void initDays() {
        dayStack.clearChildren();
        mountedDays.clear();

        final TimeComponents now = X_Time.breakdown(X_Time.nowMillis(), ModelSettings.timeZone());
        DateKey today = DateKey.from(now);
        mountDay(today.minusDays(1), false);
        mountDay(today, false);
        mountDay(today.plusDays(1), false);
        layoutDayStack();

        ensureSomeContent(today, 7); // probe outward initially
        invalidateHierarchy();
        layout();
    }

    @Override
    public void refresh() {
        // Rebuild all mounted days from the index’s current snapshot of ACTIVE tasks.
        // If your definition changes, swap to getAll().

        if (!initialized) return;
        for (Map.Entry<DateKey, DayView> e : mountedDays.entrySet()) {
            e.getValue().setTasks(index.getDayWithDeadlines(e.getKey().getTime()));
            e.getValue().refresh();
        }
        // If still empty, extend outward a bit more to find items (non-destructive).
        final TimeComponents now = X_Time.breakdown(X_Time.nowMillis(), ModelSettings.timeZone());
        DateKey pivot = DateKey.from(now);
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
            DateKey first = mountedDays.keySet().iterator().next();
            DateKey prev = first.minusDays(1);
            if (!mountedDays.containsKey(prev)) {
                mountDay(prev, true);
            }
        } else if (max - y <= LOAD_THRESHOLD_PX) {
            // Near bottom: add next day
            DateKey last = null;
            for (DateKey d : mountedDays.keySet()) {
                last = d;
            }
            if (last != null) {
                DateKey next = last.plusDays(1);
                if (!mountedDays.containsKey(next)) {
                    mountDay(next, false);
                }
            }
        }
    }

    private void mountDay(DateKey date, boolean prepend) {
        DayView view = new DayView(skin, date, index.getDayWithDeadlines(date.getTime()));
        view.refresh();
        if (prepend) {
            LinkedHashMap<DateKey, DayView> tmp = new LinkedHashMap<>();
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
    private void ensureSomeContent(DateKey pivot, int maxRadiusDays) {
        if (mountedDays.values().stream().anyMatch(DayView::hasItems)) return;

        for (int r = 1; r <= maxRadiusDays; r++) {
            DateKey prev = pivot.minusDays(r);
            DateKey next = pivot.plusDays(r);

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

