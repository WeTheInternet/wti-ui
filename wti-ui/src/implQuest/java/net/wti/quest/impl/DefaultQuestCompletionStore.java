package net.wti.quest.impl;

import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestCompleted;
import net.wti.quest.api.QuestCompletionStore;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.time.X_Time;

import static xapi.util.api.SuccessHandler.NO_OP;

/// DefaultQuestCompletionStore
///
/// X_Model-backed implementation of QuestCompletionStore.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 00:00
public class DefaultQuestCompletionStore implements QuestCompletionStore {

    @Override
    public LiveQuest saveCompletedLiveQuest(final ModelDay day, final LiveQuest liveQuest) {
        if (day == null) {
            throw new IllegalArgumentException("day must not be null");
        }
        if (liveQuest == null) {
            throw new IllegalArgumentException("liveQuest must not be null");
        }
        if (liveQuest.getKey() == null) {
            throw new IllegalStateException("Cannot save quest without a key: " + liveQuest);
        }

        final long now = X_Time.nowMillisLong();
        liveQuest.setStatus(QuestStatus.FINISHED);
        liveQuest.setFinishedAtMillis(now);
        liveQuest.setUpdatedAtMillis(now);

        X_Model.persist(liveQuest, NO_OP);
        return liveQuest;
    }

    @Override
    public QuestCompleted createCompletedRecord(final ModelDay day, final LiveQuest liveQuest) {
        if (day == null) {
            throw new IllegalArgumentException("day must not be null");
        }
        if (liveQuest == null) {
            throw new IllegalArgumentException("liveQuest must not be null");
        }
        final String liveKey = liveQuest.getLiveKey();
        if (liveKey == null) {
            throw new IllegalArgumentException("liveQuest.liveKey must not be null");
        }

        final ModelKey dayKey = ModelDay.newKey(day.getDayNum());
        final ModelKey completedKey = QuestCompleted.newKey(dayKey, liveKey);

        final QuestCompleted completed = X_Model.create(QuestCompleted.class);
        completed.setKey(completedKey);

        completed.setInstanceKey(liveQuest.getKey());
        completed.setSourceDefinitionKey(liveQuest.getSourceDefinitionKey());
        completed.setSourceRuleKey(liveQuest.getSourceRuleKey());
        completed.setDayIndex((long) day.getDayNum());
        completed.setOccurredAtMillis(X_Time.nowMillisLong());

        completed.setDeadlineAtMillis(liveQuest.getDeadlineMillis());

        X_Model.persist(completed, NO_OP);

        return completed;
    }
    }
