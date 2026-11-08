package net.wti.quest.impl;

import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.ChildRef;
import net.wti.quest.api.ChildRole;
import net.wti.quest.api.ChildStartPolicy;
import net.wti.quest.api.CompletionPolicy;
import net.wti.quest.api.HasRequirements;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestDefinition;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelService;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.time.X_Time;
import xapi.util.api.SuccessHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/// Explicit read/start operations for direct, parallel ALL_OF checklists.
/// Loading never writes. Starting fills confirmed missing progress and preserves existing
/// terminal records. Callbacks stay on the model service thread; the application dispatches UI work.
/// Available conditional creation preserves racing progress; legacy providers remain non-atomic.
/// This is not an atomic multi-record start or completion transaction.
public final class ChecklistProjectionService {
    private final ModelService models;

    public ChecklistProjectionService(final ModelService models) {
        this.models = Objects.requireNonNull(models, "models");
    }

    /// Browse membership without choosing an owner or starting a run.
    public void loadDefinition(final ModelKey definition, final SuccessHandler<ChecklistSnapshot> callback) {
        final SuccessHandler<ChecklistSnapshot> done = QuestProgressKeys.once(callback);
        inspect(null, definition, QuestProgressKeys.then(done, plan -> done.onSuccess(plan.snapshot())));
    }

    /// Read membership and current progress; missing progress remains unstarted.
    public void load(final ModelDay day, final ModelKey definition,
                     final SuccessHandler<ChecklistSnapshot> callback) {
        final SuccessHandler<ChecklistSnapshot> done = QuestProgressKeys.once(callback);
        if (day == null) {
            QuestProgressKeys.fail(done, new IllegalArgumentException("Explicit owner/day required"));
            return;
        }
        inspect(day, definition, QuestProgressKeys.then(done, plan -> done.onSuccess(plan.snapshot())));
    }

    /// Start only after an explicit user action. Partial acknowledged starts can be retried.
    public void start(final ModelDay day, final ModelKey definition,
                      final SuccessHandler<ChecklistSnapshot> callback) {
        start(day, definition, () -> true, callback);
    }

    /** A disposed run may finish its submitted write, but cannot submit the next write. */
    public void start(final ModelDay day, final ModelKey definition, final BooleanSupplier current,
                      final SuccessHandler<ChecklistSnapshot> callback) {
        final SuccessHandler<ChecklistSnapshot> done = QuestProgressKeys.once(callback);
        if (day == null) {
            QuestProgressKeys.fail(done, new IllegalArgumentException("Explicit owner/day required"));
            return;
        }
        final ModelServiceWithCreateIfAbsent creator;
        try {
            creator = QuestProgressKeys.creator(models);
        } catch (final Throwable failure) {
            QuestProgressKeys.fail(done, failure);
            return;
        }
        inspect(day, definition, QuestProgressKeys.then(done, plan -> {
            final String failure = plan.startFailure();
            if (failure != null) {
                throw new IllegalStateException(failure);
            }
            fill(plan, 0, current, creator, done);
        }));
    }

    void inspect(final ModelDay day, final ModelKey definition, final SuccessHandler<ChecklistPlan> callback) {
        final SuccessHandler<ChecklistPlan> done = QuestProgressKeys.once(callback);
        try {
            QuestProgressKeys.definition(models, definition);
            final ModelKey definitionCopy = QuestProgressKeys.copy(models, definition);
            final ChecklistPlan plan = new ChecklistPlan(models, day == null ? null : QuestProgressKeys.day(models, day),
                    day == null ? 0 : day.getDayNum());
            QuestProgressKeys.optional(models, QuestDefinition.class, definitionCopy, QuestProgressKeys.then(done, root -> {
                if (root == null) {
                    throw new IllegalStateException("Checklist definition is missing");
                }
                supported(root);
                if (root.getCompletionPolicy() != null && root.getCompletionPolicy() != CompletionPolicy.ALL_OF) {
                    throw new UnsupportedOperationException("Only ALL_OF checklists are supported");
                }
                final ModelKey rootLive = plan.day == null ? null : plan.day.getChild(LiveQuest.MODEL_LIVE_QUEST, root.getKey().getId());
                plan.items.add(new ChecklistPlanItem(root, rootLive, 1, root.getTags()));
                final List<ChildRef> children = new ArrayList<>();
                final Set<String> identities = new HashSet<>();
                final Set<String> ids = new HashSet<>();
                if (root.getChildren() != null) {
                    for (final ChildRef child : root.getChildren()) {
                        if (child == null) {
                            throw new IllegalArgumentException("Null checklist member");
                        }
                        QuestProgressKeys.definition(models, child.getDefinitionKey());
                        final String identity = QuestProgressKeys.encoded(models, child.getDefinitionKey());
                        if (QuestProgressKeys.same(models, root.getKey(), child.getDefinitionKey())
                                || !identities.add(identity) || !ids.add(child.getDefinitionKey().getId())) {
                            throw new UnsupportedOperationException("Cyclic or duplicate checklist member");
                        }
                        if ((child.getRole() != null && child.getRole() != ChildRole.PARALLEL)
                                || (child.getStartPolicy() != null && child.getStartPolicy() != ChildStartPolicy.AFTER_PARENT_START)
                                || (child.getQuantity() != null && child.getQuantity() <= 0)) {
                            throw new UnsupportedOperationException(
                                    "Only positive-quantity parallel members starting with the parent are supported");
                        }
                        children.add(child);
                    }
                }
                loadChildren(plan, children, 0, done);
            }));
        } catch (final Throwable failure) {
            QuestProgressKeys.fail(done, failure);
        }
    }

    private void loadChildren(final ChecklistPlan plan, final List<ChildRef> children, final int index,
                              final SuccessHandler<ChecklistPlan> done) {
        if (index == children.size()) {
            loadProgress(plan, 0, done);
            return;
        }
        final ChildRef reference = children.get(index);
        final ModelKey definition = QuestProgressKeys.copy(models, reference.getDefinitionKey());
        QuestProgressKeys.optional(models, QuestDefinition.class, definition, QuestProgressKeys.then(done, child -> {
            if (child == null) {
                throw new IllegalStateException("Checklist member definition is missing");
            }
            supported(child);
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                throw new UnsupportedOperationException("Nested checklist composition is not supported");
            }
            final ModelKey rootLive = plan.items.get(0).key;
            final ModelKey live = rootLive == null ? null : rootLive.getChild(LiveQuest.MODEL_LIVE_QUEST, child.getKey().getId());
            final Set<String> tags = new LinkedHashSet<>();
            if (Boolean.TRUE.equals(reference.getInheritTags())) {
                addTags(tags, plan.items.get(0).definition.getTags());
            }
            addTags(tags, child.getTags());
            addTags(tags, reference.getAdditionalTags());
            plan.items.add(new ChecklistPlanItem(child, live, reference.getQuantity() == null ? 1 : reference.getQuantity(),
                    tags.toArray(new String[0])));
            loadChildren(plan, children, index + 1, done);
        }));
    }

    private void loadProgress(final ChecklistPlan plan, final int index, final SuccessHandler<ChecklistPlan> done) {
        if (plan.day == null || index == plan.items.size()) {
            done.onSuccess(plan);
            return;
        }
        final ChecklistPlanItem item = plan.items.get(index);
        QuestProgressKeys.optional(models, LiveQuest.class, item.key, QuestProgressKeys.then(done, live -> {
            if (live != null) {
                QuestProgressKeys.live(models, live, item.key, plan.day, plan.dayNum, item.definition.getKey());
                supported(live);
            }
            item.live = live;
            loadProgress(plan, index + 1, done);
        }));
    }

    private void fill(final ChecklistPlan plan, final int index, final BooleanSupplier current,
                      final ModelServiceWithCreateIfAbsent creator, final SuccessHandler<ChecklistSnapshot> done) {
        QuestProgressKeys.current(current);
        if (index == plan.items.size()) {
            done.onSuccess(plan.snapshot());
            return;
        }
        final ChecklistPlanItem item = plan.items.get(index);
        if (item.live != null) {
            fill(plan, index + 1, current, creator, done);
            return;
        }
        final ModelKey sourceDefinition = QuestProgressKeys.copy(models, item.definition.getKey());
        final LiveQuest live = models.create(LiveQuest.class);
        live.setKey(QuestProgressKeys.copy(models, item.key));
        live.setParentDayKey(QuestProgressKeys.copy(models, plan.day));
        live.setDayIndex(plan.dayNum);
        live.setLiveKey(item.key.getId());
        live.setSourceDefinitionKey(QuestProgressKeys.copy(models, sourceDefinition));
        live.setTitle(item.definition.getTitle());
        live.setDescription(item.definition.getDescription());
        live.setTags(item.tags == null ? null : item.tags.clone());
        live.setEffectivePriority(item.definition.getPriority());
        live.setDeadlineMillis(0L);
        live.setSkip(false);
        live.setStatus(QuestStatus.ACTIVE);
        final long now = X_Time.nowMillisLong();
        live.setCreatedAtMillis(now);
        live.setUpdatedAtMillis(now);
        QuestProgressKeys.createMissing(models, creator, live, QuestProgressKeys.then(done, saved -> {
            QuestProgressKeys.live(models, saved, item.key, plan.day, plan.dayNum, sourceDefinition);
            supported(saved);
            item.live = saved;
            final String failure = plan.startFailure();
            if (failure != null) {
                throw new IllegalStateException(failure);
            }
            fill(plan, index + 1, current, creator, done);
        }));
    }

    /// This MVP has no requirement/ACL evaluator; never silently ignore configured restrictions.
    private void supported(final HasRequirements model) {
        // A declared empty ACL denies everyone under HasAcl; it is not unrestricted.
        if (model.getToSee() != null || model.getToStart() != null
                || model.getToComplete() != null || model.getToEdit() != null
                || model.getAllowedUsers() != null || model.getAllowedGroups() != null) {
            throw new UnsupportedOperationException("Checklist requirements and ACL rules need an evaluator");
        }
    }

    private static void addTags(final Set<String> tags, final String[] values) {
        if (values != null) {
            for (final String value : values) {
                if (value != null) {
                    tags.add(value);
                }
            }
        }
    }

}
