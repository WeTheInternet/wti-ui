package net.wti.ui.quest.sample;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestDefinition;
import net.wti.quest.api.QuestStatus;
import net.wti.quest.impl.DefaultQuestCompletionStore;
import net.wti.quest.impl.QuestCompletionService;
import net.wti.quest.model.impl.LiveQuestLoaderImpl;
import net.wti.time.api.ModelDay;
import net.wti.time.impl.DayIndexService;
import net.wti.time.impl.ModelDayService;
import net.wti.ui.demo.theme.LifeQuestTheme;
import net.wti.ui.quest.api.QuestActionHandler;
import net.wti.ui.quest.impl.DayPlanView;
import net.wti.ui.quest.impl.DefaultLiveQuestRowFactory;
import net.wti.ui.sample.AbstractSampleApp;
import xapi.jre.model.ModelServiceJre;
import xapi.model.X_Model;
import xapi.time.X_Time;
import xapi.time.api.TimeZoneInfo;
import xapi.util.api.SuccessHandler;

import java.util.ArrayList;
import java.util.List;

/// LiveQuestDemoApp
///
/// Concrete sample app that:
///  - Creates a synthetic ModelDay for "today" in system zone.
///  - Seeds a handful of LiveQuest instances with different deadlines,
///    priorities, and skip/status flags.
///  - Renders them using DayPlanView inside a ScrollPane.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 03:43
public class LiveQuestDemoApp extends AbstractSampleApp {

    private LifeQuestTheme theme;

    @Override
    protected Skin createSkin() {
        theme = new LifeQuestTheme();
        theme.applyTooltipDefaults();
        return theme.getSkin();
    }

    @Override
    protected void createContent(final Stage stage, final Skin skin) {

        final ModelDay today = createTodayModelDay();
        final DemoBootstrapService setup = new DemoBootstrapService();
        final List<LiveQuest> quests = setup.loadForToday(today);

        final QuestCompletionService completionService = new QuestCompletionService(new DefaultQuestCompletionStore());

        final DefaultLiveQuestRowFactory rows = new DefaultLiveQuestRowFactory(skin, new QuestActionHandler() {
            @Override
            public void complete(final ModelDay day, final LiveQuest quest) {
                completionService.complete(day, quest);
            }
        });

        final DayPlanView view = new DayPlanView(skin, today, quests, rows);
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
}
