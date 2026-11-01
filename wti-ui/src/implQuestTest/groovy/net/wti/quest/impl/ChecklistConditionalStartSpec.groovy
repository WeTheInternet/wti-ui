package net.wti.quest.impl

import net.wti.quest.api.LiveQuest
import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.QuestStatus
import net.wti.quest.api.QuestRequirements
import net.wti.time.api.ModelDay
import spock.lang.Specification
import xapi.model.api.Model
import xapi.model.api.ModelKey
import xapi.model.api.ModelCreateResult
import xapi.util.api.ErrorHandler
import xapi.util.api.SuccessHandler

/** Actual shared start with real model values and controlled conditional ACKs; not an IO atomicity proof. */
class ChecklistConditionalStartSpec extends Specification {
    Capable models = new Capable()
    ChecklistProjectionService service = new ChecklistProjectionService(models)
    ModelDay day
    QuestDefinition root

    def setup() {
        day = models.create(ModelDay); day.dayNum = 42
        day.key = models.newKey('child-a','day','42').setKeyType(ModelKey.KEY_TYPE_LONG)
        root = ChecklistProjectionServiceSpec.definition(models,'packing',['water','snack'])
    }
    Map start(Closure<Boolean> current = { true }) {
        ChecklistProjectionServiceSpec.result { service.start(day,root.key,current,it) }
    }
    LiveQuest terminal(LiveQuest candidate) {
        def value = models.copy(candidate) as LiveQuest
        value.status = QuestStatus.FINISHED; value.createdAtMillis = 11L; value.updatedAtMillis = 22L
        value.finishedAtMillis = 33L; value.title = 'Previously finished'; value.tags = ['retained'] as String[]
        models.put(value); value
    }

    def 'raced finished root is retained and prevents any child creation'() {
        given:
        models.deferCreates = true
        def result = start()
        def existing = terminal(models.pending[0].model as LiveQuest)
        when:
        models.ackCreate()
        then:
        result.error instanceof IllegalStateException
        models.creates.size() == 1
        models.pending.empty
        models.ordinaryPersists == 0
        def stored = models.records[models.keyToString(existing.key)] as LiveQuest
        stored.status == QuestStatus.FINISHED
        stored.createdAtMillis == 11L && stored.updatedAtMillis == 22L && stored.finishedAtMillis == 33L
        stored.title == 'Previously finished'
        stored.tags as List == ['retained']
    }
    def 'raced finished child is reused and only absent siblings are created'() {
        given:
        models.deferCreates = true
        def result = start(); models.ackCreate()
        def existing = terminal(models.pending[0].model as LiveQuest)
        when:
        models.ackCreate(); models.ackCreate()
        then:
        !result.error && result.value.children[0].complete
        models.created == 2
        models.ordinaryPersists == 0
        def stored = models.records[models.keyToString(existing.key)] as LiveQuest
        stored.finishedAtMillis == 33L && stored.createdAtMillis == 11L && stored.updatedAtMillis == 22L
        stored.parentDayKey.keyType == ModelKey.KEY_TYPE_LONG
        stored.sourceDefinitionKey.id == 'water'
        stored.title == 'Previously finished'
    }
    def 'two same child day starts create each record once and acknowledge existing models to the other caller'() {
        given:
        models.deferCreates = true
        def first = start(); def second = start()
        when:
        while (!models.pending.empty) models.ackCreate()
        then:
        !first.error && !second.error
        models.created == 3
        models.creates.size() == 6
        models.ordinaryPersists == 0
        first.value.root.liveKey == second.value.root.liveKey
        first.value.children*.liveKey == second.value.children*.liveKey
    }
    def 'partial conditional failure never downgrades and retry preserves acknowledged terminal progress'() {
        given:
        models.failCreateId = 'snack'
        def failed = start()
        def water = models.records.values().find { it instanceof LiveQuest && it.key.id == 'water' } as LiveQuest
        terminal(water)
        def rootBefore = models.records[models.keyToString(day.key.getChild('lv','packing'))].createdAtMillis
        models.failCreateId = null; models.creates.clear()
        when:
        def retried = start()
        then:
        failed.error instanceof IOException
        !retried.error && retried.value.children[0].complete
        models.creates*.key*.id == ['snack']
        models.ordinaryPersists == 0
        models.records[retried.value.root.liveKey].createdAtMillis == rootBefore
        models.records[retried.value.children[0].liveKey].finishedAtMillis == 33L
    }
    def 'lost child ACK retry retains a subsequently finished image instead of resetting it'() {
        given:
        models.uncertainCreateId = 'water'
        def uncertain = start()
        def water = models.records.values().find { it instanceof LiveQuest && it.key.id == 'water' } as LiveQuest
        terminal(water)
        models.uncertainCreateId = null; models.creates.clear()
        when:
        def retried = start()
        then:
        uncertain.error instanceof IOException
        !retried.error && retried.value.children[0].complete
        models.creates*.key*.id == ['snack']
        models.ordinaryPersists == 0
        models.records[retried.value.children[0].liveKey].finishedAtMillis == 33L
    }

    def 'conditional unsupported failure and null result never cause ordinary persist'() {
        given:
        models.failure = failure
        when:
        def result = start()
        then:
        result.error
        models.creates.size() == 1
        models.ordinaryPersists == 0
        models.created == 0
        where:
        failure << ['unsupported','null','throw']
    }
    def 'each conditional ACK is required and duplicate callbacks cannot submit another child'() {
        given:
        models.deferCreates = true; models.duplicateAck = true
        def result = start()
        when:
        models.ackCreate(); models.ackCreate()
        then:
        result.isEmpty()
        models.creates.size() == 3
        when:
        models.ackCreate()
        then:
        !result.error && result.value.children.every { it.started }
        models.creates.size() == 3
        models.pending.empty
        models.ordinaryPersists == 0
    }
    def 'caller context mutations and disposal cannot redirect or chain an already submitted create'() {
        given:
        models.deferCreates = true
        boolean current = true
        def result = start { current }
        day.key = models.newKey('child-b','day','43').setKeyType(ModelKey.KEY_TYPE_LONG); day.dayNum = 43
        current = false
        when:
        models.ackCreate()
        then:
        result.error instanceof java.util.concurrent.CancellationException
        models.creates.size() == 1
        models.pending.empty
        def stored = models.records.values().find { it instanceof LiveQuest } as LiveQuest
        stored.key.namespace == 'child-a'
        stored.parentDayKey.namespace == 'child-a' && stored.parentDayKey.id == '42'
        stored.dayIndex == 42
        models.ordinaryPersists == 0
    }
    def 'bad acknowledged lineage fails before the next child creation'() {
        given:
        models.deferCreates = true
        def result = start()
        def altered = models.copy(models.pending[0].model) as LiveQuest
        if (kind == 'owner') altered.key = models.newKey('other','day','42').setKeyType(ModelKey.KEY_TYPE_LONG).getChild('lv','packing')
        if (kind == 'day') altered.dayIndex = 43
        if (kind == 'definition') altered.sourceDefinitionKey = models.newKey('definitions','qdef','other')
        when:
        models.pending.remove(0).callback.onSuccess(new ModelCreateResult(false,altered))
        then:
        result.error instanceof IllegalStateException || result.error instanceof IllegalArgumentException
        models.creates.size() == 1 && models.pending.empty
        models.ordinaryPersists == 0
        where:
        kind << ['owner','day','definition']
    }
    def 'newly appeared restricted root or child fails before creating further siblings'() {
        given:
        models.deferCreates = true
        def result = start()
        if (child) models.ackCreate()
        def restricted = models.copy(models.pending[0].model) as LiveQuest
        restricted.toComplete = models.create(QuestRequirements); models.put(restricted)
        when:
        models.ackCreate()
        then:
        result.error instanceof UnsupportedOperationException
        models.pending.empty
        models.creates.size() == (child ? 2 : 1)
        models.ordinaryPersists == 0
        where:
        child << [false,true]
    }
    def 'explicit unavailable capability retains documented legacy behavior'() {
        given:
        models.available = false
        when:
        def result = start()
        then:
        !result.error
        models.creates.empty
        models.ordinaryPersists == 3
    }
    def 'cancellation before a create submits no write'() {
        when:
        def result = start { false }
        then:
        result.error instanceof java.util.concurrent.CancellationException
        models.creates.empty
        models.ordinaryPersists == 0
    }

    static class Capable extends ChecklistProjectionServiceSpec.Backend {
        boolean available = true, deferCreates, duplicateAck
        String failure, failCreateId, uncertainCreateId
        int ordinaryPersists, created
        List<LiveQuest> creates = []
        List<Map> pending = []
        @Override boolean supportsCreateIfAbsent() { available }
        @Override void persist(Model model, SuccessHandler callback) {
            ordinaryPersists++
            if (available) throw new AssertionError('Capable start must never use ordinary persist')
            super.persist(model,callback)
        }
        @Override Model copy(Model model) {
            def detached = super.copy(model)
            if (detached instanceof LiveQuest) {
                detached.parentDayKey = keyFromString(keyToString(detached.parentDayKey))
                detached.sourceDefinitionKey = keyFromString(keyToString(detached.sourceDefinitionKey))
                detached.tags = detached.tags == null ? null : Arrays.copyOf(detached.tags, detached.tags.length)
            }
            detached
        }
        @Override void createIfAbsent(Model candidate, SuccessHandler callback) {
            creates << (copy(candidate) as LiveQuest)
            if (failure == 'throw') throw new UnsupportedOperationException('conditional unavailable')
            if (failure == 'unsupported') { (callback as ErrorHandler).onError(new UnsupportedOperationException('conditional unavailable')); return }
            if (failure == 'null') { callback.onSuccess(null); return }
            pending << [model:copy(candidate),callback:callback]
            if (!deferCreates) ackCreate()
        }
        void ackCreate() {
            def call = pending.remove(0)
            if (call.model.key.id == failCreateId) { (call.callback as ErrorHandler).onError(new IOException('conditional failure')); return }
            String key = keyToString(call.model.key)
            boolean exists = records.containsKey(key)
            if (!exists) { put(call.model as Model); created++ }
            if (call.model.key.id == uncertainCreateId) {
                (call.callback as ErrorHandler).onError(new IOException('uncertain conditional acknowledgement')); return
            }
            def result = new ModelCreateResult(!exists,copy(records[key]))
            call.callback.onSuccess(result)
            if (duplicateAck) call.callback.onSuccess(result)
        }
    }
}
