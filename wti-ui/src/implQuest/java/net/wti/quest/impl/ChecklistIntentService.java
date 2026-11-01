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

/** Serial acknowledged checkbox intents for one active board. Browsing never enters this queue.
 * Each intent captures its full owner/day/definition/target identity. Already submitted IO may settle
 * after disposal; the guard is checked before every subsequent write. This is local sequencing,
 * not a cross-device transaction. */
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

    public void complete(final ModelDay day, final ModelKey definition, final ModelKey target,
                         final BooleanSupplier current, final SuccessHandler<ChecklistSnapshot> callback) {
        complete(day, definition, target, current, snapshot -> {}, callback);
    }

    /** Also reports acknowledged child state before attempting ALL_OF ancestor reconciliation. */
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
                    try { outer.onSuccess(value); } finally { next(); }
                }, error -> {
                    try { QuestProgressKeys.fail(outer, error); } finally { next(); }
                }));
                try {
                    QuestProgressKeys.current(current);
                    projection.inspect(captured, root, QuestProgressKeys.then(done, plan -> {
                        QuestProgressKeys.current(current);
                        ChecklistPlan.Item selected = null;
                        for (final ChecklistPlan.Item item : plan.items) {
                            if (QuestProgressKeys.same(models, item.key, leaf)) selected = item;
                        }
                        if (selected == null) throw new IllegalArgumentException("Target is outside this checklist");
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
                        if (plan.hasMissingProgress()) projection.start(captured, root, current, started);
                        else started.onSuccess(plan.snapshot());
                    }));
                } catch (final Throwable failure) { QuestProgressKeys.fail(done, failure); }
            });
        } catch (final Throwable failure) { QuestProgressKeys.fail(outer, failure); }
    }

    private void finishAncestor(final ModelDay day, final BooleanSupplier current,
                                final ChecklistSnapshot snapshot, final SuccessHandler<ChecklistSnapshot> done) {
        QuestProgressKeys.current(current);
        // ALL_OF is the only supported checklist policy. No separate root-confirmation click is needed.
        if (!snapshot.getChildren().isEmpty() && !snapshot.getRoot().isComplete()
                && snapshot.getChildren().stream().allMatch(ChecklistSnapshot.Entry::isComplete)) {
            completion.complete(day, models.keyFromString(snapshot.getRoot().getLiveKey()), current, done);
        } else done.onSuccess(snapshot);
    }

    private void enqueue(final Runnable task) {
        final boolean start;
        synchronized (queue) { queue.add(task); start = !running; if (start) running = true; }
        if (start) task.run();
    }

    private void next() {
        final Runnable task;
        synchronized (queue) { queue.remove(); task = queue.peek(); if (task == null) running = false; }
        if (task != null) task.run();
    }
}
