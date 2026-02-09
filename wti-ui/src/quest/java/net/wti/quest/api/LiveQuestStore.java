package net.wti.quest.api;

import net.wti.time.api.ModelDay;
import xapi.model.api.ModelKey;

/// LiveQuestStore
///
/// Storage abstraction used by PlannerService:
/// - find existing LiveQuest for a given day + LiveKey
/// - create a new LiveQuest instance with required fields
///
/// Concrete implementations can use X_Model or any other backing store.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 00:51
public interface LiveQuestStore {

    /**
     * Finds an existing LiveQuest for the given ModelDay + liveKey.
     *
     * @param day     Parent ModelDay (not null).
     * @param liveKey Stable LiveKey string, e.g. "{definitionId}[/{ruleId}]".
     * @return existing LiveQuest or null if none.
     */
    LiveQuest findByDayAndLiveKey(ModelDay day, String liveKey);

    /**
     * Creates and persists a new LiveQuest instance for the given (day, def, rule).
     *
     * @param day            Parent ModelDay.
     * @param definition     Source QuestDefinition.
     * @param rule           Source RecurrenceRule (may be null for manual).
     * @param deadlineMillis Absolute deadline (0 => no deadline).
     * @param skip           Whether this instance should be created as skipped.
     * @return the created LiveQuest.
     */
    LiveQuest createLiveQuest(
            ModelDay day,
            QuestDefinition definition,
            RecurrenceRule rule,
            long deadlineMillis,
            boolean skip
    );

    /**
     * Persists updates to an existing LiveQuest, if needed.
     *
     * For simple stores, this may be a no-op.
     */
    LiveQuest save(LiveQuest quest);

    /**
     * Helper to build the parent day key if the store needs it.
     */
    default ModelKey dayKey(ModelDay day) {
        return ModelDay.newKey(day.getDayNum());
    }
}
