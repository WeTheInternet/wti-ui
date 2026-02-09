package net.wti.quest.api;


import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelKey;

/// QuestFailed
///
/// dy/{DayNum}/fld/{LiveKey}
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:52
@IsModel(
        modelType = QuestFailed.MODEL_QUEST_FAILED,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface QuestFailed extends QuestHistoryRecord {

    String MODEL_QUEST_FAILED = "fld";

    KeyBuilder KEY_BUILDER_FAILED =
            KeyBuilder.build(MODEL_QUEST_FAILED).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(ModelKey dayKey, String liveKey) {
        return KEY_BUILDER_FAILED.buildKey(liveKey).setParent(dayKey);
    }

    Long getDeadlineAtMillis();
    QuestFailed setDeadlineAtMillis(Long deadlineAt);

    Long getDurationSpentMillis();
    QuestFailed setDurationSpentMillis(Long duration);

    String getFailureReason();
    QuestFailed setFailureReason(String reason);
}
