package net.wti.model.quest.api;

import net.wti.quest.api.QuestDefinition;
import xapi.model.api.ModelKey;

///
/// QuestDefinitionStore:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 09:44
public interface QuestDefinitionStore {

    Iterable<QuestDefinition> findDefinitionsInNamespace(ModelKey namespaceKey);

    QuestDefinition findDefinitionInNamespace(ModelKey namespaceKey, String definitionId);

}