package net.wti.ui.quest.api;

import net.wti.quest.api.LiveQuest;
import net.wti.time.api.ModelDay;

/// QuestActionHandler
///
/// Callback interface used by TodayView to perform actions on quests.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 00:00
public interface QuestActionHandler {

    void complete(ModelDay day, LiveQuest quest);
}
