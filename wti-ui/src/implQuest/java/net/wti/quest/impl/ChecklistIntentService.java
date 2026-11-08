package net.wti.quest.impl;

import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelService;
import xapi.util.api.SuccessHandler;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/// Coordinates a user's "complete this checkbox" action across the checklist services.
/// The application session owns one instance for its active board. For example,
/// LifeQuestChecklistSession.completeRequested passes the selected owner/day, root
/// definition and target live key to complete(); its display shows pending state
/// and renders the returned acknowledged snapshot.
///
/// Each queued action reloads the checklist and validates the target. If its root or
/// target has no progress yet, ChecklistProjectionService starts the missing progress.
/// AsyncQuestCompletionService then completes the target. The acknowledged observer
/// receives that child state before we try to finish an ALL_OF root whose members are
/// all complete. If root completion fails, the child remains acknowledged and a later
/// action can retry the root. Only then does the queue move to the next action.
///
/// One queue covers the board's goals so rapid clicks cannot overlap their local
/// start/complete/reconcile sequences. Browsing uses ChecklistProjectionService.load
/// or loadDefinition directly and never writes or enters this queue. Captured keys
/// and day values keep a queued action tied to its original selection; current must
/// become false when that board is replaced, disposed, or switches owner/day. An
/// already submitted write may settle, but the guard stops subsequent work.
///
/// This is an in-memory coordinator, not a durable offline queue or a cross-device
/// transaction. Storage guarantees come from the delegated model/completion service;
/// child and ancestor completion remain separate operations. Callbacks run on the
/// model service's callback thread. The controller must dispatch snapshot/pending/error
/// changes to its owning UI thread and ignore results for a board that is no longer current.
public final class ChecklistIntentService {
    private final ModelService models;
    private final ChecklistProjectionService projection;
    private final AsyncQuestCompletionService completion;
    private final Queue<Runnable> queue = new ArrayDeque<>();
    private boolean running;

    public ChecklistIntentService(final ModelService models) {
        this.models = models;
        projection = new ChecklistProjectionService(models);
        completion = new AsyncQuestCompletionService(models);
    }

    /// Queue one explicit completion action; callback reports the final snapshot or failure.
    /// definition is the checklist's root definition; target is the selected root/member live key.
    public void complete(final ModelDay day, final ModelKey definition, final ModelKey target,
                         final BooleanSupplier current, final SuccessHandler<ChecklistSnapshot> callback) {
        complete(day, definition, target, current, snapshot -> {}, callback);
    }

    /// Also reports acknowledged child state before attempting ALL_OF ancestor reconciliation.
    /// The controller can retain that success if the later root operation fails.
    public void complete(final ModelDay day, final ModelKey definition, final ModelKey target,
                         final BooleanSupplier current, final Consumer<ChecklistSnapshot> acknowledged,
                         final SuccessHandler<ChecklistSnapshot> callback) {
        final SuccessHandler<ChecklistSnapshot> outer = QuestProgressKeys.once(callback);
        try {
            final ModelDay captured = models.create(ModelDay.class);
            captured.setKey(QuestProgressKeys.day(models, day));
            captured.setDayNum(day.getDayNum());
            final ModelKey root = QuestProgressKeys.copy(models, definition);
            final ModelKey leaf = QuestProgressKeys.copy(models, target);
            enqueue(() -> {
                final SuccessHandler<ChecklistSnapshot> done = QuestProgressKeys.once(SuccessHandler.handler(value -> {
                    try {
                        outer.onSuccess(value);
                    } finally {
                        next();
                    }
                }, error -> {
                    try {
                        QuestProgressKeys.fail(outer, error);
                    } finally {
                        next();
                    }
                }));
                try {
                    QuestProgressKeys.current(current);
                    projection.inspect(captured, root, QuestProgressKeys.then(done, plan -> {
                        QuestProgressKeys.current(current);
                        ChecklistPlanItem selected = null;
                        for (final ChecklistPlanItem item : plan.items) {
                            if (QuestProgressKeys.same(models, item.key, leaf)) {
                                selected = item;
                            }
                        }
                        if (selected == null) {
                            throw new IllegalArgumentException("Target is outside this checklist");
                        }
                        final QuestStatus status = selected.live == null ? null : selected.live.getStatus();
                        if (status != null && status != QuestStatus.ACTIVE && status != QuestStatus.FINISHED) {
                            throw new IllegalStateException("This quest is not active or finished");
                        }
                        if (selected == plan.items.get(0) && !plan.childrenFinished()) {
                            throw new IllegalStateException("Complete checklist members first");
                        }
                        final SuccessHandler<ChecklistSnapshot> started = QuestProgressKeys.then(done, ignored -> {
                            QuestProgressKeys.current(current);
                            completion.complete(captured, leaf, current, QuestProgressKeys.then(done,
                                    snapshot -> {
                                        QuestProgressKeys.current(current);
                                        acknowledged.accept(snapshot);
                                        finishAncestor(captured, current, snapshot, done);
                                    }));
                        });
                        // Existing progress can finish even when an unrelated missing member cannot start.
                        if (plan.items.get(0).live == null || selected.live == null) {
                            projection.start(captured, root, current, started);
                        } else {
                            started.onSuccess(plan.snapshot());
                        }
                    }));
                } catch (final Throwable failure) {
                    QuestProgressKeys.fail(done, failure);
                }
            });
        } catch (final Throwable failure) {
            QuestProgressKeys.fail(outer, failure);
        }
    }

    private void finishAncestor(final ModelDay day, final BooleanSupplier current,
                                final ChecklistSnapshot snapshot, final SuccessHandler<ChecklistSnapshot> done) {
        QuestProgressKeys.current(current);
        // ALL_OF is the only supported checklist policy. No separate root-confirmation click is needed.
        if (!snapshot.getChildren().isEmpty() && !snapshot.getRoot().isComplete()
                && snapshot.getChildren().stream().allMatch(ChecklistSnapshot.Entry::isComplete)) {
            completion.complete(day, models.keyFromString(snapshot.getRoot().getLiveKey()), current, done);
        } else {
            done.onSuccess(snapshot);
        }
    }

    private void enqueue(final Runnable task) {
        final boolean start;
        synchronized (queue) {
            queue.add(task);
            start = !running;
            if (start) {
                running = true;
            }
        }
        if (start) {
            task.run();
        }
    }

    private void next() {
        final Runnable task;
        synchronized (queue) {
            queue.remove();
            task = queue.peek();
            if (task == null) {
                running = false;
            }
        }
        if (task != null) {
            task.run();
        }
    }
}
