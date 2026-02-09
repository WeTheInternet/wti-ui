package net.wti.ui.quest.sample;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestStatus;
import net.wti.time.api.ModelDay;
import net.wti.time.impl.DayIndexService;
import net.wti.time.impl.ModelDayService;
import net.wti.ui.quest.impl.DefaultLiveQuestView;
import net.wti.ui.sample.AbstractSampleApp;
import net.wti.ui.demo.theme.TaskUiTheme;
import xapi.model.X_Model;
import xapi.time.X_Time;
import xapi.time.api.TimeZoneInfo;

import java.util.ArrayList;
import java.util.List;

/// LiveQuestDemoApp
///
/// Concrete sample app that:
///  - Creates a synthetic ModelDay for "today" in system zone.
///  - Seeds a handful of LiveQuest instances with different deadlines,
///    priorities, and skip/status flags.
///  - Renders them using DefaultLiveQuestView inside a ScrollPane.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 03:43
public class LiveQuestDemoApp extends AbstractSampleApp {

    private TaskUiTheme theme;

    @Override
    protected Skin createSkin() {
        theme = new TaskUiTheme();
        theme.applyTooltipDefaults();
        return theme.getSkin();
    }

    @Override
    protected void createContent(final Stage stage, final Skin skin) {
        final ModelDay today = createTodayModelDay();
        final List<LiveQuest> quests = seedSampleQuests(today);

        final DefaultLiveQuestView view = new DefaultLiveQuestView(skin, today, quests);
        view.refresh();

        final ScrollPane scroller = new ScrollPane(view, skin);
        scroller.setFadeScrollBars(false);
        scroller.setScrollingDisabled(false, false);

        stage.addActor(scroller);
        scroller.setFillParent(true);
        scroller.invalidateHierarchy();
    }

    protected ModelDay createTodayModelDay() {
        final TimeZoneInfo zone = X_Time.systemZone();
        final int rolloverHour = 4;
        final double now = X_Time.nowMillis();

        final DayIndexService indexService = new DayIndexService(zone);
        final ModelDayService dayService = new ModelDayService(indexService);

        final ModelDay day = dayService.getOrCreateModelDay(now, zone, rolloverHour);
        return day;
    }

    protected List<LiveQuest> seedSampleQuests(final ModelDay day) {
        final List<LiveQuest> list = new ArrayList<>();

        final long start = day.startTimestamp();
        final long hourMillis = 60L * 60L * 1000L;

        list.add(createSampleQuest(day, "questA/morning", start + 2L * hourMillis, 10, false, "work", "focus"));
        list.add(createSampleQuest(day, "questB/afternoon", start + 8L * hourMillis, 5, false, "admin"));
        list.add(createSampleQuest(day, "questC/evening", start + 13L * hourMillis, 7, false, "health"));
        list.add(createSampleQuest(day, "questD/late", start + 20L * hourMillis, 1, true, "offday"));
        list.add(createSampleQuest(day, "questE/no_deadline", 0L, 3, false, "flex"));

        return list;
    }

    protected LiveQuest createSampleQuest(
            final ModelDay day,
            final String liveKey,
            final long deadlineMillis,
            final int priority,
            final boolean skip,
            final String... tags
    ) {
        final LiveQuest quest = X_Model.create(LiveQuest.class);
        quest.setParentDayKey(ModelDay.newKey(day.getDayNum()));
        quest.setDayIndex(day.getDayNum());
        quest.setLiveKey(liveKey);
        quest.setKey(LiveQuest.newKey(quest.getParentDayKey(), liveKey));

        quest.setDeadlineMillis(deadlineMillis);
        quest.setEffectivePriority(priority);
        quest.setSkip(skip);
        quest.setStatus(QuestStatus.ACTIVE);
        quest.setTags(tags);

        final long now = X_Time.nowMillisLong();
        quest.setCreatedAtMillis(now);
        quest.setUpdatedAtMillis(now);

        return quest;
    }
}
