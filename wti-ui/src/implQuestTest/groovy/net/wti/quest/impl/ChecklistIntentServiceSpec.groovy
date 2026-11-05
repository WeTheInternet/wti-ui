package net.wti.quest.impl

import net.wti.quest.api.*
import net.wti.time.api.ModelDay
import spock.lang.Specification
import xapi.model.api.ModelKey
import static net.wti.quest.impl.ChecklistProjectionServiceSpec.definition
import static net.wti.quest.impl.ChecklistProjectionServiceSpec.result

class ChecklistIntentServiceSpec extends Specification {
    def models = new ChecklistProjectionServiceSpec.Backend()
    def projection = new ChecklistProjectionService(models)
    def intents = new ChecklistIntentService(models)
    ModelDay day
    QuestDefinition root
    boolean current = true
    def setup() {
        day = models.create(ModelDay)
        day.key = models.newKey('child-a', 'day', '42').setKeyType(ModelKey.KEY_TYPE_LONG)
        day.dayNum = 42
        root = definition(models, 'morning', ['brush', 'dress'])
    }
    ModelKey target(String child) { day.key.getChild('lv', root.key.id).getChild('lv', child) }

    def 'first checkbox starts and completes only acknowledged target; last child closes ALL_OF root'() {
        when:
        def first = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        then:
        !first.error
        first.value.children*.complete == [true, false]
        !first.value.root.complete
        models.writes*.key*.kind == ['lv', 'lv', 'lv', 'dn', 'lv']
        when:
        models.writes.clear()
        def last = result { intents.complete(day, root.key, target('dress'), { current }, it) }
        then:
        !last.error
        last.value.root.complete
        models.writes*.key*.kind == ['dn', 'lv', 'dn', 'lv']
        when: 'repeating the explicit intent reconciles without another write'
        models.writes.clear()
        def retry = result { intents.complete(day, root.key, target('dress'), { current }, it) }
        then:
        retry.value.root.complete
        models.writes.empty
    }

    def 'first checks across goals queue instead of overlapping or dropping the second intent'() {
        given:
        def second = definition(models, 'before-you-go', ['bag'])
        models.defer = true
        when:
        def first = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        def other = result { intents.complete(day, second.key, day.key.getChild('lv', second.key.id).getChild('lv', 'bag'), { current }, it) }
        then:
        first.isEmpty() && other.isEmpty()
        models.queued.size() == 1
        models.writes*.key*.id == ['morning']
        when:
        int count = 0
        while (models.queued && count++ < 30) models.ack()
        then:
        count == 11
        first.value.children[0].complete
        other.value.root.complete
        models.records.values().count { it instanceof LiveQuest } == 5
        !models.writes.any { it instanceof ModelDay }
    }

    def 'dispose after submitted start write stops remaining start and queued goal writes'() {
        given:
        models.defer = true
        when:
        def first = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        def queued = result { intents.complete(day, root.key, target('dress'), { current }, it) }
        current = false
        models.ack()
        then:
        first.error instanceof java.util.concurrent.CancellationException
        queued.error instanceof java.util.concurrent.CancellationException
        models.writes*.key*.id == ['morning']
        models.queued.empty
    }

    def 'dispose between history and terminal acknowledgment prevents subsequent live write'() {
        given:
        assert !result { projection.start(day, root.key, it) }.error
        models.writes.clear(); models.defer = true
        when:
        def first = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        current = false
        models.ack()
        then:
        first.error instanceof java.util.concurrent.CancellationException
        models.writes*.key*.kind == ['dn']
        models.records[models.keyToString(target('brush'))].status == QuestStatus.ACTIVE
    }

    def 'failed acknowledgment is retryable without recreating acknowledged start records'() {
        given:
        models.failPersistKind = 'dn'
        when:
        def failure = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        models.failPersistKind = null; models.writes.clear()
        def retry = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        then:
        failure.error
        retry.value.children[0].complete
        models.writes*.key*.kind == ['dn', 'lv']
    }

    def 'failed ancestor finalization preserves acknowledged children and root can reconcile on retry'() {
        given:
        assert !result { intents.complete(day, root.key, target('brush'), { current }, it) }.error
        models.failPersistId = 'morning'
        List<ChecklistSnapshot> acknowledged = []
        when:
        def failed = result { intents.complete(day, root.key, target('dress'), { current }, { acknowledged << it }, it) }
        then:
        failed.error
        acknowledged.last().children.every { it.complete }
        !acknowledged.last().root.complete
        acknowledged.last().root.canComplete()
        models.records[models.keyToString(target('dress'))].status == QuestStatus.FINISHED
        when:
        models.failPersistId = null; models.writes.clear()
        def retry = result { intents.complete(day, root.key, day.key.getChild('lv', 'morning'), { current }, it) }
        then:
        retry.value.root.complete
        models.writes*.key*.kind == ['dn', 'lv']
    }

    def 'queued intent captures full day identity rather than mutable caller model'() {
        given:
        models.defer = true
        when:
        def answer = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        day.key = models.newKey('other-owner', 'day', '99').setKeyType(ModelKey.KEY_TYPE_LONG)
        day.dayNum = 99
        int count = 0
        while (models.queued && count++ < 20) models.ack()
        then:
        answer.value.children[0].complete
        models.writes.every { it.key.namespace == 'child-a' }
        models.writes.findAll { it instanceof LiveQuest }.every { it.dayIndex == 42 }
    }

    def 'an active child completes without restarting an inactive missing sibling'() {
        given:
        def started = result { projection.start(day, root.key, it) }.value
        models.records.remove(started.children[1].liveKey)
        def inactive = models.records.values().find { it instanceof QuestDefinition && it.key.id == 'dress' }
        inactive.active = false
        models.put(inactive)
        models.writes.clear()

        when:
        def loaded = result { projection.load(day, root.key, it) }
        def restart = result { projection.start(day, root.key, it) }

        then:
        !loaded.error
        loaded.value.children[0].canComplete()
        !loaded.value.canStart()
        restart.error instanceof IllegalStateException
        models.writes.empty

        when:
        def answer = result { intents.complete(day, root.key, target('brush'), { current }, it) }

        then:
        !answer.error
        answer.value.children[0].complete
        !answer.value.children[1].started
        !answer.value.root.complete
        models.writes*.key*.kind == ['dn', 'lv']
        models.writes*.key*.id == ['brush', 'brush']
        models.records[started.root.liveKey].status == QuestStatus.ACTIVE
        !models.records.containsKey(started.children[1].liveKey)

        when:
        models.writes.clear()
        def retry = result { intents.complete(day, root.key, target('brush'), { current }, it) }

        then:
        !retry.error
        retry.value.children[0].complete
        !retry.value.children[1].started
        models.writes.empty
    }

    def 'a missing #missing still starts before completing an existing checklist intent'() {
        given:
        def started = result { projection.start(day, root.key, it) }.value
        def missingKey = missing == 'root' ? started.root.liveKey : started.children[0].liveKey
        models.records.remove(missingKey)
        models.writes.clear()

        when:
        def answer = result { intents.complete(day, root.key, target('brush'), { current }, it) }

        then:
        !answer.error
        answer.value.root.started
        answer.value.children[0].complete
        answer.value.children[1].started
        !answer.value.root.complete
        models.writes*.key*.kind == ['lv', 'dn', 'lv']
        models.keyToString(models.writes[0].key) == missingKey
        models.writes.drop(1)*.key*.id == ['brush', 'brush']

        where:
        missing << ['root', 'target']
    }

    def 'ineligible target with missing sibling is rejected before any start writes'() {
        given:
        def started = result { projection.start(day, root.key, it) }.value
        models.records[started.children[0].liveKey].status = status
        models.records.remove(started.children[1].liveKey)
        models.writes.clear()
        when:
        def answer = result { intents.complete(day, root.key, target('brush'), { current }, it) }
        then:
        answer.error
        models.writes.empty
        where:
        status << [QuestStatus.PAUSED, QuestStatus.CANCELLED, QuestStatus.ARCHIVED]
    }

    def 'target outside captured checklist and premature root completion cannot start records'() {
        when:
        def wrong = result { intents.complete(day, root.key, day.key.getChild('lv', 'different'), { current }, it) }
        def premature = result { intents.complete(day, root.key, day.key.getChild('lv', root.key.id), { current }, it) }
        then:
        wrong.error && premature.error
        models.writes.empty
    }
}
