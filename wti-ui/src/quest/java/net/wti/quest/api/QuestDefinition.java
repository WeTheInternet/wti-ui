package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;

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
public interface QuestDefinition extends Model {

    String MODEL_QUEST_DEFINITION = "qdef";

    KeyBuilder KEY_BUILDER_DEF =
            KeyBuilder.build(MODEL_QUEST_DEFINITION).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(String id) {
        return KEY_BUILDER_DEF.buildKey(id);
    }

    /// Human-visible name / title.
    String getName();
    QuestDefinition setName(String name);

    /// Optional description (markdown/text).
    String getDescription();
    QuestDefinition setDescription(String description);

    /// Logical priority (higher = more important, or your own convention).
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

    /// Definition-level composition.
    ChildRef[] getComposition();
    QuestDefinition setComposition(ChildRef[] composition);

    /// Default alarm relative to deadline (minutes before).
    Integer getDefaultAlarmMinutes();
    QuestDefinition setDefaultAlarmMinutes(Integer mins);

    /// Default grace period in minutes (after deadline before fail).
    Integer getDefaultGracePeriodMinutes();
    QuestDefinition setDefaultGracePeriodMinutes(Integer mins);

    /// Visibility policy (MVP: simple enum).
    String getVisibility();
    QuestDefinition setVisibility(String visibility);

    /// Whether this definition is currently active.
    Boolean getActive();
    QuestDefinition setActive(Boolean active);

    /// Composition completion policy (parent-level).
    ParentCompletionPolicy getCompletionPolicy();
    QuestDefinition setCompletionPolicy(ParentCompletionPolicy policy);

}
