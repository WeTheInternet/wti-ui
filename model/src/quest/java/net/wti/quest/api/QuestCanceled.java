package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelKey;

/// QuestCanceled
///
/// dy/{DayNum}/cncl/{LiveKey}
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:53
@IsModel(
        modelType = QuestCanceled.MODEL_QUEST_CANCELED,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface QuestCanceled extends QuestHistoryRecord {

    String MODEL_QUEST_CANCELED = "cncl";

    KeyBuilder KEY_BUILDER_CANCELED =
            KeyBuilder.build(MODEL_QUEST_CANCELED).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(ModelKey dayKey, String liveKey) {
        return KEY_BUILDER_CANCELED.buildKey(liveKey).setParent(dayKey);
    }

}
