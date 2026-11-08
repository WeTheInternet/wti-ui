package net.wti.quest.impl

import net.wti.quest.api.*
import net.wti.time.api.ModelDay
import spock.lang.Specification
import xapi.model.api.ModelKey

import static net.wti.quest.impl.ChecklistProjectionServiceSpec.definition
import static net.wti.quest.impl.ChecklistProjectionServiceSpec.result
import static net.wti.quest.impl.ChecklistProjectionServiceSpec.restrict

class AsyncQuestCompletionServiceSpec extends Specification {
    ChecklistProjectionServiceSpec.Backend models = new ChecklistProjectionServiceSpec.Backend()
    ChecklistProjectionService projection = new ChecklistProjectionService(models)
    AsyncQuestCompletionService completion = new AsyncQuestCompletionService(models)
    ModelDay day
    QuestDefinition root
    ChecklistSnapshot started
    ModelKey childKey

    def setup() {
        day = models.create(ModelDay)
        day.dayNum = 42
        day.key = models.newKey('child-a', 'owner', 'profile').setKeyType(9)
                .getChild('day', '42').setKeyType(ModelKey.KEY_TYPE_LONG)
        root = definition(models, 'packing', ['water', 'snack'])
        started = result { projection.start(day, root.key, it) }.value
        childKey = models.keyFromString(started.children[0].liveKey)
        models.writes.clear()
    }

    def 'completion saves nested history first and never mutates loaded caller model'() {
        given:
        LiveQuest caller = models.records[models.keyToString(childKey)]
        when:
        def answer = result { completion.complete(day, childKey, it) }
        then:
        !answer.error
        models.writes*.key*.kind == ['dn', 'lv']
        caller.status == QuestStatus.ACTIVE
        caller.finishedAtMillis == null
        answer.value.children[0].complete
        !started.children[0].complete
        def history = models.writes[0] as QuestCompleted
        models.keyToString(history.key.parent) == models.keyToString(childKey.parent)
        history.key.namespace == 'child-a'
        history.key.parent.parent.parent.keyType == 9
        history.instanceKey.keyType == childKey.keyType
        history.snapshot.name == 'water'
        history.occurredAtMillis == models.writes[1].finishedAtMillis
    }

    def 'root ALL_OF is enforced by service before writes and allowed only after children finish'() {
        given:
        def rootKey = models.keyFromString(started.root.liveKey)
        expect:
        result { completion.complete(day, rootKey, it) }.error
        models.writes.empty
        when:
        started.children.each { entry -> assert !result { completion.complete(day, models.keyFromString(entry.liveKey), it) }.error }
        def answer = result { completion.complete(day, rootKey, it) }
        then:
        !answer.error
        answer.value.root.complete
    }

    def 'missing or inaccessible child blocks root without being treated as finished'() {
        given:
        def key = started.children[1].liveKey
        if (fault == 'missing') models.records.remove(key)
        else models.loadErrors[key] = new IOException('unavailable')
        when:
        def answer = result { completion.complete(day, models.keyFromString(started.root.liveKey), it) }
        then:
        answer.error
        models.writes.empty
        where:
        fault << ['missing', 'unavailable']
    }

    def 'history failure leaves live active and retry never mutates original'() {
        given:
        def caller = models.records[models.keyToString(childKey)]
        models.failPersistKind = 'dn'
        when:
        def failed = result { completion.complete(day, childKey, it) }
        then:
        failed.error
        models.writes*.key*.kind == ['dn']
        caller.status == QuestStatus.ACTIVE
        models.records[models.keyToString(childKey)].status == QuestStatus.ACTIVE
        when:
        models.failPersistKind = null
        def retry = result { completion.complete(day, childKey, it) }
        then:
        !retry.error
        caller.status == QuestStatus.ACTIVE
        retry.value.children[0].complete
    }

    def 'partial completion retry reuses stable history and its original time'() {
        given:
        models.failPersistKind = 'lv'
        when:
        def failed = result { completion.complete(day, childKey, it) }
        def history = models.records.values().find { it instanceof QuestCompleted }
        def time = history.occurredAtMillis
        then:
        failed.error
        models.records[models.keyToString(childKey)].status == QuestStatus.ACTIVE
        when:
        models.failPersistKind = null
        models.writes.clear()
        def retry = result { completion.complete(day, childKey, it) }
        then:
        !retry.error
        models.writes*.key*.kind == ['lv']
        models.writes[0].finishedAtMillis == time
        models.records[models.keyToString(history.key)].is(history)
    }

    def 'persisted but timed out writes reconcile through reload without duplicate history'() {
        given:
        models.failPersistKind = phase
        models.writeThenFail = true
        when:
        def failed = result { completion.complete(day, childKey, it) }
        models.failPersistKind = null
        models.writes.clear()
        def retry = result { completion.complete(day, childKey, it) }
        then:
        failed.error
        !retry.error
        retry.value.children[0].complete
        models.records.values().count { it instanceof QuestCompleted } == 1
        models.writes*.key*.kind == (phase == 'dn' ? ['lv'] : [])
        where:
        phase << ['dn', 'lv']
    }

    def 'terminal lv missing history is backfilled with its original timestamp'() {
        given:
        def live = models.records[models.keyToString(childKey)]
        live.status = QuestStatus.FINISHED
        live.finishedAtMillis = 123L
        when:
        def answer = result { completion.complete(day, childKey, it) }
        then:
        !answer.error
        models.writes*.key*.kind == ['dn']
        models.writes[0].occurredAtMillis == 123L
        live.finishedAtMillis == 123L
    }

    def 'contradictory existing history is never overwritten'() {
        given:
        assert !result { completion.complete(day, childKey, it) }.error
        def history = models.records.values().find { it instanceof QuestCompleted }
        history.instanceKey = day.key.getChild('lv', 'other')
        models.writes.clear()
        when:
        def answer = result { completion.complete(day, childKey, it) }
        then:
        answer.error
        models.writes.empty
    }

    def 'pending completion and double-click do not report success before both acknowledgments'() {
        given:
        models.defer = true
        when:
        def first = result { completion.complete(day, childKey, it) }
        def duplicate = result { completion.complete(day, childKey, it) }
        then:
        first.isEmpty()
        duplicate.error
        models.queued.size() == 1
        when:
        models.ack()
        then:
        first.isEmpty()
        models.queued.size() == 1
        when:
        models.ack()
        then:
        first.value.children[0].complete
    }

    def 'wrong owner day refuses completion before any writes'() {
        given:
        day.key = models.newKey('child-b', 'day', '42').setKeyType(ModelKey.KEY_TYPE_LONG)
        when:
        def answer = result { completion.complete(day, childKey, it) }
        then:
        answer.error
        models.writes.empty
    }

    def 'restrictions on existing live root or child prevent completion writes'() {
        given:
        def restricted = models.records[target == 'root' ? started.root.liveKey : started.children[0].liveKey]
        restrict(models, restricted, restriction)
        when:
        def answer = result { completion.complete(day, childKey, it) }
        then:
        answer.error
        models.writes.empty
        models.records[models.keyToString(childKey)].status == QuestStatus.ACTIVE
        where:
        target | restriction
        'root' | 'requirement'
        'child' | 'requirement'
        'root' | 'users-empty'
        'child' | 'users-empty'
        'root' | 'groups-empty'
        'child' | 'groups-empty'
        'root' | 'users-nonempty'
        'child' | 'users-nonempty'
        'root' | 'groups-nonempty'
        'child' | 'groups-nonempty'
    }
}
