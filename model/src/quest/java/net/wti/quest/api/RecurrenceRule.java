package net.wti.quest.api;

import net.wti.time.api.ModelDuration;
import net.wti.time.api.TimeAnchor;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;

/// RecurrenceRule
///
/// Relative cadence + anchor used to materialize LiveQuest instances.
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:45
@IsModel(
        modelType = RecurrenceRule.MODEL_RECURRENCE_RULE,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface RecurrenceRule extends Model {

    String MODEL_RECURRENCE_RULE = "qrule";

    KeyBuilder KEY_BUILDER_RULE =
            KeyBuilder.build(MODEL_RECURRENCE_RULE).withType(ModelKey.KEY_TYPE_STRING);

    static ModelKey newKey(String id) {
        return KEY_BUILDER_RULE.buildKey(id);
    }

    /// Stable key within a QuestDefinition (you can also just use ModelKey identity).
    String getRuleId();
    RecurrenceRule setRuleId(String id);

    /// Parent QuestDefinition (lineage).
    ModelKey getParentDefinitionKey();
    RecurrenceRule setParentDefinitionKey(ModelKey key);

    /// Cadence (amount + unit).
    ModelDuration getCadence();
    RecurrenceRule setCadence(ModelDuration cadence);

    /// Anchor within the ModelDay window.
    TimeAnchor getAnchor();
    RecurrenceRule setAnchor(TimeAnchor anchor);

    /// Optional active range (epoch millis).
    Long getActiveRangeStartMillis();
    RecurrenceRule setActiveRangeStartMillis(Long start);

    Long getActiveRangeEndMillis();
    RecurrenceRule setActiveRangeEndMillis(Long end);

    /// If false, do not auto-materialize; require manual start.
    Boolean getAutoMaterialize();
    RecurrenceRule setAutoMaterialize(Boolean auto);

    /// Whether this rule is currently active.
    Boolean getActive();
    RecurrenceRule setActive(Boolean active);
}
