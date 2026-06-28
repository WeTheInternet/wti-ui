package net.wti.quest.api;

import net.wti.time.api.ModelDay;

/// QuestCompletionStore
///
/// Storage abstraction for completing a LiveQuest:
///  - Create a QuestCompleted history record under the correct day key.
///  - Delete the LiveQuest instance.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 00:00
public interface QuestCompletionStore {

    /// Persist updates to an existing LiveQuest when it has been completed.
    LiveQuest saveCompletedLiveQuest(ModelDay day, LiveQuest liveQuest);

    /// Persist a QuestCompleted record for the given LiveQuest under the provided day.
    QuestCompleted createCompletedRecord(ModelDay day, LiveQuest liveQuest);
}
