package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

final class TaskActionBarUtil {

    private TaskActionBarUtil() {}

    static ClickListener click(final Runnable r) {
        return new ClickListener() {
            @Override
            public void clicked(final InputEvent event, final float x, final float y) {
                r.run();
            }
        };
    }
}
