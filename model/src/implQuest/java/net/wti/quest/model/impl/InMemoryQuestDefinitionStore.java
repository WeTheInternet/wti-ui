package net.wti.quest.model.impl;

import net.wti.model.quest.api.QuestDefinitionStore;
import net.wti.quest.api.QuestDefinition;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.java.X_Jdk;
import xapi.fu.data.MapLike;
import xapi.model.api.ModelKey;

///
/// InMemoryQuestDefinitionStore:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 10:04
public class InMemoryQuestDefinitionStore implements QuestDefinitionStore {

    private final MapLike<String, MapLike<String, QuestDefinition>> byNamespace = X_Jdk.mapOrderedInsertion();

    @Override
    public Iterable<QuestDefinition> findDefinitionsInNamespace(final ModelKey namespaceKey) {
        if (namespaceKey == null) {
            return EmptyIterator.none();
        }
        MapLike<String, QuestDefinition> defs = byNamespace.get(namespaceKey.getId());
        return defs == null ? EmptyIterator.none() : defs.mappedValues();
    }

    @Override
    public QuestDefinition findDefinitionInNamespace(final ModelKey namespaceKey, final String definitionId) {
        if (namespaceKey == null || definitionId == null) {
            return null;
        }
        MapLike<String, QuestDefinition> defs = byNamespace.get(namespaceKey.getId());
        return defs == null ? null : defs.get(definitionId);
    }

    public void put(final ModelKey namespaceKey, final QuestDefinition definition) {
        if (namespaceKey == null || definition == null || definition.getKey() == null) {
            return;
        }
        MapLike<String, QuestDefinition> defs = byNamespace.get(namespaceKey.getId());
        if (defs == null) {
            defs = X_Jdk.mapOrderedInsertion();
            byNamespace.put(namespaceKey.getId(), defs);
        }
        defs.put(definition.getKey().getId(), definition);
    }
}
