package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import net.wti.ui.quest.api.LiveQuestRowFactory;
import xapi.string.X_String;
import xapi.time.api.TimeZoneInfo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/// DefaultLiveQuestRowFactory
///
/// Basic row implementation for LiveQuestView:
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
    private final DateTimeFormatter timeFormatter;

    public DefaultLiveQuestRowFactory(final Skin skin) {
        this(skin, DateTimeFormatter.ofPattern("h:mm a"));
    }

    public DefaultLiveQuestRowFactory(final Skin skin, final DateTimeFormatter formatter) {
        this.skin = skin;
        this.timeFormatter = formatter;
    }

    @Override
    public Table buildRow(final ModelDay day, final LiveQuest quest) {
        final Table row = new Table(skin);
        row.defaults().pad(1, 4, 1, 4).left();

        final String timeText = formatTime(day, quest);
        final String titleText = computeTitle(quest);
        final String metaText = computeMeta(quest);

        final Label timeLabel = new Label(timeText, skin.get(Label.LabelStyle.class));
        final Label titleLabel = new Label(titleText, skin.get(Label.LabelStyle.class));
        final Label metaLabel = new Label(metaText, skin.get(Label.LabelStyle.class));

        timeLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        titleLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        metaLabel.setAlignment(com.badlogic.gdx.utils.Align.right);

        row.add(timeLabel).width(80).left().padRight(8);
        row.add(titleLabel).growX().left();
        row.add(metaLabel).width(140).right();

        return row;
    }

    protected String formatTime(final ModelDay day, final LiveQuest quest) {
        final Long deadline = quest.getDeadlineMillis();
        if (deadline == null || deadline.longValue() <= 0L) {
            return "";
        }
        final long millis = deadline.longValue();
        final TimeZoneInfo zoneInfo = day.zone();
        final ZoneId zoneId = ZoneId.of(zoneInfo.getId());
        return Instant.ofEpochMilli(millis)
                .atZone(zoneId)
                .toLocalTime()
                .format(timeFormatter)
                .toLowerCase();
    }

    protected String computeTitle(final LiveQuest quest) {
        /// Placeholder: derive something human-friendly from LiveKey.
        final String liveKey = quest.getLiveKey();
        if (liveKey == null) {
            return "";
        }
        final String[] parts = liveKey.split("/", 2);
        final String definitionId = parts.length > 0 ? parts[0] : liveKey;
        final String ruleId = parts.length > 1 ? parts[1] : "";

        final String base = definitionId.replace('_', ' ');
        if (ruleId.isEmpty()) {
            return X_String.toTitleCase(base);
        }
        return X_String.toTitleCase(base) + " [" + ruleId + "]";
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
