package net.wti.quest.spi;

import net.wti.quest.api.QuestDefinition;
import xapi.fu.In2;
import xapi.model.api.ModelKey;

/// NamespacedQuestDefinitionSource
///
/// Async / streaming source that emits definitions in priority order:
/// 1) user namespace
/// 2) group namespaces
/// 3) root namespace
///
/// Lower-priority duplicates (same definition id) are suppressed.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 09:06
public interface NamespacedQuestDefinitionSource {

    void streamDefinitionsForUser(
            ModelKey userKey,
            In2<ModelKey, QuestDefinition> onDefinition,
            Runnable onComplete
    );
}
