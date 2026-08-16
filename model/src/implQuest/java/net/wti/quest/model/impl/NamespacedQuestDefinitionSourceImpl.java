package net.wti.quest.model.impl;

import net.wti.model.quest.api.QuestDefinitionStore;
import net.wti.model.user.core.UserGroupStore;
import net.wti.quest.api.QuestDefinition;
import net.wti.quest.spi.NamespacedQuestDefinitionSource;
import xapi.fu.In2;
import xapi.fu.java.X_Jdk;
import xapi.fu.data.SetLike;
import xapi.fu.log.Log;
import xapi.model.api.ModelKey;

public class NamespacedQuestDefinitionSourceImpl implements NamespacedQuestDefinitionSource {

    private final QuestDefinitionStore definitionStore;
    private final UserGroupStore userGroupStore;
    private final ModelKey rootNamespace;

    public NamespacedQuestDefinitionSourceImpl(
            final QuestDefinitionStore definitionStore,
            final UserGroupStore userGroupStore,
            final ModelKey rootNamespace
    ) {
        this.definitionStore = definitionStore;
        this.userGroupStore = userGroupStore;
        this.rootNamespace = rootNamespace;
    }

    @Override
    public void streamDefinitionsForUser(
            final ModelKey userKey,
            final In2<ModelKey, QuestDefinition> onDefinition,
            final Runnable onComplete
    ) {
        final SetLike<String> seenDefinitionIds = X_Jdk.setLinked();

        // 1) user namespace
        streamNamespace(userKey, onDefinition, seenDefinitionIds);

        // 2) group namespaces
        for (ModelKey groupNs : userGroupStore.findGroupNamespacesForUser(userKey)) {
            streamNamespace(groupNs, onDefinition, seenDefinitionIds);
        }

        // 3) root namespace
        streamNamespace(rootNamespace, onDefinition, seenDefinitionIds);

        if (onComplete != null) {
            onComplete.run();
        }
    }

    private void streamNamespace(
            final ModelKey namespace,
            final In2<ModelKey, QuestDefinition> onDefinition,
            final SetLike<String> seenDefinitionIds
    ) {
        if (namespace == null) {
            return;
        }
        final Iterable<QuestDefinition> defs = definitionStore.findDefinitionsInNamespace(namespace);
        if (defs == null) {
            Log.tryLog(NamespacedQuestDefinitionSourceImpl.class, this, Log.LogLevel.INFO,
                    "No quest definitions found for namespace", namespace);
            return;
        }
        for (QuestDefinition def : defs) {
            if (def == null || def.getKey() == null || def.getKey().getId() == null) {
                continue;
            }
            final String id = def.getKey().getId();
            if (seenDefinitionIds.contains(id)) {
                continue;
            }
            seenDefinitionIds.add(id);
            onDefinition.in(namespace, def);
        }
    }
}
