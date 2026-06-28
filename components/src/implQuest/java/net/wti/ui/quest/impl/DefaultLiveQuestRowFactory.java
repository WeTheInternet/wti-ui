package net.wti.ui.quest.impl;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import net.wti.ui.quest.api.LiveQuestRowFactory;
import net.wti.ui.quest.api.QuestActionHandler;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.time.api.TimeComponents;

import java.util.Arrays;

import static com.badlogic.gdx.utils.Align.*;

/// DefaultLiveQuestRowFactory
///
/// Basic row implementation for QuestDayView:
///  - Left column: formatted time (or blank if no deadline).
///  - Middle column: title / name (currently best-effort from LiveKey).
///  - Right column: status, skip flag, tags.
///
/// This is deliberately simple and intended as a starting point; callers
/// can provide their own LiveQuestRowFactory for richer UIs.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 03:05
public class DefaultLiveQuestRowFactory implements LiveQuestRowFactory {

    private final Skin skin;
    private final QuestActionHandler handler;

    public DefaultLiveQuestRowFactory(final Skin skin, final QuestActionHandler handler) {
        this.skin = skin;
        this.handler = handler;
    }

    @Override
    public Table buildRow(final ModelDay day, final LiveQuest quest) {
        final Table row = new Table(skin);
        row.defaults().pad(1, 4, 1, 4).left();

        final String timeText = formatTime(day, quest);
        final String titleText = quest.title();
        final String descriptionText = quest.description();

        final Label timeLabel = new Label(timeText, skin.get(Label.LabelStyle.class));
        final Label titleLabel = new Label(titleText, skin.get(Label.LabelStyle.class));
        final Label descriptionLabel = new Label(descriptionText, skin.get(Label.LabelStyle.class));

        timeLabel.setAlignment(left);
        titleLabel.setAlignment(left);
        descriptionLabel.setAlignment(center);

        row.add(timeLabel).width(80).left().padRight(8);
        row.add(titleLabel).growX().left();

        if (handler != null) {
            final TextButton complete = new TextButton("Complete", skin);
            complete.addListener(new ClickListener() {
                @Override
                public void clicked(final InputEvent event, final float x, final float y) {
                    row.remove();
                    handler.complete(day, quest);
                }
            });
            row.add(complete).right().padLeft(8);
        }
        row.row().colspan(3);
        row.add(descriptionLabel);
//        row.background(skin.newDrawable("white", com.badlogic.gdx.graphics.Color.RED));
//        descriptionLabel.getStyle().background = skin.newDrawable("white", Color.GREEN);

        return row;
    }

    protected String formatTime(final ModelDay day, final LiveQuest quest) {
        final Long deadline = quest.getDeadlineMillis();
        if (deadline == null || deadline <= 0L) {
            return "";
        }
        final long millis = deadline;
        final TimeComponents tc = X_Time.breakdown(millis, day.zone());
        // this is currently 24-hour format
        final String t = X_String.formatTime(tc.getHour(), tc.getMinute());
        return t.toLowerCase();
    }

    protected String computeMeta(final LiveQuest quest) {
        final StringBuilder builder = new StringBuilder();

        final QuestStatus status = quest.getStatus();
        if (status != null) {
            builder.append(status.name().toLowerCase());
        } else {
            builder.append("active");
        }

        final Boolean skip = quest.getSkip();
        if (Boolean.TRUE.equals(skip)) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append("skipped");
        }

        final String[] tags = quest.getTags();
        if (tags != null && tags.length > 0) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append("#");
            builder.append(String.join(" #", Arrays.asList(tags)));
        }

        return builder.toString();
    }
}
