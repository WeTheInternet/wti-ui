package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelKey;

/// QuestCompleted
///
/// dy/{DayNum}/dn/{LiveKey}
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:52
@IsModel(
        modelType = QuestCompleted.MODEL_QUEST_COMPLETED,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface QuestCompleted extends QuestHistoryRecord {

    String MODEL_QUEST_COMPLETED = "dn";

    KeyBuilder KEY_BUILDER_COMPLETED =
            KeyBuilder.build(MODEL_QUEST_COMPLETED).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(ModelKey dayKey, String liveKey) {
        return KEY_BUILDER_COMPLETED.buildKey(liveKey).setParent(dayKey);
    }

    Long getDeadlineAtMillis();
    QuestCompleted setDeadlineAtMillis(Long deadlineAt);

    Long getDurationSpentMillis();
    QuestCompleted setDurationSpentMillis(Long duration);

    /// Optional serialized completion requirements snapshot (JSON or similar).
    String getCompletionRequirementsSnapshot();
    QuestCompleted setCompletionRequirementsSnapshot(String payload);
}
