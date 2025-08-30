package net.wti.ui.view.panes;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Logger;

///
/// ClipGroup:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 30/08/2025 @ 02:45
public final class ClipGroup <W extends Actor & Layout> extends WidgetGroup {
    private final Rectangle tmp = new Rectangle();
    private boolean measureByCurrentSize = false;
    private final Logger log = new Logger("ClipGroup", Logger.INFO);

    public ClipGroup(W child) {
        addActor(child);
        setTransform(false); // required for safe scissor with Table/Stage default camera
        setTouchable(Touchable.childrenOnly);
    }

    public void setMeasureByCurrentSize(boolean measureByCurrentSize) {
        if (this.measureByCurrentSize != measureByCurrentSize) {
            this.measureByCurrentSize = measureByCurrentSize;
            invalidateHierarchy();
            log.debug("measureByCurrentSize=" + measureByCurrentSize + " size=[" + getWidth() + "x" + getHeight() + "]");
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // Optional auto-mode: if we have active actions, prefer measuring by current size.
        boolean hasAnims = getActions().size > 0;
        if (hasAnims != measureByCurrentSize) {
            // Flip only when it helps; avoid flapping logs.
            setMeasureByCurrentSize(hasAnims);
        }
    }

    @Override
    public void layout() {
        if (getChildren().size > 0) {
            // Lay out the child at its natural size; we clip our own bounds when drawing.
            final Actor child = getChildren().first();
            if (child instanceof Layout) {
                Layout label = (Layout) child;
                float w = label.getPrefWidth();
                float h = label.getPrefHeight();
                child.setSize(w, h);
            }
            child.setPosition(0, 0);
        }
        log.debug("layout -> bounds [" + getX() + "," + getY() + " " + getWidth() + "x" + getHeight() + "], pref [" + getPrefWidth() + "x" + getPrefHeight() + "]");
    }

    @Override
    public float getPrefWidth() {
        if (getChildren().size == 0) return 0;
        final Actor child = getChildren().first();
        if (child instanceof Layout) {
            return ((Layout) child).getPrefWidth();
        }
        return child.getWidth();
    }

    @Override
    public float getPrefHeight() {
        // When animating/collapsing, report current height so parent can shrink with the animation.
        if (measureByCurrentSize) return getHeight();
        if (getChildren().size == 0) return 0;
        final Actor child = getChildren().first();
        if (child instanceof Layout) {
            return ((Layout) child).getPrefHeight();
        }
        return child.getHeight();
    }

    @Override
    public float getMinHeight() {
        return 0; // allow collapsing to 0
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Stage stage = getStage();
        if (stage == null) return;

        if (getWidth() <= 0 || getHeight() <= 0) return;

        tmp.set(getX(), getY(), getWidth(), getHeight());
        stage.calculateScissors(tmp, tmp);
        if (ScissorStack.pushScissors(tmp)) {
            super.draw(batch, parentAlpha);
            ScissorStack.popScissors();
        }
        log.debug("draw -> size [" + getWidth() + "x" + getHeight() + "], measureByCurrent=" + measureByCurrentSize);
    }

}
