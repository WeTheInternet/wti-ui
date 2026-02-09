package net.wti.quest.impl

import net.wti.quest.api.LiveQuest
import net.wti.quest.api.LiveQuestStore
import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.QuestStatus
import net.wti.quest.api.RecurrenceRule
import net.wti.time.api.ModelDay
import xapi.model.X_Model

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
        lv.setLiveKey(QuestKeyUtil.liveKeyFor(definition, rule))
        lv.setSourceDefinitionKey(definition.key)
        if (rule != null) {
            lv.setSourceRuleKey(rule.key)
        }
        lv.setDeadlineMillis(deadlineMillis)
        lv.setSkip(skip)
        lv.setStatus(QuestStatus.ACTIVE)
        lv.setCreatedAtMillis(System.currentTimeMillis())
        lv.setUpdatedAtMillis(lv.createdAtMillis)

        all.add(lv)
        return lv
    }

    @Override
    LiveQuest save(final LiveQuest quest) {
        // in-memory: already in list
        return quest
    }
}
