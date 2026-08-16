package net.wti.quest.impl

import net.wti.quest.api.ChildRef
import net.wti.quest.api.CompletionPolicy
import net.wti.quest.api.LiveQuest
import net.wti.quest.api.LiveQuestStore
import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.QuestStatus
import net.wti.quest.api.RecurrenceRule
import net.wti.quest.api.SubQuest
import net.wti.time.api.ModelDay
import xapi.model.X_Model
import xapi.model.api.ModelList

// -------------------------------------------------------------------------
// Simple in-memory implementation of LiveQuestStore for tests
// -------------------------------------------------------------------------
class InMemoryLiveQuestStore implements LiveQuestStore {

    final List<LiveQuest> all = new ArrayList<>()

    @Override
    LiveQuest findByDayAndLiveKey(final ModelDay day, final String liveKey) {
        return all.find { it.dayIndex == day.getDayNum() && liveKey == it.liveKey }
    }

    @Override
    LiveQuest createLiveQuest(final ModelDay day, final QuestDefinition definition, final RecurrenceRule rule, final long deadlineMillis, final boolean skip) {
        final LiveQuest lv = X_Model.create(LiveQuest)
        lv.setParentDayKey(ModelDay.newKey(day.getDayNum()))
        lv.setDayIndex(day.getDayNum())

        final String id = QuestKeyUtil.liveKeyFor(definition, rule)
        lv.setKey(LiveQuest.newKey(lv.getParentDayKey(), id))

        lv.setLiveKey(QuestKeyUtil.liveKeyFor(definition, rule))
        lv.setSourceDefinitionKey(definition.key)
        if (rule != null) {
            lv.setSourceRuleKey(rule.key)
        }
        lv.copyAclsFrom(definition)
        lv.copyRequirementsFrom(definition)

        lv.setDeadlineMillis(deadlineMillis)
        lv.setSkip(skip)
        lv.setStatus(QuestStatus.ACTIVE)
        lv.setCreatedAtMillis(System.currentTimeMillis())
        lv.setUpdatedAtMillis(lv.createdAtMillis)

        all.add(lv)

        final ModelList<ChildRef> children = definition.getChildren();
        if (children != null) {
            final int minRequired = definition.minimumRequired
            for (final ChildRef child : children) {
                SubQuest subQuest = X_Model.create(SubQuest.class)
                subQuest.parent = lv
                subQuest.reference = child

                // TODO: compose the child live quests
                switch (definition.completionPolicy) {
                    case CompletionPolicy.ALL_OF:
                    case CompletionPolicy.ANY_OF:
                    case CompletionPolicy.WEIGHTED:
                        break
                }
            }
        }
        return lv
    }

    @Override
    LiveQuest save(final LiveQuest quest) {
        // in-memory: already in list
        return quest
    }
}
