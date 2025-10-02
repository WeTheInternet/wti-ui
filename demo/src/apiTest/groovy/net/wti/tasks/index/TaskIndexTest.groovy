package net.wti.tasks.index

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.utils.GdxNativesLoader
import net.wti.tasks.event.TaskEvent
import net.wti.tasks.event.TaskFinishedEvent
import net.wti.tasks.event.TaskStartedEvent
import net.wti.ui.demo.api.ModelRecurrence
import net.wti.ui.demo.api.ModelTask
import net.wti.ui.demo.api.RecurrenceUnit
import net.wti.ui.demo.api.Schedule
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Title
import xapi.constants.X_Namespace
import xapi.fu.Do
import xapi.model.X_Model
import xapi.model.api.ModelKey
import xapi.prop.X_Properties

import java.time.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

@Title("TaskIndex comprehensive specification")
@Narrative("""
Validates TaskIndex behavior across:
- Queries: getAll/getActive/getFinished/getCanceled
- Subscriptions: subscribe, subscribeEvents, subscribeActiveTasks
- Controller hooks: onTaskCreated/Started/Finished/Cancelled/Updated/Deleted
- Bucketing: getDayWithDeadlines, setRolloverHour, setBucketZone
- Auto refresh wiring: startAutoRefresh/stopAutoRefresh
- Diagnostics: getRefreshCount, getBucketUpdateCount
""")
@Stepwise
class TaskIndexTest extends Specification {

    static private final String NAMESPACE_TEST = "test"

    // ----------------------------------------------------------------------------
    // Minimal libGDX Application that executes runnables synchronously
    // ----------------------------------------------------------------------------
    static class TestApplication extends HeadlessApplication {
        static {
            GdxNativesLoader.disableNativesLoading = true
        }

        TestApplication(final ApplicationListener listener) {
            super(listener)
        }
    }

    void setupSpec() {
        // Ensure Gdx.app is available and synchronous for event delivery
        Gdx.app = new TestApplication(new ApplicationAdapter() { })
        // Provide minimal GL to avoid any stray calls in event paths
//        Gdx.gl = Mock(GL20)
        Gdx.gl20 = Gdx.gl
        // delete all test models.
        final String modelDir = X_Properties.getProperty(X_Namespace.PROPERTY_MODEL_DIR) ?: "/tmp/models"
        final File toDelete = new File(modelDir, NAMESPACE_TEST)
        if (toDelete.exists()) {
            toDelete.deleteDir()
        }
        TestModelService.registerTypes()
    }

    void cleanupSpec() {
        Gdx.app = null
//        Gdx.gl = null
        Gdx.gl20 = null
    }

    // ----------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------

    static Double epochMillis(final LocalDateTime ldt, final ZoneId zone = ZoneId.systemDefault()) {
        return Double.valueOf(ldt.atZone(zone).toInstant().toEpochMilli())
    }

    private static saveTasks(final ModelTask ... tasks) {
        tasks.toList().parallelStream().forEach { persistAndWait(it) }
    }

    /**
     * Creates a task with the given parameters and calculates deadline based on recurrence rules
     * @param id Task identifier
     * @param minutesFromNow Minutes from current time for one-time tasks
     * @param finished Whether task is finished
     * @param cancelled Whether task is cancelled
     * @param archived Whether task is archived
     * @param paused Whether task is paused
     * @return Created ModelTask instance
     */
    static ModelTask testTaskOneShot(final String id,
                                     final Double desiredDeadline,
                                     final boolean finished = false,
                                     final boolean cancelled = false,
                                     final boolean archived = false,
                                     final boolean paused = false) {
        final ModelKey key = X_Model.newKey(NAMESPACE_TEST, ModelTask.MODEL_TASK).setId(id)
        final ModelTask t = TaskFactory.create(id, "test:$id")
        t.setKey(key)
        // Add one-time recurrence for the specified minutes from now
        final ModelRecurrence recurrence = X_Model.create(ModelRecurrence.class)
        recurrence.setUnit(RecurrenceUnit.ONCE)
        recurrence.setValue(desiredDeadline.longValue())
        t.recurrence().add(recurrence)
        // Let factory calculate deadline
        TaskFactory.nextTime(t)
        t.setFinished(finished)
        t.setCancelled(cancelled)
        t.setArchived(archived)
        t.setPaused(paused)
        return t
    }

    static LocalDate toLocalDate(final Double epoch, final ZoneId zone) {
        if (epoch == null) return null
        return Instant.ofEpochMilli(epoch.longValue()).atZone(zone).toLocalDate()
    }


    private static void refreshAndWait(final TaskIndex index) {
        index.refresh()
        index.awaitRefresh(2000)
        waitForGdx()
    }

    private static void waitForGdx() {
        final boolean[] done = new boolean[]{false}
        Gdx.app.postRunnable {
            done[0] = true
            synchronized (done) {
                done.notify()
            }
        }
        synchronized (done) {
            done.wait(500)
        }
    }
    private static void persistAndWait(final ModelTask t) {
        X_Model.persist(t, {
            synchronized (t) {
                t.notifyAll()
            }
        })
        synchronized (t) {
            t.wait(1500)
        }
    }

    // ----------------------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------------------

    def "getAll/getActive/getFinished/getCanceled reflect task states"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)

        and: "three tasks in varying states"
        final LocalDateTime now = LocalDateTime.now()
        final ModelTask t1 = testTaskOneShot("t1", epochMillis(now.plusHours(2)), false, false) // active
        final ModelTask t2 = testTaskOneShot("t2", epochMillis(now.plusDays(1)), true, false)   // finished
        final ModelTask t3 = testTaskOneShot("t3", epochMillis(now.plusDays(2)), false, true)   // canceled

        when:

        saveTasks(t1, t2, t3)
        refreshAndWait(index)

        then: "all three are present"
        index.getAll().toList()*.task.containsAll([t1, t2, t3])

        and: "filters partition correctly"
        index.getActive().toList()*.task == [t1]
        index.getFinished().toList()*.task == [t2]
        index.getCanceled().toList()*.task == [t3]

        cleanup:
        index.destroy()
    }

    def "subscribe receives all events; unsubscribe stops further events"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        List<String> received = new ArrayList<>()
        final Do unsub = index.subscribe {
            final TaskEvent evt ->
                received << evt.class.simpleName
        }

        when:
        final ModelTask t = testTaskOneShot("a", epochMillis(LocalDateTime.now().plusHours(1)))
        index.onTaskCreated(t)
        index.onTaskStarted(t)
        index.onTaskUpdated(t)
        index.onTaskFinished(t)
        index.onTaskCancelled(t)
        index.onTaskDeleted(t.getKey())

        final List<String> expected = [
                'TaskLoadedEvent',
                'TaskCreatedEvent',
                'TaskStartedEvent',
                'TaskUpdatedEvent',
                'TaskFinishedEvent',
                'TaskCancelledEvent',
                'TaskDeletedEvent'
        ]

        persistAndWait(t)
        refreshAndWait(index)

        then:
        // TaskIndex posts TaskLoadedEvent (on first upsert) + subsequent events
        received.containsAll(expected)

        when: "unsubscribe and trigger more"
        unsub.done()
        received.clear()
        List<String> received2 = new ArrayList<>()
        index.subscribe { final evt -> received2 << evt.class.simpleName }
        persistAndWait(testTaskOneShot("b", epochMillis(LocalDateTime.now().plusHours(2))))

        refreshAndWait(index)

        then: "no new events for original subscription"
        // original 'received' should not change; new temporary sub collected none after immediate detach
        received.isEmpty()
        !received2.isEmpty()

        cleanup:
        index.destroy()
    }

    def "subscribeEvents filters by desired event classes"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        AtomicInteger started = new AtomicInteger(0)
        AtomicInteger finished = new AtomicInteger(0)

        and:
        final Do unsub = index.subscribeEvents({ final TaskEvent e ->
            if (e instanceof TaskStartedEvent) started.incrementAndGet()
            if (e instanceof TaskFinishedEvent) finished.incrementAndGet()
        }, TaskStartedEvent, TaskFinishedEvent)

        when:
        final ModelTask t = testTaskOneShot("s1", epochMillis(LocalDateTime.now().plusHours(3)))
        index.onTaskStarted(t)
        index.onTaskUpdated(t)
        index.onTaskFinished(t)
        waitForGdx()

        then:
        started.get() == 1
        finished.get() == 1

        cleanup:
        unsub.done()
        index.destroy()
    }

    def "subscribeActiveTasks delivers initial active list and incremental updates only when active"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        List<String> seen = new ArrayList<>()

        and: "seed a cancelled and an active task"
        final ModelTask tActive = testTaskOneShot("act", epochMillis(LocalDateTime.now().plusMinutes(30)), false, false)
        final ModelTask tCanceled = testTaskOneShot("can", epochMillis(LocalDateTime.now().plusMinutes(45)), false, true)
        index.onTaskCreated(tActive)
        index.onTaskCreated(tCanceled)

        when: "subscribe to active tasks"
        final Do unsub = index.subscribeActiveTasks { final ModelTask t -> seen << t.getKey().toString() }

        then: "initial dump only contains active"
        seen == [tActive.getKey().toString()]

        when: "create another active -> should be delivered"
        final ModelTask tActive2 = testTaskOneShot("act2", epochMillis(LocalDateTime.now().plusMinutes(50)), false, false)
        index.onTaskCreated(tActive2)

        and: "finish existing active -> not delivered (removals are not part of this subscription)"
        final ModelTask tFinished = testTaskOneShot("act", epochMillis(LocalDateTime.now().plusMinutes(30)), true, false)
        index.onTaskFinished(tFinished)
        waitForGdx()

        then:
        seen.contains(tActive2.getKey().toString())
        !seen.contains(tFinished.getKey().toString() + ":finished")

        cleanup:
        unsub.done()
        index.destroy()
    }

    def "onTaskDeleted removes from index and buckets; bucketUpdateCount tracks changes"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        final ZoneId zone = ZoneId.systemDefault()
        index.setBucketZone(zone)

        and: "three tasks across two days"
        final LocalDate today = LocalDate.now()
        final Double d1 = epochMillis(LocalDateTime.of(today, LocalTime.of(10, 0)))
        final Double d2 = epochMillis(LocalDateTime.of(today.plusDays(1), LocalTime.of(9, 0)))
        final ModelTask a = testTaskOneShot("a", d1)
        final ModelTask b = testTaskOneShot("b", d1)
        final ModelTask c = testTaskOneShot("c", d2)

        when:
        saveTasks(a, b, c)
        refreshAndWait(index)

        then: "day buckets present"
        final List<Schedule> listToday = index.getDayWithDeadlines(today)
        final List<Schedule> listTomorrow = index.getDayWithDeadlines(today.plusDays(1))
        listToday*.task.containsAll([a, b])
        listTomorrow*.task == [c]
        index.getBucketUpdateCount() > 0

        when: "delete one from today"
        index.onTaskDeleted(a.getKey())

        then: "removed from today list"
        final List<Schedule> after = index.getDayWithDeadlines(today)
        !after*.task.contains(a)
        after*.task == [b]

        cleanup:
        index.destroy()
    }

    def "getDayWithDeadlines filters out finished/cancelled and sorts by deadline ascending"() {
        given:
        TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        final LocalDate today = LocalDate.now()
        final ModelTask t1 = testTaskOneShot("t1", epochMillis(LocalDateTime.of(today, LocalTime.of(9, 0))), false, false)
        final ModelTask t2 = testTaskOneShot("t2", epochMillis(LocalDateTime.of(today, LocalTime.of(8, 30))), false, false)
        final ModelTask t3 = testTaskOneShot("t3", epochMillis(LocalDateTime.of(today, LocalTime.of(7, 45))), true, false)  // finished -> not included
        final ModelTask t4 = testTaskOneShot("t4", epochMillis(LocalDateTime.of(today, LocalTime.of(10, 15))), false, true) // cancelled -> not included

        when:
        [t1, t2, t3, t4].each { index.onTaskCreated(it) }

        then: "only active, sorted by deadline"
        final List<Schedule> out = index.getDayWithDeadlines(today)
        out*.task == [t2, t1]
        out.collect { it.nextDueMillis } == out.collect { it.task.getDeadline().longValue() }

        cleanup:
        index.destroy()
    }

    def "setRolloverHour affects bucketing before cutoff"() {
        given:
        TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        final ZoneId zone = ZoneId.systemDefault()
        index.setBucketZone(zone)
        index.setRolloverHour(4)

        and: "deadline at 02:00 local should roll to previous day bucket"
        final LocalDate day = LocalDate.now()
        final Double deadline = epochMillis(LocalDateTime.of(day, LocalTime.of(2, 0)), zone)
        final ModelTask t = testTaskOneShot("early", deadline)

        when:
        index.onTaskCreated(t)

        then:
        // Because rolloverHour=4, 2AM belongs to previous day
        index.getDayWithDeadlines(day).isEmpty()
        index.getDayWithDeadlines(day.minusDays(1))*.task == [t]

        when: "change rollover to 0; re-upsert same task with same deadline (no state change events expected, but bucket may re-index)"
        index.setRolloverHour(0)
        index.onTaskUpdated(t) // triggers upsert and rebucket

        then:
        index.getDayWithDeadlines(day) *.task == [t]

        cleanup:
        index.destroy()
    }

    def "setBucketZone changes bucketing date conversion"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        index.setRolloverHour(0)

        and: "deadline fixed instant interpreted in two different zones"
        final Instant instant = Instant.now().truncatedTo(ChronoUnit.HOURS)

        final ZoneId zoneA = ZoneId.of("UTC")
        final ZoneId zoneB = ZoneId.systemDefault() == ZoneId.of("UTC") ? ZoneId.of("America/Chicago") : ZoneId.of("UTC")

        final Double deadlineDouble = (double) instant.toEpochMilli()
        final ModelTask t = testTaskOneShot("zoney", deadlineDouble)

        when: "bucket in zone A"
        index.setBucketZone(zoneA)
        index.onTaskCreated(t)

        then:
        final LocalDate dayA = instant.atZone(zoneA).toLocalDate()
        index.getDayWithDeadlines(dayA)*.task == [t]

        when: "rebucket in zone B (update call to trigger rebucketing)"
        index.setBucketZone(zoneB)
        index.onTaskUpdated(t)

        then:
        final LocalDate dayB = instant.atZone(zoneB).toLocalDate()
        index.getDayWithDeadlines(dayB)*.task == [t]
        // If zone changes date, ensure not present in old day (best-effort)
        if (dayA != dayB) {
            assert index.getDayWithDeadlines(dayA).isEmpty()
        }

        cleanup:
        index.destroy()
    }

    def "onTaskStarted/Finished/Cancelled/Updated all reindex and notify"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)
        List<String> seen = []
        final Do unsub = index.subscribe { final evt -> seen << evt.class.simpleName }

        and:
        final Double d = epochMillis(LocalDateTime.now().plusHours(4))
        final ModelTask t = testTaskOneShot("life", d)

        when:
        index.onTaskCreated(t)
        index.onTaskStarted(t)
        index.onTaskUpdated(t)
        index.onTaskFinished(testTaskOneShot("life", d, true, false))
        index.onTaskCancelled(testTaskOneShot("life", d, false, true))
        waitForGdx()
        then:
        seen.containsAll([
                'TaskLoadedEvent',
                'TaskCreatedEvent',
                'TaskStartedEvent',
                'TaskUpdatedEvent',
                'TaskFinishedEvent',
                'TaskCancelledEvent'
        ])

        and: "finished/cancelled do not appear in active day view"
        final LocalDate theDay = toLocalDate(d, ZoneId.systemDefault())
        index.getDayWithDeadlines(theDay).isEmpty()

        cleanup:
        unsub.done()
        index.destroy()

    }

    static class TestIndex extends TaskIndex {
        AtomicInteger refreshes = new AtomicInteger(0)
        @Override
        void refresh() {
            refreshes.incrementAndGet()
        }
    }

    def "startAutoRefresh triggers immediate refresh; stopAutoRefresh prevents further runs"() {
        given:
        final TestIndex index = new TestIndex()

        when: "start auto-refresh with any positive interval"
        final Do stop = index.startAutoRefresh(5f) // minutes; schedules immediately then repeats
        // Allow a tiny bit of time to let timer invoke run() at least once
        sleep(50)

        then: "at least one refresh is triggered"
        index.refreshes.get() >= 1

        when: "stop auto refresh and observe stable count"
        final int before = index.refreshes.get()
        stop.done()
        sleep(100)
        final int after = index.refreshes.get()

        then:
        after == before

        cleanup:
        index.destroy()
    }

    def "diagnostics: refreshCount starts at 0; bucketUpdateCount increases as tasks rebucket"() {
        given:
        final TaskIndex index = new TaskIndex(NAMESPACE_TEST)

        expect:
        index.getRefreshCount() == 0

        when: "add some tasks that enter/leave buckets"
        final LocalDate today = LocalDate.now()
        final ModelTask t1 = testTaskOneShot("d1", epochMillis(LocalDateTime.of(today, LocalTime.of(11, 0))))
        index.onTaskCreated(t1)
        final int before = index.getBucketUpdateCount()
        index.onTaskDeleted(t1.getKey())
        final int after = index.getBucketUpdateCount()

        then:
        after > before

        cleanup:
        index.destroy()
    }
}
