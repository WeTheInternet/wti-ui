package net.wti.quest.impl

import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.QuestDefinitionSource
import xapi.model.api.ModelKey

class InMemoryQuestDefinitionSource implements QuestDefinitionSource {
    Iterable<QuestDefinition> definitions = Collections.emptyList()

    @Override
    Iterable<QuestDefinition> findDefinitionsForUser(final ModelKey userKey) {
        return definitions
    }
}
