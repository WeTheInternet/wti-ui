package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.quest.api.LiveQuest;
import net.wti.time.api.ModelDay;
import net.wti.time.api.DayIndex;
import net.wti.ui.quest.api.LiveQuestRowFactory;
import net.wti.ui.quest.api.LiveQuestView;
import net.wti.ui.view.api.BaseViewTable;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.time.api.TimeComponents;
import xapi.time.api.TimeZoneInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// LiveQuestView
///
/// Renders a single day's LiveQuest instances grouped by hour and sorted by:
///  - deadlineMillis (non-zero first, earliest first),
///  - effectivePriority (higher priority first),
///  - then title as a tie-breaker.
///
/// Behavior:
///  - Accepts a ModelDay and a list of LiveQuest instances.
///  - Groups quests into hourly buckets based on deadline time in the day's zone.
///  - Collapses consecutive empty hours into summary rows (similar to DayView).
///  - Uses a LiveQuestRowFactory to render individual rows (customizable).
///
/// Responsibilities:
///  - Layout and grouping only. It does not load data or perform persistence.
///  - Caller is responsible for providing an up-to-date list of LiveQuests.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 03:07
public class DefaultLiveQuestView extends BaseViewTable implements LiveQuestView {

    private final DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("h a");

    private ModelDay modelDay;
    private List<LiveQuest> liveQuests = new ArrayList<>();
    private LiveQuestRowFactory rowFactory;

    private int rolloverHour = 4;
    private boolean hasItems;

    public DefaultLiveQuestView(final Skin skin, final ModelDay day, final Iterable<LiveQuest> quests) {
        this(skin, day, quests, null);
    }

    public DefaultLiveQuestView(
            final Skin skin,
            final ModelDay day,
            final Iterable<LiveQuest> quests,
            final LiveQuestRowFactory factory
    ) {
        super(skin);
        this.modelDay = day;
        setLiveQuests(quests);
        this.rowFactory = factory != null ? factory : new DefaultLiveQuestRowFactory(skin);
    }

    /// Replace the data source for this day (call refresh() afterward).
    public void setLiveQuests(final Iterable<LiveQuest> quests) {
        final List<LiveQuest> list = new ArrayList<>();
        if (quests != null) {
            for (final LiveQuest quest : quests) {
                list.add(quest);
            }
        }
        this.liveQuests = list;
    }

    /// Set the ModelDay being rendered. Does not auto-refresh.
    public void setModelDay(final ModelDay day) {
        this.modelDay = day;
    }

    /// Adjust the “rollover” hour used for bucketing (default 4).
    /// This value is only used when inferring buckets; ModelDay already
    /// encodes the window start/end and zone.
    public void setRolloverHour(final int hour0to23) {
        this.rolloverHour = Math.max(0, Math.min(23, hour0to23));
    }

    public void setRowFactory(final LiveQuestRowFactory factory) {
        if (factory != null) {
            this.rowFactory = factory;
        }
    }

    /// Rebuild the hour-grouped layout from current day + LiveQuests.
    @Override
    public void refresh() {
        clearChildren();
        if (modelDay == null) {
            hasItems = false;
            return;
        }

        add(headerLabel(dayTitle(modelDay))).left().row();

        final List<LiveQuest> sorted = new ArrayList<>(liveQuests);
        sorted.sort(liveQuestComparator());

        final Map<Integer, List<LiveQuest>> byHour = bucketByHour(sorted, modelDay);
        hasItems = !byHour.isEmpty();

        int hour = 0;
        while (hour < 24) {
            if (!byHour.containsKey(hour)) {
                final int start = hour;
                while (hour < 24 && !byHour.containsKey(hour)) {
                    hour++;
                }
                final int end = hour - 1;
                add(emptyHourLabel(collapseTitle(start, end))).left().row();
            } else {
                final List<LiveQuest> items = byHour.get(hour);
                add(hourLabel(formatHour(hour))).left().row();
                for (final LiveQuest quest : items) {
                    add(rowFactory.buildRow(modelDay, quest)).left().row();
                }
                hour++;
            }
        }

        invalidateHierarchy();
    }

    /// @return true if this day currently contains any renderable items.
    public boolean hasItems() {
        return hasItems;
    }

    @Override
    public Actor asActor() {
        return this;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    // ---------------------------------------------------------------------
    // Grouping / sorting helpers
    // ---------------------------------------------------------------------

    protected Comparator<LiveQuest> liveQuestComparator() {
        return (a, b) -> {
            final long da = a.getDeadlineMillis() == null ? 0L : a.getDeadlineMillis();
            final long db = b.getDeadlineMillis() == null ? 0L : b.getDeadlineMillis();

            final boolean aHasDeadline = da > 0L;
            final boolean bHasDeadline = db > 0L;

            if (aHasDeadline && !bHasDeadline) {
                return -1;
            }
            if (!aHasDeadline && bHasDeadline) {
                return 1;
            }
            if (aHasDeadline && bHasDeadline) {
                final int cmpDeadline = Long.compare(da, db);
                if (cmpDeadline != 0) {
                    return cmpDeadline;
                }
            }

            final Integer pa = a.getEffectivePriority();
            final Integer pb = b.getEffectivePriority();
            final int priorityA = pa == null ? 0 : pa;
            final int priorityB = pb == null ? 0 : pb;
            final int cmpPriority = Integer.compare(priorityB, priorityA);
            if (cmpPriority != 0) {
                return cmpPriority;
            }

            final String titleA = safeTitle(a);
            final String titleB = safeTitle(b);
            return titleA.compareToIgnoreCase(titleB);
        };
    }

    protected String safeTitle(final LiveQuest quest) {
        final String liveKey = quest.getLiveKey();
        if (liveKey == null) {
            return "";
        }
        return liveKey;
    }

    protected Map<Integer, List<LiveQuest>> bucketByHour(final List<LiveQuest> quests, final ModelDay day) {
        final Map<Integer, List<LiveQuest>> result = new LinkedHashMap<>();
        final TimeZoneInfo zone = day.zone();

        for (final LiveQuest quest : quests) {
            final Long deadline = quest.getDeadlineMillis();
            if (deadline == null || deadline.longValue() <= 0L) {
                continue;
            }
            final long millis = deadline.longValue();
            if (!day.contains(millis)) {
                continue;
            }
            final TimeComponents components = X_Time.breakdown(millis, zone);
            int hourLocal = components.getHour();
            if (hourLocal < rolloverHour) {
                /// Before rolloverHour, treat as previous calendar day; for this view we skip it.
                continue;
            }
            if (hourLocal < 0) {
                hourLocal = 0;
            } else if (hourLocal > 23) {
                hourLocal = 23;
            }

            List<LiveQuest> bucket = result.get(hourLocal);
            if (bucket == null) {
                bucket = new ArrayList<>();
                result.put(hourLocal, bucket);
            }
            bucket.add(quest);
        }

        return result;
    }

    // ---------------------------------------------------------------------
    // Label / formatting helpers
    // ---------------------------------------------------------------------

    protected String dayTitle(final ModelDay day) {
        final DayIndex index = day.dayIndex();
        final int dayNum = index.getDayNum();

        final TimeComponents now = X_Time.breakdown(X_Time.nowMillis(), day.zone());
        final DayIndex todayIndex = day.dayIndex();
        final int todayNum = todayIndex.getDayNum();

        if (dayNum == todayNum) {
            return "Today";
        }
        if (dayNum == todayNum - 1) {
            return "Yesterday";
        }
        if (dayNum == todayNum + 1) {
            return "Tomorrow";
        }
        return X_String.formatDayOfWeekDate(now.getDayOfWeek(), now.getDayOfMonth());
    }

    protected String collapseTitle(final int start, final int end) {
        if (start > end) {
            return "";
        }
        if (start == end) {
            return formatHour(start);
        }
        return formatHour(start) + " – " + formatHour(end) + " (no items)";
    }

    protected String formatHour(final int hour24) {
        return LocalTime.of(hour24, 0).format(hourFormatter).toLowerCase();
    }

    protected Label headerLabel(final String text) {
        final Label.LabelStyle style = skin.get(Label.LabelStyle.class);
        final Label label = new Label(text, style);
        label.setFontScale(1.05f);
        return label;
    }

    protected Label hourLabel(final String text) {
        final Label.LabelStyle style = skin.get(Label.LabelStyle.class);
        final Label label = new Label(text, style);
        label.setColor(0.8f, 0.8f, 1f, 1f);
        label.setFontScale(0.98f);
        return label;
    }

    protected Label emptyHourLabel(final String text) {
        final Label.LabelStyle style = skin.get(Label.LabelStyle.class);
        final Label label = new Label(text, style);
        label.setColor(0.7f, 0.7f, 0.8f, 1f);
        label.setFontScale(0.92f);
        return label;
    }

}
