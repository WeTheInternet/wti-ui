package net.wti.quest.api;

import xapi.model.api.Model;
import xapi.model.api.ModelKey;

/// QuestHistoryRecord
///
/// Common fields for dn/fld/cncl/skp records.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:51
public interface QuestHistoryRecord extends Model {

    /// Key of the LiveQuest that produced this record.
    ModelKey getInstanceKey();
    QuestHistoryRecord setInstanceKey(ModelKey key);

    /// Optional lineage back to definition/rule.
    ModelKey getSourceDefinitionKey();
    QuestHistoryRecord setSourceDefinitionKey(ModelKey key);

    ModelKey getSourceRuleKey();
    QuestHistoryRecord setSourceRuleKey(ModelKey key);

    Long getDayIndex();
    QuestHistoryRecord setDayIndex(Long dayIndex);

    /// When this event occurred (epoch millis).
    Long getOccurredAtMillis();
    QuestHistoryRecord setOccurredAtMillis(Long millis);

    /// Snapshot of definition for stable history rendering.
    QuestSnapshot getSnapshot();
    QuestHistoryRecord setSnapshot(QuestSnapshot snapshot);

    String getNotes();
    QuestHistoryRecord setNotes(String notes);
}
