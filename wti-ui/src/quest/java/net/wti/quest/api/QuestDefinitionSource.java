package net.wti.quest.api;

import xapi.model.api.ModelKey;

/// QuestDefinitionSource
///
/// Abstraction used by TodayPlannerService to obtain the set of quest
/// definitions to materialize for a given user.
///
/// Concrete implementations can query X_Model, remote services, etc.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 01:43
public interface QuestDefinitionSource {

    /// Returns all QuestDefinitions that should be considered for materialization
    /// for the given user.
    ///
    /// Implementations are free to apply additional filters (e.g. active only).
    Iterable<QuestDefinition> findDefinitionsForUser(ModelKey userKey);

}
