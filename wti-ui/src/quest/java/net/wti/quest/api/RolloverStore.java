package net.wti.quest.api;

import net.wti.time.api.ModelDay;
import xapi.model.api.ModelKey;

import java.util.List;

/// RolloverStore
///
/// Storage abstraction used by RolloverService:
///  - Enumerate active LiveQuest under a given day
///  - Persist QuestFailed history records
///  - Delete LiveQuest instances
///
/// Concrete implementations can use X_Model or another persistence layer.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:48
public interface RolloverStore {

    /// Returns all active LiveQuest instances for the given ModelDay.
    List<LiveQuest> findActiveLiveQuests(ModelDay day);

    /// Persists a QuestFailed history record.
    QuestFailed createFailureRecord(
            LiveQuest liveQuest,
            RolloverContext context,
            String failureReason
    );

    /// Deletes the given LiveQuest instance.
    void deleteLiveQuest(LiveQuest liveQuest);

    /// Returns the ModelKey for a ModelDay.
    default ModelKey dayKey(ModelDay day) {
        return net.wti.time.api.ModelDay.newKey(day.getDayNum());
    }
}
