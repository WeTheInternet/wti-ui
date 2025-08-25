package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import net.wti.ui.demo.api.BasicModelTask;
import net.wti.ui.demo.view.api.IsTaskView;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;

/// TaskCache:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 30/04/2025 @ 00:56
public class TaskCache<M extends BasicModelTask<M>, V extends Actor & IsTaskView<M>> {
    private final MapLike<IsTaskView<M>, Cell<V>> map = X_Jdk.mapWeak();

    public void add(final V actor, final Cell<V> cell) {
        map.put(actor, cell);
    }
}
