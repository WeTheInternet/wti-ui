package net.wti.quest.impl;

import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestCompleted;
import net.wti.quest.api.QuestSnapshot;
import net.wti.quest.api.QuestStatus;
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
/// History is stored beside the live record (same parent/id/key type, leaf kind dn).
/// A child therefore retains its checklist ancestry; no flattened path loses ownership.
/// The history acknowledgment precedes the terminal live write. This is recoverable
/// single-writer sequencing, not a transaction or cross-device compare-and-set operation.
public final class AsyncQuestCompletionService {
    private final ModelService models;
    private final ChecklistProjectionService projection;
    private final Set<String> pending = new HashSet<>();

    public AsyncQuestCompletionService(final ModelService models) {
        this.models = Objects.requireNonNull(models, "models");
        projection = new ChecklistProjectionService(models);
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
            final ModelKey parent = key.getParent();
            final ModelKey rootKey;
            if (QuestProgressKeys.same(models, parent, dayKey)) rootKey = key;
            else if (parent != null && LiveQuest.MODEL_LIVE_QUEST.equals(parent.getKind())
                    && QuestProgressKeys.same(models, parent.getParent(), dayKey)) rootKey = parent;
            else throw new IllegalArgumentException("Live quest must belong to the supplied day and direct checklist");
            QuestProgressKeys.optional(models, LiveQuest.class, rootKey, QuestProgressKeys.then(done, root -> {
                if (root == null) throw new IllegalStateException("Checklist has not been started");
                projection.inspect(day, root.getSourceDefinitionKey(), QuestProgressKeys.then(done, plan -> {
                    ChecklistPlan.Item target = null;
                    for (final ChecklistPlan.Item item : plan.items) {
                        if (QuestProgressKeys.same(models, item.key, key)) target = item;
                    }
                    if (target == null || target.live == null) throw new IllegalStateException("Checklist member has not been started");
                    if (target == plan.items.get(0) && !plan.childrenFinished()) {
                        throw new IllegalStateException("Complete every checklist member before completing the list");
                    }
                    QuestProgressKeys.current(current);
                    finish(plan, target, current, done);
                }));
            }));
        } catch (final Throwable error) { QuestProgressKeys.fail(done, error); }
    }

    private void finish(final ChecklistPlan plan, final ChecklistPlan.Item item,
                        final BooleanSupplier currentRun, final SuccessHandler<ChecklistSnapshot> done) {
        final LiveQuest current = item.live;
        final QuestStatus status = current.getStatus();
        if (status != null && status != QuestStatus.ACTIVE && status != QuestStatus.FINISHED) {
            throw new IllegalStateException("This quest is not active or finished");
        }
        final ModelKey historyKey = QuestProgressKeys.copy(models, item.key.getParent())
                .getChild(QuestCompleted.MODEL_QUEST_COMPLETED, item.key.getId()).setKeyType(item.key.getKeyType());
        QuestProgressKeys.optional(models, QuestCompleted.class, historyKey, QuestProgressKeys.then(done, history -> {
            if (history != null) {
                validateHistory(plan, item, historyKey, history);
                finishLive(plan, item, history, currentRun, done);
                return;
            }
            final Long finished = current.getFinishedAtMillis();
            if (status == QuestStatus.FINISHED && (finished == null || finished < 0)) {
                throw new IllegalStateException("Finished quest has no usable completion timestamp");
            }
            final long when = status == QuestStatus.FINISHED ? finished : X_Time.nowMillisLong();
            final QuestCompleted created = models.create(QuestCompleted.class);
            created.setKey(historyKey);
            created.setInstanceKey(QuestProgressKeys.copy(models, item.key));
            created.setSourceDefinitionKey(QuestProgressKeys.copy(models, current.getSourceDefinitionKey()));
            created.setSourceRuleKey(current.getSourceRuleKey());
            created.setDayIndex((long) plan.dayNum);
            created.setOccurredAtMillis(when);
            created.setDeadlineAtMillis(current.getDeadlineMillis());
            final QuestSnapshot snapshot = models.create(QuestSnapshot.class);
            snapshot.setName(current.getTitle());
            snapshot.setDescription(current.getDescription());
            snapshot.setTags(current.getTags() == null ? null : current.getTags().clone());
            snapshot.setPriority(current.getEffectivePriority());
            created.setSnapshot(snapshot);
            QuestProgressKeys.current(currentRun);
            QuestProgressKeys.save(models, created, QuestProgressKeys.then(done, saved -> {
                validateHistory(plan, item, historyKey, saved);
                finishLive(plan, item, saved, currentRun, done);
            }));
        }));
    }

    private void validateHistory(final ChecklistPlan plan, final ChecklistPlan.Item item,
                                 final ModelKey expected, final QuestCompleted history) {
        if (!QuestProgressKeys.same(models, expected, history.getKey())
                || !QuestProgressKeys.same(models, item.key, history.getInstanceKey())
                || !QuestProgressKeys.same(models, item.live.getSourceDefinitionKey(), history.getSourceDefinitionKey())
                || !QuestProgressKeys.same(models, item.live.getSourceRuleKey(), history.getSourceRuleKey())
                || !Objects.equals((long) plan.dayNum, history.getDayIndex())
                || history.getOccurredAtMillis() == null || history.getOccurredAtMillis() < 0
                || history.getSnapshot() == null) {
            throw new IllegalStateException("Existing completion history has different identity or lineage");
        }
        if (item.live.getStatus() == QuestStatus.FINISHED
                && !Objects.equals(item.live.getFinishedAtMillis(), history.getOccurredAtMillis())) {
            throw new IllegalStateException("Live quest and history disagree on completion time");
        }
    }

    private void finishLive(final ChecklistPlan plan, final ChecklistPlan.Item item,
                            final QuestCompleted history, final BooleanSupplier currentRun,
                            final SuccessHandler<ChecklistSnapshot> done) {
        QuestProgressKeys.current(currentRun);
        if (item.live.getStatus() == QuestStatus.FINISHED) { done.onSuccess(plan.snapshot()); return; }
        // The copy shares untouched nested values only; every changed field/key is detached.
        final LiveQuest staged = models.create(LiveQuest.class);
        staged.absorb(item.live);
        staged.setKey(QuestProgressKeys.copy(models, item.key));
        staged.setParentDayKey(QuestProgressKeys.copy(models, plan.day));
        staged.setStatus(QuestStatus.FINISHED);
        staged.setFinishedAtMillis(history.getOccurredAtMillis());
        staged.setUpdatedAtMillis(history.getOccurredAtMillis());
        QuestProgressKeys.save(models, staged, QuestProgressKeys.then(done, saved -> {
            QuestProgressKeys.live(models, saved, item.key, plan.day, plan.dayNum, item.definition.getKey());
            if (saved.getStatus() != QuestStatus.FINISHED
                    || !Objects.equals(saved.getFinishedAtMillis(), history.getOccurredAtMillis())) {
                throw new IllegalStateException("Persist did not acknowledge completed state");
            }
            item.live = saved;
            done.onSuccess(plan.snapshot());
        }));
    }
}
