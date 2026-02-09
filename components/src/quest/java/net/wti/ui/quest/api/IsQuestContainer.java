package net.wti.ui.quest.api;

import net.wti.quest.api.LiveQuest;
import net.wti.ui.view.api.IsView;

///
/// IsQuestContainer:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 09/02/2026 @ 02:46
public interface IsQuestContainer extends IsView {

    /// Replaces the LiveQuest instances rendered for the current day.
    /// Call refresh() afterward to rebuild the layout.
    void setLiveQuests(Iterable<LiveQuest> quests);

    /// Returns true if this view currently has any renderable items.
    boolean hasItems();

}
