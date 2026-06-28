package net.wti.quest.api;

import net.wti.time.api.ModelDuration;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.fu.data.ListLike;
import xapi.fu.data.SetLike;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;

///
/// QuestRequirements:
///
/// A model which generically describes some testable-requirements that other
/// data models can use to implement ACLs / permissions / quest requirements.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/06/2026 @ 20:10
@SuppressWarnings("UnusedReturnValue")
@IsModel(
        modelType = QuestRequirements.MODEL_QUEST_REQUIREMENTS,
        persistence = @Persistent(strategy = PersistenceStrategy.Inline)
)
public interface QuestRequirements extends Model, HasAcl {
    String MODEL_QUEST_REQUIREMENTS = "qreq";

    SetLike<ModelKey> getRequiredQuests();
    default SetLike<ModelKey> requiredQuests() {
        return getOrCreateSet(this::getRequiredQuests, this::setRequiredQuests);
    }
    QuestRequirements setRequiredQuests(SetLike<ModelKey> requirements);

    Integer getMinimumRequired();
    QuestRequirements setMinimumRequired(Integer minimumRequired);


}
