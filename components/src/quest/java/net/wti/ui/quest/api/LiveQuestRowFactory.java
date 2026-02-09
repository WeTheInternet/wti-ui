package net.wti.ui.quest.api;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.quest.api.LiveQuest;
import net.wti.time.api.ModelDay;

/// LiveQuestRowFactory
///
/// Strategy interface used by LiveQuestView to build per-row tables.
/// This allows callers to customize how individual LiveQuest instances
/// are rendered without changing the core grouping/sorting logic.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 02:38
public interface LiveQuestRowFactory {

    /// Creates a row for the given LiveQuest within the specified ModelDay window.
    ///
    /// @param day   The ModelDay being rendered.
    /// @param quest The LiveQuest instance to render.
    ///
    /// @return A LibGDX Table representing a single row.
    Table buildRow(ModelDay day, LiveQuest quest);
}
