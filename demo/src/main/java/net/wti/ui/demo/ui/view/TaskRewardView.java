package net.wti.ui.demo.ui.view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.inventory.api.ItemType;
import net.wti.ui.inventory.model.Inventory;

/// TaskRewardView:
///
/// A compact, right-aligned coin summary (G/S/C).
/// - Uses a small text style and tight spacing
/// - Hides entirely when there is no reward
///
/// Created by James X. Nelson (James@WeTheInter.net) on 30/08/2025 @ 05:05
public class TaskRewardView extends Table {

    private final ModelTask task;
    private final Skin skin;
    private Label goldLabel;
    private Label silverLabel;
    private Label copperLabel;

    private static final float LABEL_FONT_SCALE = 0.85f;
    private static final float LABEL_SPACING = 2f;

    /**
     * Cached totals for layout/visibility logic
     */
    private int gold, silver, copper;
    private boolean hasReward;

    public TaskRewardView(ModelTask task, Skin skin) {
        this.task = task;
        this.skin = skin;

        /// Build with no internal padding; we’ll add children only when needed
        pad(0);
        defaults().space(2).pad(0);

        /// Initial data pull determines whether to render anything
        updateRewards();

        if (hasReward) {
            buildRow();
        } else {
            /// Hide completely and take no layout space
            setVisible(false);
        }
    }

    /// Recompute reward totals and update UI visibility.
    private void updateRewards() {
        final Inventory rewards = task.getRewards();
        gold = silver = copper = 0;

        // testing
        gold = 1;
        if (rewards != null) {
            gold   = (int)rewards.count(ItemType.GOLD_COIN);
            silver = (int)rewards.count(ItemType.SILVER_COIN);
            copper = (int)rewards.count(ItemType.COPPER_COIN);
        }
        hasReward = gold > 0 || silver > 0 || copper > 0;
    }

    /// Build the compact row once we know we have something to show.
    private void buildRow() {
        clearChildren();

        final Label.LabelStyle small = skin.get("note", Label.LabelStyle.class);

        goldLabel   = new Label(String.valueOf(gold), small);
        silverLabel = new Label(String.valueOf(silver), small);
        copperLabel = new Label(String.valueOf(copper), small);

        /// Tight, right-aligned cluster: 0G 0S 0C
        /// TODO: replace with inventory icons
        if (gold > 0) {
            add(goldLabel).right();
            add(new Label("G", small)).left();
        }
        if (silver > 0) {
            add(silverLabel).right();
            add(new Label("S", small)).left();
        }
        if (copper > 0) {
            add(copperLabel).right();
            add(new Label("C", small)).left();
        }

        setVisible(true);
        invalidateHierarchy();
    }

    /// Public hook to refresh from task in case rewards change.
    public void refresh() {
        updateRewards();
        if (hasReward) {
            buildRow();
        } else {
            clearChildren();
            setVisible(false);
            invalidateHierarchy();
        }
    }

    /// Report zero size when hidden so our parent row doesn’t reserve space.
    @Override public float getPrefWidth() {
        return isVisible() ? super.getPrefWidth() : 0f;
    }
    @Override public float getPrefHeight() {
        return isVisible() ? super.getPrefHeight() : 0f;
    }
}
