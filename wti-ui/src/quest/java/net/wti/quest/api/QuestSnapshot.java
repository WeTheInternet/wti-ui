package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.Model;

/// QuestSnapshot
///
/// Minimal immutable snapshot of definition fields needed to render history
/// without chasing changing definitions.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:51
@IsModel(
        modelType = QuestSnapshot.MODEL_QUEST_SNAPSHOT,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface QuestSnapshot extends Model {

    String MODEL_QUEST_SNAPSHOT = "qsnap";

    String getName();
    QuestSnapshot setName(String name);

    String getDescription();
    QuestSnapshot setDescription(String description);

    String[] getTags();
    QuestSnapshot setTags(String[] tags);

    Integer getPriority();
    QuestSnapshot setPriority(Integer priority);
}
