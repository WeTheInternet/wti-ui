package net.wti.ui.quest.api;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.quest.api.LiveQuest;
import net.wti.time.api.ModelDay;
import net.wti.ui.view.api.IsView;

import java.util.List;

/// LiveQuestView
///
/// Interface for a LibGDX view that renders a single day's LiveQuest instances.
/// Concrete implementations provide layout and rendering; callers provide:
///  - A ModelDay window,
///  - A collection of LiveQuest instances (already loaded),
///  - An optional LiveQuestRowFactory.
///
/// Responsibilities:
///  - Group / sort / render LiveQuest instances for a given ModelDay.
///  - Expose an Actor that can be added to a Stage.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 03:29
public interface LiveQuestView extends IsView {

    /// Sets the ModelDay being rendered. Does not auto-refresh.
    void setModelDay(ModelDay day);

    /// Replaces the LiveQuest instances rendered for the current day.
    /// Call refresh() afterward to rebuild the layout.
    void setLiveQuests(Iterable<LiveQuest> quests);

    /// Convenience overload to accept a List directly.
    default void setLiveQuestsList(final List<LiveQuest> quests) {
        setLiveQuests(quests);
    }

    /// Adjust the “rollover” hour used for day bucketing (default 4).
    /// This value is only used when inferring buckets; ModelDay already
    /// encodes the window start/end and zone.
    void setRolloverHour(int hour0to23);

    /// Sets the row factory used to build individual LiveQuest rows.
    void setRowFactory(LiveQuestRowFactory factory);

    /// Returns true if this view currently has any renderable items.
    boolean hasItems();

    /// Underlying LibGDX Actor for this view (usually a Table or ScrollPane).
    Actor asActor();

    /// Returns the Skin associated with this view, if any.
    Skin getSkin();
}
