package net.wti.ui.quest.impl;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import net.wti.ui.api.IsExpandable;
import net.wti.ui.components.IsSkinnable;
import net.wti.ui.quest.api.QuestActionHandler;

import java.util.Arrays;

public class LiveQuestRowView extends Table implements IsExpandable, IsSkinnable {

    private final Skin skin;
    private final ModelDay day;
    private final LiveQuest quest;

    private boolean expanded;
    private final Table details;

    public LiveQuestRowView(
            final Skin skin,
            final ModelDay day,
            final LiveQuest quest,
            final String timeText,
            final String titleText,
            final QuestActionHandler handler
    ) {
        super(skin);
        this.skin = skin;
        this.day = day;
        this.quest = quest;

        final Drawable bg = skin.has("button", Drawable.class)
                ? skin.getDrawable("button")
                : (skin.has("panel-actionbar", Drawable.class) ? skin.getDrawable("panel-actionbar") : null);
        if (bg != null) {
            setBackground(bg);
        }

        defaults().pad(4, 8, 4, 8).left();

        final Label.LabelStyle titleStyle = skin.has("task-name", Label.LabelStyle.class)
                ? skin.get("task-name", Label.LabelStyle.class)
                : skin.get(Label.LabelStyle.class);
        final Label.LabelStyle previewStyle = skin.has("task-preview", Label.LabelStyle.class)
                ? skin.get("task-preview", Label.LabelStyle.class)
                : skin.get(Label.LabelStyle.class);

        final Label timeLabel = new Label(timeText == null ? "" : timeText, previewStyle);
        timeLabel.setAlignment(Align.left);

        final Label titleLabel = new Label(titleText == null ? "" : titleText, titleStyle);
        titleLabel.setAlignment(Align.left);

        details = new Table(skin);
        details.defaults().left();

        final Label metaLabel = new Label(computeDetailsText(quest), previewStyle);
        metaLabel.setAlignment(Align.left);
        metaLabel.setColor(new Color(0.8f, 0.8f, 0.85f, 1f));
        details.add(metaLabel).left().row();

        final Table center = new Table(skin);
        center.defaults().left();
        center.add(titleLabel).left().row();
        center.add(details).left().growX().row();

        setExpanded(false);

        final QuestActionHandler uiFirst = handler == null ? null : new QuestActionHandler() {
            @Override
            public void complete(final ModelDay d, final LiveQuest q) {
                LiveQuestRowView.this.remove();
                handler.complete(d, q);
            }
        };

        final QuestActionBar actionBar = new QuestActionBar(skin, this, day, quest, uiFirst);

        add(timeLabel).width(80).left().padRight(8);
        add(center).growX().left();
        add(actionBar).right();
    }

    private void setExpanded(final boolean expanded) {
        this.expanded = expanded;
        details.setVisible(expanded);
        details.setTransform(false);
        invalidateHierarchy();
    }

    private static String computeDetailsText(final LiveQuest quest) {
        final StringBuilder b = new StringBuilder();

        final QuestStatus status = quest.getStatus();
        if (status != null && status != QuestStatus.ACTIVE) {
            b.append(status.name().toLowerCase());
        }

        if (quest.skipped()) {
            if (b.length() > 0) {
                b.append(" · ");
            }
            b.append("skipped");
        }

        final String[] tags = quest.getTags();
        if (tags != null && tags.length > 0) {
            if (b.length() > 0) {
                b.append(" · ");
            }
            b.append("#");
            b.append(String.join(" #", Arrays.asList(tags)));
        }

        return b.toString();
    }


    @Override
    public void toggleExpanded() {
        setExpanded(!expanded);
    }

    @Override
    public void expand() {
        setExpanded(true);
    }

    @Override
    public void collapse() {
        setExpanded(false);
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }
}
