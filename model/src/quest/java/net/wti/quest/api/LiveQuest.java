package net.wti.quest.api;

import net.wti.time.api.ModelDay;
import net.wti.time.api.ModelDuration;
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
public interface LiveQuest extends ModelQuest<LiveQuest> {

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

    static String computePath(ModelKey key) {
        if (key == null || key.getKind().equals(ModelDay.MODEL_DAY)) {
            assert key != null : "Null key found";
            return "";
        }
        final ModelKey parentKey = key.getParent();
        final String parentPath = computePath(parentKey);
        if (parentPath.isEmpty()) {
            return key.getId();
        }
        return parentPath + "/" + key.getId();
    }

    default String liveId() {
        return computePath(getKey());
    }

    /// Source definition and rule keys (nullable for manual/live-only quests).
    ModelKey getSourceDefinitionKey();
    LiveQuest setSourceDefinitionKey(ModelKey key);

    ModelKey getSourceRuleKey();
    LiveQuest setSourceRuleKey(ModelKey key);

    /// Absolute deadline; 0 == no deadline.
    Long getDeadlineMillis();
    LiveQuest setDeadlineMillis(Long deadline);

    QuestStatus getStatus();
    default QuestStatus status() {
        final QuestStatus status = getStatus();
        if (status == null) {
            setStatus(QuestStatus.ACTIVE);
            return QuestStatus.ACTIVE;
        }
        return status;
    }
    LiveQuest setStatus(QuestStatus status);

    /// Per-instance alarm override (minutes before deadline).
    ModelDuration getAlarmDuration();
    LiveQuest setAlarmDuration(ModelDuration delay);

    ModelDuration getEstimatedDuration();
    LiveQuest setEstimatedDuration(ModelDuration mins);

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
    default boolean skipped() {
        return Boolean.TRUE.equals(getSkip());
    }
    LiveQuest setSkip(Boolean skip);

    /// Per-instance override; may fall back to definition or user default.
    ModelDuration getGracePeriodDuration();
    LiveQuest setGracePeriodDuration(ModelDuration mins);

    /// For filtering by template at instance level.
    String getScheduleTemplateKey();
    LiveQuest setScheduleTemplateKey(String key);

    default int comparePriority(LiveQuest other) {
        final QuestStatus myStatus = status();
        final QuestStatus yourStatus = other.status();
        if (myStatus != yourStatus) {
            // bucket all sort operations by quest status before priority
            return Integer.compare(myStatus.ordinal(), yourStatus.ordinal());
        }
        final Integer myPrio = getEffectivePriority();
        final Integer yourPrio = other.getEffectivePriority();
        if (yourPrio == null) {
            if (myPrio == null) {
                // neither one has a priority. Try for shortest duration.
                final ModelDuration myDuration = getEstimatedDuration();
                final ModelDuration yourDuration = other.getEstimatedDuration();
                if (myDuration != yourDuration) {
                    if (yourDuration == null) {
                        return -1;
                    }
                    if (myDuration == null) {
                        return 1;
                    }
                    // shorter duration tasks come first.
                    final int result = Long.compare(myDuration.toMillis(), yourDuration.toMillis());
                    if (result != 0) {
                        return result;
                    }
                }

                // with nothing else to prioritize on, use creation time:
                // return the newest created first
                return Long.compare(other.getCreatedAtMillis(), getCreatedAtMillis());
            }
            return -1;
        }
        if (myPrio == null) {
            return 1;
        }
        // higher priority first
        return Integer.compare(yourPrio, myPrio);
    }
}
