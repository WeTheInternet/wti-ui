package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.wti.quest.api.LiveQuest;
import net.wti.time.api.ModelDay;
import net.wti.ui.api.GlyphConstants;
import net.wti.ui.api.IsExpandable;
import net.wti.ui.components.SymbolButton;
import net.wti.ui.quest.api.QuestActionHandler;

public class QuestActionBar extends Table {

    private final IsExpandable expandable;
    private final SymbolButton toggleButton;

    public QuestActionBar(
            final Skin skin,
            final IsExpandable expandable,
            final ModelDay day,
            final LiveQuest quest,
            final QuestActionHandler handler
    ) {
        super(skin);
        this.expandable = expandable;

        pad(0);
        defaults().space(2).pad(0f);

        toggleButton = new SymbolButton(
                expandable != null && expandable.isExpanded() ? GlyphConstants.GLYPH_COLLAPSE : GlyphConstants.GLYPH_EXPAND,
                SymbolButton.STYLE_NORMAL,
                skin,
                null
        );

        final SymbolButton finish = new SymbolButton(
                GlyphConstants.GLYPH_FINISH,
                SymbolButton.STYLE_PRIMARY,
                skin,
                null
        );

        final float targetH = 29f;
        final float hToggle = toggleButton.clampedSquare(targetH);
        final float hFinish = finish.clampedSquare(targetH);
        final float tallest = Math.min(targetH, Math.max(hToggle, hFinish));

        toggleButton.addListener(TaskActionBarUtil.click(() -> {
            if (this.expandable != null) {
                this.expandable.toggleExpanded();
                toggleButton.setText(this.expandable.isExpanded() ? GlyphConstants.GLYPH_COLLAPSE : GlyphConstants.GLYPH_EXPAND);
            }
        }));

        finish.addListener(TaskActionBarUtil.click(() -> {
            if (handler != null) {
                handler.complete(day, quest);
            }
        }));

        add(toggleButton).size(hToggle, tallest).pad(0);
        divider(targetH);
        add(finish).size(hFinish, tallest).pad(0);

        getCells().forEach(c -> c.expandX().fillX());
    }

    private void divider(final float barHeight) {
        final Drawable d = getSkin().has("divider-vert", Drawable.class) ? getSkin().getDrawable("divider-vert") : null;
        if (d == null) {
            return;
        }
        final Image img = new Image(d);
        final float h = barHeight * 0.66f;
        add(img).width(2f).height(h).padLeft(2f).padRight(2f);
    }
}
