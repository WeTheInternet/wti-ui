package net.wti.tasks.api.view;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.tasks.index.TaskIndex;
import net.wti.ui.demo.api.BasicModelTask;
import net.wti.ui.demo.view.api.IsTaskView;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;

///
/// AbstractTaskViewCache:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) and chatgpt on 28/08/2025 @ 05:17
public abstract class AbstractTaskViewCache<M extends BasicModelTask<M>, V extends Actor & IsTaskView<M>> implements IsTaskViewCache<M, V> {

    private final TaskIndex index;
    protected final Skin skin;
    private final MapLike<M, V> cache = X_Jdk.mapWeak();

    protected AbstractTaskViewCache(Skin skin, TaskIndex index) {
        this.skin = skin;
        this.index = index;
    }

    @Override
    public final V getOrCreate(M model) {

        V view = cache.get(model);
        if (view == null) {
            view = create(model);
            cache.put(model, view);
        }
        return view;
    }

    @Override
    public final V getIfPresent(M model) {
        return cache.get(model);
    }

    @Override
    public final void remove(M model) {
        cache.remove(model);
    }

    @Override
    public final void clear() {
        cache.clear();
    }

    protected abstract V create(M model);
}

