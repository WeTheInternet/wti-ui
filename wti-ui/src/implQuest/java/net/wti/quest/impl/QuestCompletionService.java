package net.wti.quest.impl;

import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestCompleted;
import net.wti.quest.api.QuestCompletionStore;
import net.wti.time.api.ModelDay;

/// QuestCompletionService
///
/// Orchestrates completion: persist the LiveQuest as FINISHED, then create a dn record.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 00:00
public class QuestCompletionService {

    private final QuestCompletionStore store;

    public QuestCompletionService(final QuestCompletionStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.store = store;
    }

    public QuestCompleted complete(final ModelDay day, final LiveQuest liveQuest) {
        store.saveCompletedLiveQuest(day, liveQuest);
        return store.createCompletedRecord(day, liveQuest);
    }
}
