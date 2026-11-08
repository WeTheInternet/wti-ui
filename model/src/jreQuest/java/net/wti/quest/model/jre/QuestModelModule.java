package net.wti.quest.model.jre;

import net.wti.quest.api.ChildRef;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestCompleted;
import net.wti.quest.api.QuestDefinition;
import net.wti.quest.api.QuestRequirements;
import net.wti.quest.api.QuestSnapshot;
import net.wti.quest.api.RecurrenceRule;
import net.wti.time.api.ModelDay;
import net.wti.time.api.ModelDuration;
import net.wti.time.api.TimeAnchor;
import xapi.jre.model.ModelServiceJre;
import xapi.model.api.Model;
import xapi.model.api.ModelList;
import xapi.model.api.ModelModule;

/// One explicitly exported serialization policy for the server and its model clients.
/// Ship the resulting ModelSerializerDefault artifact to Android; do not independently reflect it there.
public final class QuestModelModule {

    public static final String NAME = "lifequest-quests";

    private QuestModelModule() {
    }

    /// Registers definition, live/history and embedded model closure in a fixed order.
    public static ModelModule create(final ModelServiceJre models) {
        final ModelModule module = new ModelModule();
        module.setModuleName(NAME);
        add(module, models, ModelList.class);
        add(module, models, ChildRef.class);
        add(module, models, ModelDuration.class);
        add(module, models, TimeAnchor.class);
        add(module, models, RecurrenceRule.class);
        add(module, models, QuestRequirements.class);
        add(module, models, QuestDefinition.class);
        add(module, models, LiveQuest.class);
        add(module, models, QuestSnapshot.class);
        add(module, models, QuestCompleted.class);
        add(module, models, ModelDay.class);
        return module;
    }

    private static void add(
            final ModelModule module, final ModelServiceJre models, final Class<? extends Model> type
    ) {
        models.register(type);
        module.addManifest(models.getOrMakeModelManifest(type));
    }
}
