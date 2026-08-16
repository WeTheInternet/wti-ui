package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelKey;

/// QuestSkipped
///
/// dy/{DayNum}/skp/{LiveKey}
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:53
@IsModel(
        modelType = QuestSkipped.MODEL_QUEST_SKIPPED,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface QuestSkipped extends QuestHistoryRecord {

    String MODEL_QUEST_SKIPPED = "skp";

    KeyBuilder KEY_BUILDER_SKIPPED =
            KeyBuilder.build(MODEL_QUEST_SKIPPED).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(ModelKey dayKey, String liveKey) {
        return KEY_BUILDER_SKIPPED.buildKey(liveKey).setParent(dayKey);
    }

}
