package net.wti.tasks.api.view;

import com.badlogic.gdx.scenes.scene2d.Actor;
import net.wti.ui.demo.api.BasicModelTask;
import net.wti.ui.demo.view.api.IsTaskView;

///
/// IsTaskViewCache:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 28/08/2025 @ 05:17
public interface IsTaskViewCache <M extends BasicModelTask<M>, V extends Actor & IsTaskView<M>> {

    V getOrCreate(M task);

    V getIfPresent(M model);

    void remove(M model);

    void clear();
}
