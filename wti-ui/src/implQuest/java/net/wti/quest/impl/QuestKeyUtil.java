package net.wti.quest.impl;

import net.wti.quest.api.QuestDefinition;
import net.wti.quest.api.RecurrenceRule;

/// QuestKeyUtil
///
/// Helpers for building stable LiveKey strings for LiveQuest instances.
///
/// LiveKey format:
///   {definitionId}[/{ruleId}]
///
/// - definitionId comes from QuestDefinition.getKey().getIdAsString()
///   (or any other convention you prefer).
/// - ruleId comes from RecurrenceRule.getRuleId().
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 00:50
public final class QuestKeyUtil {

    private QuestKeyUtil() {
        // utility
    }

    public static String liveKeyFor(QuestDefinition def, RecurrenceRule rule) {
        if (def == null) {
            throw new IllegalArgumentException("QuestDefinition must not be null");
        }
        if (def.getKey() == null) {
            throw new IllegalStateException("QuestDefinition.getKey() must not be null");
        }

        final String defId = def.getKey().getId().toString();
        final String ruleId = rule == null ? null : rule.getRuleId();

        if (ruleId == null || ruleId.isEmpty()) {
            return defId;
        }
        return defId + "/" + ruleId;
    }
}
