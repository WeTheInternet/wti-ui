package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.KeyOnly;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;

///
/// SubQuest:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 11/04/2026 @ 22:19
@IsModel(
        modelType = SubQuest.MODEL_SUB_QUEST,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface SubQuest extends LiveQuest {

    String MODEL_SUB_QUEST = "sq";

    @KeyOnly
    ChildRef getReference();
    SubQuest setReference(ChildRef reference);

    @KeyOnly
    LiveQuest getParent();
    SubQuest setParent(LiveQuest parent);
}
