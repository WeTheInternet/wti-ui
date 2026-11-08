package net.wti.quest.impl;

import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestDefinition;
import xapi.model.api.ModelKey;

/// One definition and its acknowledged progress inside a single ChecklistPlan.
/// This working entry has no stored identity or wire representation of its own.
/// The services replace live after a read/write acknowledgment; snapshot() copies
/// rendering values before they leave the plan. Tags are captured independently.
final class ChecklistPlanItem {
    final QuestDefinition definition;
    final ModelKey key;
    final int quantity;
    final String[] tags;
    LiveQuest live;

    ChecklistPlanItem(final QuestDefinition definition, final ModelKey key,
                      final int quantity, final String[] tags) {
        this.definition = definition;
        this.key = key;
        this.quantity = quantity;
        this.tags = tags == null ? null : tags.clone();
    }
}
