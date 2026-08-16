package net.wti.quest.impl

import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.RecurrenceRule
import net.wti.quest.api.ScheduleTemplateService
import net.wti.time.api.ModelDay

class InMemoryScheduleTemplateService implements ScheduleTemplateService {
    /// key format: defId:ruleId
    final Set<String> skippedPairs = new HashSet<>()

    @Override
    boolean shouldSkip(final ModelDay day, final QuestDefinition questDefinition, final RecurrenceRule rule) {
        if (questDefinition == null || rule == null) {
            return false
        }
        final String defId = questDefinition.key.id.toString()
        final String key = defId + ":" + rule.ruleId
        return skippedPairs.contains(key)
    }
}
