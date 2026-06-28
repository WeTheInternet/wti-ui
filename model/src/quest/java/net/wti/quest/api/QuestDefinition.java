package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelList;

/// QuestDefinition
///
/// Canonical quest definition: name, tags, rules, composition, defaults.
/// Instances are projected as LiveQuest per-day.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:48
@IsModel(
        modelType = QuestDefinition.MODEL_QUEST_DEFINITION,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
@SuppressWarnings("UnusedReturnValue")
public interface QuestDefinition extends ModelQuest<QuestDefinition> {

    String MODEL_QUEST_DEFINITION = "qdef";

    KeyBuilder KEY_BUILDER_DEF =
            KeyBuilder.build(MODEL_QUEST_DEFINITION).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(String id) {
        return KEY_BUILDER_DEF.buildKey(id);
    }

    /// Logical priority (higher = more important, 0-100 by convention).
    Integer getPriority();
    QuestDefinition setPriority(Integer priority);

    /// Tags: typeahead, multi-select.
    String[] getTags();
    QuestDefinition setTags(String[] tags);

    /// Which schedule template this definition uses (e.g. "workday").
    String getScheduleTemplateKey();
    QuestDefinition setScheduleTemplateKey(String key);

    /// Recurrence rules (relative, anchor-based).
    RecurrenceRule[] getRules();
    QuestDefinition setRules(RecurrenceRule[] rules);

    /// The children of each quest definition are ChildRef models, which point to
    /// the child quest definition and contain additional metadata for how the given
    /// child reference is to be handled within the context of the owning quest.
    ModelList<ChildRef> getChildren();
    default ModelList<ChildRef> children() {
        return getOrCreateModelList(ChildRef.class, this::getChildren, this::setChildren);
    }
    QuestDefinition setChildren(ModelList<ChildRef> children);

    /// Default alarm relative to deadline (minutes before).
    Integer getDefaultAlarmMinutes();
    QuestDefinition setDefaultAlarmMinutes(Integer mins);

    /// Default grace period in minutes (after deadline before fail).
    Integer getDefaultGracePeriodMinutes();
    QuestDefinition setDefaultGracePeriodMinutes(Integer mins);

    /// Visibility policy (MVP: simple enum).
    String getVisibility();
    QuestDefinition setVisibility(String visibility);

    /// Whether this definition should be auto-materialized.
    /// Null means "true" by default.
    Boolean getAuto();
    /// Convenience helper: auto defaults to true.
    default boolean auto() {
        return !Boolean.FALSE.equals(getAuto());
    }
    QuestDefinition setAuto(Boolean auto);

    /// Whether this definition is currently active.
    Boolean getActive();
    QuestDefinition setActive(Boolean active);

    /// Composition completion policy (parent-level).
    CompletionPolicy getCompletionPolicy();
    QuestDefinition setCompletionPolicy(CompletionPolicy policy);

    int getMinimumRequired();
    QuestDefinition setMinimumRequired(int minimumRequired);
}
