package net.wti.quest.api;


import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;

/// LiveQuest
///
/// Active instance for a specific (Definition × Rule × DayIndex).
/// Keyed under parent ModelDay:
///   parent: ModelDay.newKey(dayNum)
///   type: "lv"
///   id:   LiveKey (e.g. "{DefinitionKey}[/{RuleKey}]")
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:49
@IsModel(
        modelType = LiveQuest.MODEL_LIVE_QUEST,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface LiveQuest extends Model {

    String MODEL_LIVE_QUEST = "lv";

    KeyBuilder KEY_BUILDER_LIVE =
            KeyBuilder.build(MODEL_LIVE_QUEST).withType(ModelKey.KEY_TYPE_STRING);

    /// Build a LiveQuest key splayed under a ModelDay.
    static ModelKey newKey(ModelKey dayKey, String liveId) {
        return KEY_BUILDER_LIVE.buildKey(liveId).setParent(dayKey);
    }

    /// Convenience for building the "LiveKey" portion ({DefinitionKey}[/{RuleKey}]).
    static String liveKey(String definitionId, String ruleIdOrNull) {
        return ruleIdOrNull == null || ruleIdOrNull.isEmpty()
                ? definitionId
                : definitionId + "/" + ruleIdOrNull;
    }

    /// Parent day; should match getDayKey().
    ModelKey getParentDayKey();
    LiveQuest setParentDayKey(ModelKey key);

    /// Cached DayIndex for convenience.
    Integer getDayIndex();
    LiveQuest setDayIndex(Integer dayIndex);

    /// LiveKey id ({definition}[/{rule}]).
    String getLiveKey();
    LiveQuest setLiveKey(String liveKey);

    /// Source definition and rule keys (nullable for manual/live-only quests).
    ModelKey getSourceDefinitionKey();
    LiveQuest setSourceDefinitionKey(ModelKey key);

    ModelKey getSourceRuleKey();
    LiveQuest setSourceRuleKey(ModelKey key);

    /// Absolute deadline; 0 == no deadline.
    Long getDeadlineMillis();
    LiveQuest setDeadlineMillis(Long deadline);

    QuestStatus getStatus();
    LiveQuest setStatus(QuestStatus status);

    /// Per-instance alarm override (minutes before deadline).
    Integer getAlarmMinutes();
    LiveQuest setAlarmMinutes(Integer mins);

    /// Absolute snooze-until timestamp (epoch millis), optional.
    Long getSnoozeUntilMillis();
    LiveQuest setSnoozeUntilMillis(Long snooze);

    Long getCreatedAtMillis();
    LiveQuest setCreatedAtMillis(Long created);

    Long getUpdatedAtMillis();
    LiveQuest setUpdatedAtMillis(Long updated);

    Long getStartedAtMillis();
    LiveQuest setStartedAtMillis(Long started);

    Long getFinishedAtMillis();
    LiveQuest setFinishedAtMillis(Long finished);

    Integer getEffectivePriority();
    LiveQuest setEffectivePriority(Integer priority);

    /// Tags copied from definition on creation; updates may propagate.
    String[] getTags();
    LiveQuest setTags(String[] tags);

    /// True when day is off via schedule template or ad-hoc.
    Boolean getSkip();
    LiveQuest setSkip(Boolean skip);

    /// Per-instance override; may fall back to definition or user default.
    Integer getGracePeriodMinutes();
    LiveQuest setGracePeriodMinutes(Integer mins);

    /// For filtering by template at instance level.
    String getScheduleTemplateKey();
    LiveQuest setScheduleTemplateKey(String key);
}
