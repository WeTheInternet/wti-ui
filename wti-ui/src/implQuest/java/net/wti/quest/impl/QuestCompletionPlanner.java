package net.wti.quest.impl;

import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestCompleted;
import net.wti.quest.api.QuestSnapshot;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelService;
import xapi.util.api.SuccessHandler;

import java.util.Objects;
import java.util.function.LongSupplier;
import net.wti.quest.api.QuestCompletionResult;

/// Read-only preparation shared by legacy sequencing and the server snapshot read session.
/// Uses load/allocation/key codec only; never persists, queries or serializes models.
public final class QuestCompletionPlanner {
    private final ModelService models;
    private final ChecklistProjectionService projection;
    private final LongSupplier clock;

    public QuestCompletionPlanner(final ModelService models, final LongSupplier clock) {
        this.models = Objects.requireNonNull(models, "models");
        this.clock = Objects.requireNonNull(clock, "clock");
        projection = new ChecklistProjectionService(models);
    }

    public void prepare(final ModelDay day, final ModelKey liveKey, final SuccessHandler<Plan> callback) {
        final SuccessHandler<Plan> done = QuestProgressKeys.once(callback);
        try {
            final ModelKey dayKey = QuestProgressKeys.day(models, day);
            final ModelKey key = QuestProgressKeys.copy(models, liveKey);
            if (!LiveQuest.MODEL_LIVE_QUEST.equals(key.getKind())) {
                throw new IllegalArgumentException("Expected live quest key");
            }
            final ModelDay captured = models.create(ModelDay.class);
            captured.setKey(dayKey);
            captured.setDayNum(day.getDayNum());
            final ModelKey parent = key.getParent();
            final ModelKey rootKey;
            if (QuestProgressKeys.same(models, parent, dayKey)) {
                rootKey = key;
            } else if (parent != null && LiveQuest.MODEL_LIVE_QUEST.equals(parent.getKind())
                    && QuestProgressKeys.same(models, parent.getParent(), dayKey)) {
                rootKey = parent;
            } else {
                throw new IllegalArgumentException("Live quest must belong to the supplied day and direct checklist");
            }
            QuestProgressKeys.optional(models, LiveQuest.class, rootKey, QuestProgressKeys.then(done, root -> {
                if (root == null) {
                    throw new IllegalStateException("Checklist has not been started");
                }
                projection.inspect(captured, root.getSourceDefinitionKey(), QuestProgressKeys.then(done, plan -> {
                    ChecklistPlanItem target = null;
                    for (final ChecklistPlanItem item : plan.items) {
                        if (QuestProgressKeys.same(models, item.key, key)) {
                            target = item;
                        }
                    }
                    if (target == null || target.live == null) {
                        throw new IllegalStateException("Checklist member has not been started");
                    }
                    if (target == plan.items.get(0) && !plan.childrenFinished()) {
                        throw new IllegalStateException("Complete every checklist member before completing the list");
                    }
                    stage(plan, target, done);
                }));
            }));
        } catch (final Throwable error) {
            QuestProgressKeys.fail(done, error);
        }
    }

    private void stage(final ChecklistPlan plan, final ChecklistPlanItem item, final SuccessHandler<Plan> done) {
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
                done.onSuccess(new Plan(plan, item, history, false));
                return;
            }
            final Long finished = current.getFinishedAtMillis();
            if (status == QuestStatus.FINISHED && (finished == null || finished < 0)) {
                throw new IllegalStateException("Finished quest has no usable completion timestamp");
            }
            final long when = status == QuestStatus.FINISHED ? finished : clock.getAsLong();
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
            validateHistory(plan, item, historyKey, created);
            done.onSuccess(new Plan(plan, item, created, true));
        }));
    }

    private void validateHistory(final ChecklistPlan plan, final ChecklistPlanItem item,
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

    public final class Plan {
        private final ChecklistPlan plan;
        private final ChecklistPlanItem item;
        private final QuestCompleted history;
        private final LiveQuest live;
        private final boolean historyWrite, liveWrite;
        private Plan(final ChecklistPlan plan, final ChecklistPlanItem item, final QuestCompleted history,
                     final boolean historyWrite) {
            this.plan = plan;
            this.item = item;
            this.history = history;
            this.historyWrite = historyWrite;
            liveWrite = item.live.getStatus() != QuestStatus.FINISHED;
            live = models.create(LiveQuest.class);
            live.absorb(item.live);
            live.setKey(QuestProgressKeys.copy(models, item.key));
            live.setParentDayKey(QuestProgressKeys.copy(models, plan.day));
            live.setSourceDefinitionKey(QuestProgressKeys.copy(models, item.live.getSourceDefinitionKey()));
            live.setTags(item.live.getTags() == null ? null : item.live.getTags().clone());
            if (liveWrite) {
                live.setStatus(QuestStatus.FINISHED);
                live.setFinishedAtMillis(history.getOccurredAtMillis());
                live.setUpdatedAtMillis(history.getOccurredAtMillis());
            }
        }
        public LiveQuest getLive() {
            return live;
        }
        public QuestCompleted getHistory() {
            return history;
        }
        public boolean needsHistoryWrite() {
            return historyWrite;
        }
        public boolean needsLiveWrite() {
            return liveWrite;
        }
        public Plan withAcknowledgedHistory(final QuestCompleted acknowledged) {
            validateHistory(plan, item, history.getKey(), acknowledged);
            return new Plan(plan, item, acknowledged, false);
        }
        public ChecklistSnapshot acknowledgedSnapshot(final LiveQuest acknowledged) {
            QuestProgressKeys.live(models, acknowledged, item.key, plan.day, plan.dayNum, item.definition.getKey());
            projection.supported(acknowledged);
            if (acknowledged.getStatus() != QuestStatus.FINISHED
                    || !Objects.equals(acknowledged.getFinishedAtMillis(), history.getOccurredAtMillis())) {
                throw new IllegalStateException("Completion did not acknowledge terminal state");
            }
            item.live = acknowledged;
            return plan.snapshot();
        }
    }

    /// Complete full-key validation before transport IO; day identity remains explicit, not a stored model read.
    public static void validateTarget(final ModelService models, final ModelDay day, final ModelKey target) {
        final ModelKey dayKey = QuestProgressKeys.day(models, day);
        final ModelKey key = QuestProgressKeys.copy(models, target);
        final ModelKey parent = key.getParent();
        if (!LiveQuest.MODEL_LIVE_QUEST.equals(key.getKind()) || parent == null
                || !(QuestProgressKeys.same(models, parent, dayKey)
                || LiveQuest.MODEL_LIVE_QUEST.equals(parent.getKind())
                && QuestProgressKeys.same(models, parent.getParent(), dayKey))) {
            throw new IllegalArgumentException("Completion is outside the captured day");
        }
    }

    /// Validate a detached atomic response before transport retry state or rendering can advance.
    public static void validateAcknowledgment(final ModelService models, final ModelDay day, final ModelKey target,
                                              final QuestCompletionResult result) {
        final ModelKey dayKey = QuestProgressKeys.day(models, day);
        final ModelKey key = QuestProgressKeys.copy(models, target);
        if (!QuestProgressKeys.encoded(models, dayKey).equals(result.getCommand().getDayKey())
                || !QuestProgressKeys.encoded(models, key).equals(result.getCommand().getLiveKey())) {
            throw new IllegalStateException("Completion acknowledged another command");
        }
        validateTarget(models, day, key);
        final ModelKey parent = key.getParent();
        final LiveQuest live = result.getLive();
        final QuestCompleted history = result.getHistory();
        QuestProgressKeys.definition(models, live.getSourceDefinitionKey());
        QuestProgressKeys.live(models, live, key, dayKey, day.getDayNum(), live.getSourceDefinitionKey());
        new ChecklistProjectionService(models).supported(live);
        final ModelKey historyKey = QuestProgressKeys.copy(models, parent)
                .getChild(QuestCompleted.MODEL_QUEST_COMPLETED, key.getId()).setKeyType(key.getKeyType());
        if (!LiveQuest.MODEL_LIVE_QUEST.equals(live.getType())
                || !QuestCompleted.MODEL_QUEST_COMPLETED.equals(history.getType())
                || !QuestProgressKeys.same(models, historyKey, history.getKey())
                || !QuestProgressKeys.same(models, key, history.getInstanceKey())
                || !QuestProgressKeys.same(models, live.getSourceDefinitionKey(), history.getSourceDefinitionKey())
                || !QuestProgressKeys.same(models, live.getSourceRuleKey(), history.getSourceRuleKey())
                || !Objects.equals((long) day.getDayNum(), history.getDayIndex())
                || live.getStatus() != QuestStatus.FINISHED || live.getFinishedAtMillis() == null
                || live.getFinishedAtMillis() < 0 || history.getSnapshot() == null
                || !Objects.equals(live.getFinishedAtMillis(), history.getOccurredAtMillis())) {
            throw new IllegalStateException("Completion acknowledged inconsistent history/live state");
        }
    }
}
