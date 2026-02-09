package net.wti.quest.impl;


import net.wti.quest.api.*;
import net.wti.time.api.ModelDay;
import net.wti.time.api.TimeAnchor;
import net.wti.time.impl.TimeAnchorUtil;

/// PlannerService
///
/// Core materialization logic for a single (Definition × Rule × ModelDay).
///
/// Responsibilities:
/// - Skip if QuestDefinition or RecurrenceRule are inactive.
/// - Skip if rule.autoMaterialize == false.
/// - Build LiveKey from definition + rule.
/// - If a LiveQuest for (day, LiveKey) already exists, return it unchanged.
/// - Otherwise:
///   - Compute deadlineMillis from rule.anchor + ModelDay window.
///   - Apply supplied skip flag (from schedule template).
///   - Delegate to LiveQuestStore to create and persist a new instance.
///
/// Higher-level orchestration (iterating all defs/rules for a user, etc.)
/// can be built on top of this unit.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:01
public class PlannerService {

    private final LiveQuestStore liveQuestStore;

    public PlannerService(LiveQuestStore liveQuestStore) {
        if (liveQuestStore == null) {
            throw new IllegalArgumentException("liveQuestStore must not be null");
        }
        this.liveQuestStore = liveQuestStore;
    }

    /**
     * Ensures there is at most one LiveQuest for (definition × rule × day).
     *
     * @param day        ModelDay window.
     * @param definition QuestDefinition (not null).
     * @param rule       RecurrenceRule (may be null for manual / ad-hoc).
     * @param skip       Whether this instance should be created with skip=true.
     *
     * @return Existing or newly created LiveQuest, or null when no instance
     *         should be materialized (e.g. inactive/autoMaterialize=false).
     */
    public LiveQuest ensureLiveQuestForDay(
            ModelDay day,
            QuestDefinition definition,
            RecurrenceRule rule,
            boolean skip
    ) {
        if (day == null) {
            throw new IllegalArgumentException("ModelDay must not be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("QuestDefinition must not be null");
        }

        // If the definition itself is inactive, do nothing.
        if (Boolean.FALSE.equals(definition.getActive())) {
            return null;
        }

        // If we have a rule, enforce its flags.
        if (rule != null) {
            if (Boolean.FALSE.equals(rule.getActive())) {
                return null;
            }
            if (Boolean.FALSE.equals(rule.getAutoMaterialize())) {
                return null;
            }
        }

        final String liveKey = QuestKeyUtil.liveKeyFor(definition, rule);

        // If already exists for this day, just return it.
        LiveQuest existing = liveQuestStore.findByDayAndLiveKey(day, liveKey);
        if (existing != null) {
            return existing;
        }

        // Compute deadlineMillis from rule anchor if present; otherwise, 0.
        long deadlineMillis = 0L;
        if (rule != null) {
            final TimeAnchor anchor = rule.getAnchor();
            if (anchor != null) {
                deadlineMillis = TimeAnchorUtil.computeDeadlineMillis(day, anchor);
            }
        }

        LiveQuest created = liveQuestStore.createLiveQuest(
                day,
                definition,
                rule,
                deadlineMillis,
                skip
        );

        // Basic sanity: ensure key fields.
        if (created.getLiveKey() == null) {
            created.setLiveKey(liveKey);
        }
        if (created.getDayIndex() == null) {
            created.setDayIndex(day.getDayNum());
        }
        if (created.getParentDayKey() == null) {
            created.setParentDayKey(liveQuestStore.dayKey(day));
        }
        if (created.getStatus() == null) {
            created.setStatus(QuestStatus.ACTIVE);
        }
        if (created.getSkip() == null) {
            created.setSkip(skip);
        }
        if (created.getSourceDefinitionKey() == null) {
            created.setSourceDefinitionKey(definition.getKey());
        }
        if (rule != null && created.getSourceRuleKey() == null) {
            created.setSourceRuleKey(rule.getKey());
        }

        return liveQuestStore.save(created);
    }
}
