package net.wti.quest.impl;

import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.QuestAtomicCompletion;
import net.wti.quest.api.LiveQuest;
import net.wti.time.api.ModelDay;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelService;
import xapi.time.X_Time;
import xapi.util.api.SuccessHandler;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/// Acknowledged checklist completion with stable-key retry reconciliation.
/// Atomic providers commit history/live together. Legacy history is stored beside the live record (same parent/id/key type, leaf kind dn).
/// A child therefore retains its checklist ancestry; no flattened path loses ownership.
/// The history acknowledgment precedes the terminal live write. This is recoverable
/// single-writer sequencing, not a transaction or cross-device compare-and-set operation.
public final class AsyncQuestCompletionService {
    private final ModelService models;
    private final QuestCompletionPlanner planner;
    private final Set<String> pending = new HashSet<>();

    public AsyncQuestCompletionService(final ModelService models) {
        this.models = Objects.requireNonNull(models, "models");
        planner = new QuestCompletionPlanner(models, X_Time::nowMillisLong);
    }

    /// Reload current records, enforce ALL_OF, then complete without mutating caller models.
    /// Success returns an acknowledged rendering snapshot; callbacks require caller UI dispatch.
    public void complete(final ModelDay day, final ModelKey liveKey,
                         final SuccessHandler<ChecklistSnapshot> callback) {
        complete(day, liveKey, () -> true, callback);
    }

    public void complete(final ModelDay day, final ModelKey liveKey, final BooleanSupplier current,
                         final SuccessHandler<ChecklistSnapshot> callback) {
        final SuccessHandler<ChecklistSnapshot> outer = QuestProgressKeys.once(callback);
        final String identity;
        final ModelKey key, dayKey;
        try {
            dayKey = QuestProgressKeys.day(models, day);
            key = QuestProgressKeys.copy(models, liveKey);
            identity = QuestProgressKeys.encoded(models, key);
            if (!LiveQuest.MODEL_LIVE_QUEST.equals(key.getKind())) throw new IllegalArgumentException("Expected live quest key");
            synchronized (pending) {
                if (!pending.add(identity)) throw new IllegalStateException("Completion is already pending");
            }
        } catch (final Throwable error) { QuestProgressKeys.fail(outer, error); return; }
        final SuccessHandler<ChecklistSnapshot> done = QuestProgressKeys.once(SuccessHandler.handler(value -> {
            synchronized (pending) { pending.remove(identity); }
            outer.onSuccess(value);
        }, error -> {
            synchronized (pending) { pending.remove(identity); }
            QuestProgressKeys.fail(outer, error);
        }));
        try {
            final QuestAtomicCompletion atomic = models instanceof QuestAtomicCompletion
                    && ((QuestAtomicCompletion) models).supportsAtomicCompletion() ? (QuestAtomicCompletion) models : null;
            final ModelDay captured = models.create(ModelDay.class);
            captured.setKey(dayKey); captured.setDayNum(day.getDayNum());
            QuestCompletionPlanner.validateTarget(models, captured, key);
            if (atomic != null) {
                QuestProgressKeys.current(current);
                atomic.complete(QuestProgressKeys.copy(models, dayKey), QuestProgressKeys.copy(models, key),
                        QuestProgressKeys.then(done, result -> {
                            QuestCompletionPlanner.validateAcknowledgment(models, captured, key, result);
                            QuestProgressKeys.current(current);
                            // Authoritative read-only reload constructs existing rendering values, never an optimistic checkbox.
                            planner.prepare(captured, key, QuestProgressKeys.then(done, prepared -> {
                                QuestProgressKeys.current(current);
                                final QuestCompletionPlanner.Plan acknowledged = prepared.withAcknowledgedHistory(result.getHistory());
                                done.onSuccess(acknowledged.acknowledgedSnapshot(result.getLive()));
                            }));
                        }));
            } else {
                planner.prepare(captured, key, QuestProgressKeys.then(done, prepared -> {
                    QuestProgressKeys.current(current);
                    if (prepared.needsHistoryWrite()) {
                        QuestProgressKeys.save(models, prepared.getHistory(), QuestProgressKeys.then(done, history ->
                                finishLive(prepared.withAcknowledgedHistory(history), current, done)));
                    } else finishLive(prepared, current, done);
                }));
            }
        } catch (final Throwable error) { QuestProgressKeys.fail(done, error); }
    }

    private void finishLive(final QuestCompletionPlanner.Plan plan, final BooleanSupplier current,
                            final SuccessHandler<ChecklistSnapshot> done) {
        QuestProgressKeys.current(current);
        if (plan.needsLiveWrite()) {
            QuestProgressKeys.save(models, plan.getLive(), QuestProgressKeys.then(done, saved -> {
                QuestProgressKeys.current(current);
                done.onSuccess(plan.acknowledgedSnapshot(saved));
            }));
        } else done.onSuccess(plan.acknowledgedSnapshot(plan.getLive()));
    }
}
