package net.wti.quest.impl

import net.wti.quest.api.ChecklistSnapshot
import net.wti.quest.api.LiveQuest
import net.wti.quest.api.QuestAtomicCompletion
import net.wti.quest.api.QuestCompleted
import net.wti.quest.api.QuestCompletionCommand
import net.wti.quest.api.QuestCompletionResult
import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.QuestStatus
import net.wti.time.api.ModelDay
import spock.lang.Specification
import xapi.model.api.ModelKey
import xapi.util.api.ErrorHandler
import xapi.util.api.SuccessHandler

import static net.wti.quest.impl.ChecklistProjectionServiceSpec.definition
import static net.wti.quest.impl.ChecklistProjectionServiceSpec.result

class QuestAtomicCompletionSpec extends Specification {
    Atomic models = new Atomic()
    ModelDay day
    QuestDefinition root
    ChecklistSnapshot started
    ModelKey child
    def setup() {
        day = models.create(ModelDay); day.dayNum = 42
        day.key = models.newKey('child-a','day','42').setKeyType(ModelKey.KEY_TYPE_LONG)
        models.day = day
        root = definition(models,'packing',['water','snack'])
        started = result { new ChecklistProjectionService(models).start(day,root.key,it) }.value
        child = models.keyFromString(started.children[0].liveKey)
        models.writes.clear()
    }
    def 'planner stages detached pair without any writes and samples clock only for new history'() {
        given:
        int clocks = 0
        def planner = new QuestCompletionPlanner(models,{ clocks++; 777L })
        def loaded = models.records[models.keyToString(child)]
        when:
        def plan = result { planner.prepare(day,child,it) }.value
        then:
        clocks == 1 && models.writes.empty
        plan.needsHistoryWrite() && plan.needsLiveWrite()
        plan.live.status == QuestStatus.FINISHED && loaded.status == QuestStatus.ACTIVE
        plan.live.finishedAtMillis == 777L && plan.history.occurredAtMillis == 777L
        when:
        models.records[models.keyToString(plan.history.key)] = plan.history
        def retry = result { planner.prepare(day,child,it) }.value
        then:
        clocks == 1 && !retry.needsHistoryWrite() && retry.needsLiveWrite()
        when:
        models.records[models.keyToString(plan.live.key)] = plan.live
        def terminal = result { planner.prepare(day,child,it) }.value
        then:
        clocks == 1 && !terminal.needsHistoryWrite() && !terminal.needsLiveWrite()
        terminal.acknowledgedSnapshot(terminal.live).children[0].complete
    }
    def 'finished live missing history preserves timestamp without reading clock'() {
        given:
        def value = models.records[models.keyToString(child)]
        value.status = QuestStatus.FINISHED; value.finishedAtMillis = 123L
        when:
        def answer = result { new QuestCompletionPlanner(models,{ throw new AssertionError('clock') }).prepare(day,child,it) }
        then:
        !answer.error && answer.value.history.occurredAtMillis == 123L
        answer.value.needsHistoryWrite() && !answer.value.needsLiveWrite()
        models.writes.empty
    }
    def 'atomic acknowledgment completes without ordinary persist and preserves loaded caller state'() {
        given:
        def caller = models.records[models.keyToString(child)]
        when:
        def answer = result { new AsyncQuestCompletionService(models).complete(day,child,it) }
        then:
        !answer.error && answer.value.children[0].complete
        caller.status == QuestStatus.ACTIVE
        models.requests == 1 && models.writes.empty
        models.records[models.keyToString(child)].finishedAtMillis == 456L
    }
    def 'selected atomic failures or support exceptions never downgrade'() {
        given:
        models.fault = fault
        when:
        def answer = result { new AsyncQuestCompletionService(models).complete(day,child,it) }
        then:
        answer.error && models.writes.empty
        where:
        fault << ['support','request','null','identity','time']
    }
    def 'wrong captured day or cancelled run submits zero atomic requests'() {
        given:
        def service = new AsyncQuestCompletionService(models)
        when:
        def cancelled = result { service.complete(day,child,{ false },it) }
        day.key = models.newKey('other-child','day','42').setKeyType(ModelKey.KEY_TYPE_LONG)
        def wrong = result { service.complete(day,child,it) }
        then:
        cancelled.error instanceof java.util.concurrent.CancellationException
        wrong.error instanceof IllegalArgumentException
        models.requests == 0 && models.writes.empty
    }
    def 'explicit false alone selects legacy history before live writes'() {
        given:
        models.available = false
        when:
        def answer = result { new AsyncQuestCompletionService(models).complete(day,child,it) }
        then:
        !answer.error && models.requests == 0
        models.writes*.key*.kind == ['dn','lv']
    }
    def 'issued atomic completion may settle after disposal but cannot continue or redirect'() {
        given:
        models.deferAtomic = true
        boolean current = true
        def service = new AsyncQuestCompletionService(models)
        def answer = result { service.complete(day,child,{ current },it) }
        def before = result { service.complete(day,child,it) }
        expect:
        answer.isEmpty() && before.error && models.requests == 1
        when:
        current = false
        day.key = models.newKey('child-b','day','43').setKeyType(ModelKey.KEY_TYPE_LONG); day.dayNum = 43
        models.release()
        then:
        answer.error instanceof java.util.concurrent.CancellationException
        models.requests == 1 && models.writes.empty
        models.records[models.keyToString(child)].status == QuestStatus.FINISHED
    }
    def 'child acknowledgment survives separate ALL_OF ancestor rejection and retry reconciles'() {
        given:
        models.failRoot = true
        def intents = new ChecklistIntentService(models)
        def first = result { intents.complete(day,root.key,child,{ true },it) }
        def seen = []
        def snack = models.keyFromString(started.children[1].liveKey)
        when:
        def failed = result { intents.complete(day,root.key,snack,{ true },{ seen << it },it) }
        then:
        !first.error && failed.error
        seen.size() == 1 && seen[0].children.every { it.complete }
        models.records[models.keyToString(snack)].status == QuestStatus.FINISHED
        when:
        models.failRoot = false
        def retry = result { intents.complete(day,root.key,snack,{ true },it) }
        then:
        !retry.error && retry.value.root.complete && models.writes.empty
    }
    static class Atomic extends ChecklistProjectionServiceSpec.Backend implements QuestAtomicCompletion {
        boolean available = true, deferAtomic, failRoot
        String fault
        int requests
        ModelDay day
        Closure release
        boolean supportsAtomicCompletion() {
            if (fault == 'support') throw new IllegalStateException('unsupported')
            available
        }
        void complete(ModelKey dayKey, ModelKey live, SuccessHandler<QuestCompletionResult> callback) {
            requests++
            if (fault == 'request' || failRoot && live.id == 'packing') {
                (callback as ErrorHandler).onError(new IOException('unavailable')); return
            }
            def captured = create(ModelDay); captured.key = keyFromString(keyToString(dayKey)); captured.dayNum = dayKey.id as int
            def target = keyFromString(keyToString(live))
            def work = {
                def plan = result { new QuestCompletionPlanner(this,{ 456L }).prepare(captured,target,it) }
                if (plan.error) { (callback as ErrorHandler).onError(plan.error); return }
                records[keyToString(plan.value.live.key)] = plan.value.live
                records[keyToString(plan.value.history.key)] = plan.value.history
                def command = new QuestCompletionCommand(QuestCompletionCommand.PROTOCOL,'operation',keyToString(dayKey),keyToString(target))
                if (fault == 'identity') command = new QuestCompletionCommand(command.protocol,'operation','wrong',command.liveKey)
                if (fault == 'time') plan.value.history.occurredAtMillis = 999L
                callback.onSuccess(fault == 'null' ? null : new QuestCompletionResult(command,plan.value.live,plan.value.history))
            }
            if (deferAtomic) release = work else work()
        }
    }
}
