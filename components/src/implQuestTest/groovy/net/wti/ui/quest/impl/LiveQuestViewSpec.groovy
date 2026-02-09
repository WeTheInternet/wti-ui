package net.wti.ui.quest.impl

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import net.wti.quest.api.LiveQuest
import net.wti.quest.api.QuestStatus
import net.wti.time.api.ModelDay
import net.wti.time.impl.DayIndexService
import net.wti.time.impl.ModelDayService
import net.wti.ui.demo.theme.TaskUiTheme
import spock.lang.Shared
import spock.lang.Specification
import xapi.model.X_Model
import xapi.time.X_Time
import xapi.time.api.TimeZoneInfo

/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 22:06
class LiveQuestViewSpec extends Specification {

    @Shared
    boolean gdxInitialized = false

    def setupSpec() {
        if (!gdxInitialized) {
            final HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration()
            new HeadlessApplication(new ApplicationAdapter() {}, config)
            Gdx.graphics = new MockGraphics()

            // Loose mock: returns 0 / null by default unless we override
            final GL20 gl = Mock(GL20)

            // LibGDX often expects generated handles != 0
            gl.glGenTexture() >> 1
            // Optional: if you see failures around shader/program IDs
            // gl.glCreateShader(_ as int) >> 1
            // gl.glCreateProgram() >> 1

            Gdx.gl = gl
            Gdx.gl20 = gl

            gdxInitialized = true
        }
    }

    def "refresh builds quest rows without blowing up"() {
        given: "a ModelDay similar to the demo"
        final TimeZoneInfo zone = X_Time.systemZone()
        final int rolloverHour = 4
        final double now = X_Time.nowMillis()

        final DayIndexService indexService = new DayIndexService(zone)
        final ModelDayService dayService = new ModelDayService(indexService)
        final ModelDay today = dayService.getOrCreateModelDay(now, zone, rolloverHour)

        and: "a few sample LiveQuest instances"
        final List<LiveQuest> quests = []
        final long start = today.startTimestamp()
        final long hourMillis = 60L * 60L * 1000L

        quests << sampleQuest(today, "questA/morning",   start +  2L * hourMillis, 10, false, "work", "focus")
        quests << sampleQuest(today, "questB/afternoon", start +  8L * hourMillis,  5, false, "admin")
        quests << sampleQuest(today, "questC/evening",   start + 13L * hourMillis,  7, false, "health")

        and: "a skin from the same theme the real app uses"

        final TaskUiTheme theme = new TaskUiTheme()
        theme.applyTooltipDefaults()
        final Skin skin = theme.skin

        when: "parser will build the view and refresh it"
        final DefaultLiveQuestView view = new DefaultLiveQuestView(skin, today, quests)
        view.refresh()

        then: "at least one row is rendered (basic smoke test)"
        final Array children = view.children
        !children.isEmpty()
    }

    private static LiveQuest sampleQuest(
            final ModelDay day,
            final String liveKey,
            final long deadlineMillis,
            final int priority,
            final boolean skip,
            final String... tags
    ) {
        final LiveQuest quest = X_Model.create(LiveQuest)
        quest.parentDayKey = ModelDay.newKey(day.dayNum)
        quest.dayIndex = day.dayNum
        quest.liveKey = liveKey
        quest.key = LiveQuest.newKey(quest.parentDayKey, liveKey)

        quest.deadlineMillis = deadlineMillis
        quest.effectivePriority = priority
        quest.skip = skip
        quest.status = QuestStatus.ACTIVE
        quest.tags = tags

        final long now = X_Time.nowMillisLong()
        quest.createdAtMillis = now
        quest.updatedAtMillis = now

        quest
    }
}
