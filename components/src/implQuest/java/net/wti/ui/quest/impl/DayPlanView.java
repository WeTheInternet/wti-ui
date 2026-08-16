package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.DurationUnit;
import net.wti.time.api.ModelDay;
import net.wti.time.api.ModelDuration;
import net.wti.ui.quest.api.LiveQuestRowFactory;
import net.wti.ui.quest.api.QuestActionHandler;
import net.wti.ui.quest.api.QuestDayView;
import net.wti.ui.view.api.BaseViewTable;
import xapi.fu.log.Log;
import xapi.string.X_String;
import xapi.time.X_Time;
import xapi.time.api.TimeComponents;
import xapi.time.api.TimeZoneInfo;

import java.util.*;
import java.util.stream.Collectors;

/// DayPlanView
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
public class DayPlanView extends BaseViewTable implements QuestDayView {

    private ModelDay modelDay;
    private List<LiveQuest> liveQuests = new ArrayList<>();
    private LiveQuestRowFactory rowFactory;

    private int rolloverHour = 4;
    private boolean hasItems;

    public DayPlanView(final Skin skin, final ModelDay day, final Iterable<LiveQuest> quests) {
        this(skin, day, quests, null);
    }

    public DayPlanView(
            final Skin skin,
            final ModelDay day,
            final Iterable<LiveQuest> quests,
            final LiveQuestRowFactory factory
    ) {
        super(skin);
        this.modelDay = day;
        setLiveQuests(quests);
        this.rowFactory = factory != null ? factory : new DefaultLiveQuestRowFactory(skin, new QuestActionHandler() {
            @Override
            public void complete(final ModelDay day, final LiveQuest quest) {
                Log.tryLog(DayPlanView.class, DayPlanView.this, "Completed quest", quest);
            }
        });
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

        final List<LiveQuest> sorted = new ArrayList<>();
        for (final LiveQuest q : liveQuests) {
            if (q == null) {
                continue;
            }
            if (q.getStatus() == net.wti.quest.api.QuestStatus.FINISHED) {
                continue;
            }
            sorted.add(q);
        }
        sorted.sort(liveQuestComparator());

        final Map<Integer, List<LiveQuest>> byHour = bucketByHour(sorted, modelDay);
        hasItems = !byHour.isEmpty();
        final List<LiveQuest> noDeadline = byHour.remove(-1);

        final int maxHourExclusive = 24 + Math.max(0, Math.min(23, rolloverHour));

        int hour = 0;
        while (hour < maxHourExclusive) {
            if (!byHour.containsKey(hour)) {
                final int start = hour;
                while (hour < maxHourExclusive && !byHour.containsKey(hour)) {
                    hour++;
                }
                final int end = hour - 1;
                add(emptyHourLabel(collapseTitle(start, end))).left().row();
                final List<LiveQuest> toShow = selectNoDeadlines(noDeadline, start, end);
                for (final LiveQuest quest : toShow) {
                    add(rowFactory.buildRow(modelDay, quest)).left().row();
                    // TODO: detect when lots of items added and a new hour is needed
                }

            } else {
                final List<LiveQuest> items = byHour.get(hour);
                add(hourLabel(formatHour(hour))).left().row();
                for (final LiveQuest quest : items) {
                    add(rowFactory.buildRow(modelDay, quest)).left().row();
                }
                hour++;
            }
        }
        // TODO: consider the no-deadline items _somewhere_

        invalidateHierarchy();
    }

    private List<LiveQuest> selectNoDeadlines(final List<LiveQuest> noDeadline, final int start, final int end) {
        final List<LiveQuest> selected = noDeadline.stream()
                .filter(lq -> {
                    if (lq.skipped()) {
                        return false;
                    }
                    return lq.status() == QuestStatus.ACTIVE;
                })
                .sorted(LiveQuest::comparePriority)
                .collect(Collectors.toList());
        final List<LiveQuest> results = new ArrayList<>();
        double allowed = (1 + end - start) * X_Time.ONE_HOUR;
        for (LiveQuest lq : selected) {
            ModelDuration duration = lq.getEstimatedDuration();
            if (duration == null) {
                duration = ModelDuration.duration(1, DurationUnit.HOUR);
            }
            final long millis = duration.toMillis();
            if (allowed >= millis) {
                results.add(lq);
                allowed -= millis;
            }
            if (allowed <= 0) {
                break;
            }
        }
        return results;
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

    protected static Comparator<LiveQuest> liveQuestComparator() {
        return LiveQuest::comparePriority;
    }

    protected Map<Integer, List<LiveQuest>> bucketByHour(final List<LiveQuest> quests, final ModelDay day) {
        final Map<Integer, List<LiveQuest>> result = new LinkedHashMap<>();
        final TimeZoneInfo zone = day.zone();

        for (final LiveQuest quest : quests) {
            final Long deadline = quest.getDeadlineMillis();
            if (deadline == null || deadline.longValue() <= 0L) {
                List<LiveQuest> bucket = result.computeIfAbsent(-1, k -> new ArrayList<>());
                bucket.add(quest);
                continue;
            }
            final long millis = deadline.longValue();
            if (!day.contains(millis)) {
                continue;
            }

            final TimeComponents components = X_Time.breakdown(millis, zone);
            int hourLocal = components.getHour();

            if (hourLocal < 0) {
                hourLocal = 0;
            } else if (hourLocal > 23) {
                hourLocal = 23;
            }

            /// Policy C: hours before rolloverHour are still in this ModelDay,
            /// but are rendered at the end of the day as 24+hour buckets.
            final int bucketHour = hourLocal < rolloverHour ? 24 + hourLocal : hourLocal;

            List<LiveQuest> bucket = result.computeIfAbsent(bucketHour, k -> new ArrayList<>());
            bucket.add(quest);
        }

        return result;
    }

    // ---------------------------------------------------------------------
    // Label / formatting helpers
    // ---------------------------------------------------------------------

    protected String dayTitle(final ModelDay day) {
        final long nowMillis = X_Time.nowMillisLong();
        if (day.contains(nowMillis)) {
            return "Today";
        }
        final TimeComponents start = day.startComponents();
        /// Avoid incorrect Yesterday/Tomorrow guesses inside a view.
        /// If callers need relative naming, pass that in explicitly later.
        return X_String.formatDayOfWeekDate(start.getDayOfWeek(), start.getDayOfMonth());
    }

    protected String collapseTitle(final int start, final int end) {
        if (start > end) {
            return "";
        }
        if (start == end) {
            return formatHour(start);
        }
        return formatHour(start) + " – " + formatHour(end);
    }

    protected String formatHour(final int hourIndex) {
        /// For planner-style views we support rollover-extension hours (24..24+rolloverHour-1).
        /// Policy C requires literal 25:00-style labels for those late buckets.
        if (hourIndex >= 24) {
            return hourIndex + ":00";
        }
        final String t = X_String.formatTime(hourIndex, 0);
        return t == null ? "" : t.toLowerCase();
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
