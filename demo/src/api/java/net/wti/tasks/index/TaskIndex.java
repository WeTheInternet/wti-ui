package net.wti.tasks.index;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Timer;
import net.wti.tasks.event.*;
import net.wti.ui.demo.api.ModelSettings;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.api.Schedule;
import xapi.fu.Do;
import xapi.fu.Filter;
import xapi.fu.In1;
import xapi.fu.X_Fu;
import xapi.fu.data.MapLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.time.api.TimeComponents;
import xapi.time.api.TimeZoneInfo;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/// TaskIndex
///
/// A centralized, in-memory index of all known tasks, with a tiny event bus.
/// UI code (views, widgets) can subscribe to specific events, or to convenience
/// feeds like “all active tasks”. Controllers update the index whenever they
/// change task state. The index can also refresh itself on demand or on a
/// repeating schedule.
///
/// ### Core ideas
/// * **Cache**: stores tasks by id; exposes views (active / finished / canceled).
/// * **Events**: fire on load/create/start/finish/cancel/update/delete/refresh.
/// * **Subscriptions**: return a `Do` handle you call `.done()` to unsubscribe.
/// * **Threading**: all notifications are posted to the main (GL) thread via
///   `Gdx.app.postRunnable(...)`, so UI can mutate Scene2D safely.
/// * **Refresh**: `refresh()` uses a loader `Supplier<Collection<ModelTask>>`
///   to (re)load tasks; `startAutoRefresh(minutes)` schedules periodic refresh.
///
/// ### Minimal usage
/// ```java
/// TaskIndex index = new TaskIndex();
/// index.setLoader(repo::loadAllTasks);
/// index.refresh(); // initial load
///
/// Do stop = index.subscribeActiveTasks(task -> {
///// render or track active tasks
///});
///
///// when disposing:
/// stop.done();
///```
///
/// ### Controller hooks (examples)
/// ```java
///// in TaskController:
/// taskIndex.onTaskCreated(task);
/// taskIndex.onTaskStarted(task);
/// taskIndex.onTaskFinished(task);
/// taskIndex.onTaskCancelled(task);
/// taskIndex.onTaskUpdated(task);   // generic state change
/// taskIndex.onTaskDeleted(taskId); // remove
///```
///
/// ### Identifying tasks
/// The index relies on a stable task id. By default we call `task.getId()`.
/// If your API differs, adapt `taskId(ModelTask)` below.
///
/// Created by James X. Nelson (James@WeTheInter.net) and chatgpt on 27/08/2025 @ 06:54
public class TaskIndex {

    private final String namespace;

    public TaskIndex() {
        namespace = "";
    }
    public TaskIndex(final String namespace) {
        this.namespace = namespace;
    }

    // ---------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------

    private final MapLike<ModelKey, Schedule> byId = X_Jdk.mapHashConcurrent();
    private final CopyOnWriteArrayList<TaskEventListener> listeners = new CopyOnWriteArrayList<>();

    // ---- Bucketing (deadlines grouped by DateKey) ----
    private final ConcurrentMap<DateKey, CopyOnWriteArrayList<Schedule>> byDay = new ConcurrentHashMap<>();
    private TimeZoneInfo bucketZone = X_Time.systemZone();
    private int rolloverHour = 4; // "4am rule"

    // ---- Metrics ----
    private final AtomicInteger refreshCount = new AtomicInteger();
    private final AtomicInteger bucketUpdates = new AtomicInteger();


    /// Optional repeating refresh task.
    private Timer.Task autoRefreshTask;

    private final AtomicBoolean refreshPending = new AtomicBoolean(false);
    private Throwable lastError;

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    /// Snapshot of all tasks.
    public SizedIterable<Schedule> getAll() {
        return byId.mappedValues();
    }

    /// Tasks considered "active" (not finished and not canceled).
    public MappedIterable<Schedule> getActive() {
        return filter(this::isActive);
    }

    /// Tasks considered "finished".
    public MappedIterable<Schedule> getFinished() {
        return filter(this::isFinished);
    }

    /// Tasks considered "canceled".
    public MappedIterable<Schedule> getCanceled() {
        return filter(this::isCanceled);
    }

    private MappedIterable<Schedule> filter(Filter.Filter1<Schedule> p) {
        return byId.mappedValues().filter(p);
    }

    // ---------------------------------------------------------------------
    // Subscriptions (return Do for easy removal)
    // ---------------------------------------------------------------------

    /// Subscribe to all events.
    public Do subscribe(TaskEventListener l) {
        listeners.add(l);
        return () -> listeners.remove(l);
    }

    /// Subscribe to specific event classes.
    @SafeVarargs
    public final Do subscribeEvents(Consumer<TaskEvent> handler, Class<? extends TaskEvent>... types) {
        final Set<Class<? extends TaskEvent>> want = new HashSet<>(Arrays.asList(types));
        TaskEventListener l = evt -> {
            if (want.contains(evt.getClass())) handler.accept(evt);
        };
        listeners.add(l);
        return () -> listeners.remove(l);
    }

    /// Iterate over current active tasks immediately, then get incremental updates:
    /// - TaskCreated/Started/Updated that become active
    /// - TaskFinished/Cancelled that remove active
    public Do subscribeActiveTasks(In1<ModelTask> onEach) {
        // immediate dump
        getActive().map(Schedule::getTask).forAll(onEach);

        // incremental
        TaskEventListener l = evt -> {
            if (evt instanceof TaskCreatedEvent && isActive(((TaskCreatedEvent) evt).task)) {
                TaskCreatedEvent e = (TaskCreatedEvent) evt;
                onEach.in(e.task);
            } else if (evt instanceof TaskStartedEvent && isActive(((TaskStartedEvent) evt).task)) {
                TaskStartedEvent e = (TaskStartedEvent) evt;
                onEach.in(e.task);
            } else if (evt instanceof TaskUpdatedEvent && isActive(((TaskUpdatedEvent) evt).task)) {
                TaskUpdatedEvent e = (TaskUpdatedEvent) evt;
                onEach.in(e.task);
            }
            // finishes/cancels remove from active; if a view needs removal notice,
            // it should listen to those event types directly.
        };
        listeners.add(l);
        return () -> listeners.remove(l);
    }

    // ---------------------------------------------------------------------
    // Refresh API
    // ---------------------------------------------------------------------

    /// Immediate refresh using the configured loader (no-op if none).
    public void refresh() {
        if (refreshPending.compareAndSet(false, true)) {
            // we won the lock, request data
            final ErrorHandler<? extends Throwable> failHandler = fail -> {
                lastError = fail;
                refreshPending.set(false);
                synchronized (refreshPending) {
                    refreshPending.notifyAll();
                }
            };
            RunningRefreshQuery operation = new RunningRefreshQuery(failHandler);
            operation.getQuery().setNamespace(namespace);
            post(new RefreshStartedEvent(operation));
            X_Model.query(ModelTask.class, operation.getQuery(), SuccessHandler.handler(
                    success ->
                            processResults(operation, success), failHandler)
            );
        } else {
            // we lost the lock. block. TODO: remove this; there should be no blocking anywhere.
            final Moment start = X_Time.now();
            while (refreshPending.get()) {
                synchronized (refreshPending) {
                    try {
                        refreshPending.wait(10);
                        if (X_Time.nowMillis() - start.millis() > 100) {
                            Log.tryLog(TaskIndex.class, this, "Blocking on refresh for", X_Time.difference(start));
                        }
                    } catch (InterruptedException e) {
                        Log.tryLog(TaskIndex.class, this, "Interrupted waiting for refresh lock", e);
                    }
                }
            }
            Log.tryLog(TaskIndex.class, this, "Blocked on task refresh for", X_Time.difference(start));
        }
    }

    private void processResults(final RunningRefreshQuery operation, final ModelQueryResult<ModelTask> success) {
        operation.setSuccess(success);
        Log.tryLog(TaskIndex.class, this, "Received " + success.getSize() + " results", X_Time.now());
        final ModelQuery<ModelTask> query = operation.getQuery();
        final ErrorHandler<? extends Throwable> failHandler = operation.getFailHandler();
        for (ModelTask model : success.getModels()) {
            upsert(model);
        }

        final String newCursor = success.getCursor();
        if (X_String.isEmpty(newCursor) || newCursor.equals(operation.getCursor())) {
            // no cursor, we are done.
            finishRefresh();
        } else {
            operation.setCursor(newCursor);
            // there is a cursor, keep loading results
            X_Model.query(ModelTask.class, query, SuccessHandler.handler(next -> {
                processResults(operation, next);
            }, failHandler));

        }
    }

    private void finishRefresh() {
        refreshCount.incrementAndGet();
        post(new RefreshFinishedEvent(byId.size()));
        refreshPending.set(false);
        synchronized (refreshPending) {
            refreshPending.notifyAll();
        }
    }

    /// Start auto-refresh every N minutes. Returns Do to stop it.
    public Do startAutoRefresh(float minutes) {
        stopAutoRefresh();
        autoRefreshTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                refresh();
            }
        }, 0f, minutes * 60f); // first immediately, then every N minutes
        return this::stopAutoRefresh;
    }

    /// Stop auto-refresh if running.
    public void stopAutoRefresh() {
        if (autoRefreshTask != null) {
            autoRefreshTask.cancel();
            autoRefreshTask = null;
        }
    }

    // ---------------------------------------------------------------------
    // Controller hooks (call these when you mutate tasks)
    // ---------------------------------------------------------------------

    /// Record a brand-new task (create).
    public void onTaskCreated(ModelTask task) {
        upsert(task);
        post(new TaskCreatedEvent(task));
    }

    /// Record “work started” (if you track such a state).
    public void onTaskStarted(ModelTask task) {
        upsert(task);
        post(new TaskStartedEvent(task));
    }

    /// Record “finished”.
    public void onTaskFinished(ModelTask task) {
        upsert(task);
        post(new TaskFinishedEvent(task));
    }

    /// Record “canceled”.
    public void onTaskCancelled(ModelTask task) {
        upsert(task);
        post(new TaskCancelledEvent(task));
    }

    /// Record any other change that should notify observers.
    public void onTaskUpdated(ModelTask task) {
        upsert(task);
        post(new TaskUpdatedEvent(task));
    }

    public void onTaskDeleted(ModelKey taskId) {
        Schedule removed = byId.remove(taskId);
        if (removed != null) {
            DateKey oldDay = bucketDate(removed.getTask().getDeadline());
            if (oldDay != null) {
                removeFromBucket(oldDay, removed.getKey());
            }
        }
        post(new TaskDeletedEvent(taskId));
    }


    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private UpdateInfo upsert(ModelTask task) {
        final ModelKey modelKey = task.getKey();
        Schedule existing = byId.get(modelKey);
        final boolean newlyAdded = existing == null;
        // Track previous bucket (if any) before we mutate/replace
        final DateKey oldDay;
        final ModelTask canonical;
        final boolean isCanonical;
        final boolean isChanged;
        if (newlyAdded) {
            canonical = task;
            isCanonical = false;
            isChanged = true;
            oldDay = null;
            existing = new Schedule(task);
            byId.put(modelKey, existing);
            post(new TaskLoadedEvent(task));
        } else {
            canonical = existing.getTask();
            isCanonical = canonical == task;
            oldDay = bucketDate(canonical.getDeadline());
            if (isCanonical) {
                isChanged = false;
            } else {
                final boolean[] changes = {false};
                final Do undo = canonical.onGlobalChange((key, was, is) -> {
                    changes[0] = true;
                    Log.tryLog(TaskIndex.class, this,
                            "Detected task change in model ", modelKey,
                            "Property " + key, was + " -> " + is);
                });
                canonical.absorb(task);
                undo.done();
                isChanged = changes[0];
            }
        }

        // Post a change event for new + updated tasks
        if (isChanged) {
            canonical.setUpdated(System.currentTimeMillis());
            post(new TaskUpdatedEvent(canonical));
        }

        final Double nextTime = TaskFactory.nextTime(canonical);
        // Reindex buckets: remove from old (if changed) and add to new (if applicable)
        DateKey newDay = bucketDate(nextTime);
        if (oldDay != null && !oldDay.equals(newDay)) {
            removeFromBucket(oldDay, task.getKey());
        }
        if (newDay != null) {
            addToBucket(newDay, existing);
        }

        return new UpdateInfo(modelKey, newlyAdded, task.getUpdated());

    }

    private boolean isFinished(Schedule s) {
        return isFinished(s.getTask());
    }
    private boolean isFinished(ModelTask t) {
        return t.isFinished();
    }

    private boolean isCanceled(Schedule s) {
        return isCanceled(s.getTask());
    }
    private boolean isCanceled(ModelTask t) {
        return t.isCancelled();
    }

    private boolean isActive(Schedule s) {
        return isActive(s.getTask());
    }
    private boolean isActive(ModelTask t) {
        return !isFinished(t) && !isCanceled(t);
    }

    private void post(TaskEvent evt) {
        // Always notify on the main thread for UI safety.
        Gdx.app.postRunnable(() -> {
            for (TaskEventListener l : listeners) {
                try {
                    l.onEvent(evt);
                } catch (Throwable ignored) {
                }
            }
        });
    }


// ------------------------- Bucketing helpers + API -------------------------

    private DateKey bucketDate(Double epochMillis) {
        if (epochMillis == null || epochMillis == 0d) return null;
        TimeComponents now = X_Time.breakdown(X_Time.nowMillis(), ModelSettings.timeZone());
        if (now.getHour() < rolloverHour) {
            now = X_Time.breakdown(now.getEpochMillis() - (rolloverHour * 60 * 60 * 1000L), bucketZone);
        }
        // Note: DateKey equality/hash only use year/dayOfYear
        return DateKey.from(now);
    }

    private void addToBucket(DateKey day, Schedule schedule) {
        // Only bucket tasks with a real deadline; active filter is applied at read-time
        byDay.computeIfAbsent(day, d -> new CopyOnWriteArrayList<>()).addIfAbsent(schedule);
        bucketUpdates.incrementAndGet();
    }

    private void removeFromBucket(DateKey day, ModelKey key) {
        CopyOnWriteArrayList<Schedule> list = byDay.get(day);
        if (list != null) {
            list.removeIf(m -> m.getKey().equals(key));
            bucketUpdates.incrementAndGet();
            if (list.isEmpty()) {
                byDay.remove(day, list);
                // TODO hide missing days
            }
        }
    }

    /// Return tasks for a given day that have deadlines, filtered to active and sorted by deadline time.
    public List<Schedule> getDayWithDeadlines(TimeComponents day) {
        CopyOnWriteArrayList<Schedule> raw = byDay.get(DateKey.from(day));
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<Schedule> out = new ArrayList<>();
        for (Schedule t : raw) {
            try {
                if (isActive(t.getTask())) {
                    Double d = t.getTask().getDeadline();
                    if (d != null && d != 0d) {
                        out.add(t);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        out.sort(Comparator.comparingLong(Schedule::getNextDueMillis));
        return out;
    }


    /// Configure the rollover hour used to bucket deadlines into days (default 4).
    public void setRolloverHour(int hour0to23) {
        this.rolloverHour = Math.max(0, Math.min(23, hour0to23));
    }

    /// Configure the time zone used for day bucketing (default system zone).
    public void setBucketZone(TimeZoneInfo zone) {
        if (zone != null) this.bucketZone = zone;
    }

    /// Diagnostic counters
    public int getRefreshCount() {
        return refreshCount.get();
    }

    public int getBucketUpdateCount() {
        return bucketUpdates.get();
    }

    void awaitRefresh(final long timeout) {
        if (refreshPending.get()) {
            synchronized (refreshPending) {
                try {
                    refreshPending.wait(timeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw X_Fu.rethrow(e);
                }
            }
        }
        if (lastError != null) {
            throw X_Fu.rethrow(lastError);
        }
    }

    public void destroy() {
        autoRefreshTask.cancel();

    }
}