package net.wti.quest.impl

import net.wti.quest.api.*
import net.wti.time.api.ModelDay
import spock.lang.Specification
import xapi.jre.model.ModelServiceJre
import xapi.model.api.*
import xapi.util.api.ErrorHandler
import xapi.util.api.SuccessHandler
import xapi.fu.java.X_Jdk

class ChecklistProjectionServiceSpec extends Specification {
    Backend models = new Backend()
    ChecklistProjectionService service = new ChecklistProjectionService(models)
    ModelDay day
    QuestDefinition root

    def setup() {
        day = models.create(ModelDay)
        day.dayNum = 42
        day.key = models.newKey('child-a', ModelDay.MODEL_DAY, '42').setKeyType(ModelKey.KEY_TYPE_LONG)
        root = definition(models, 'packing', ['water', 'snack'])
    }

    def 'definition browse has no owner or write and preserves member order and quantity'() {
        when:
        def answer = result { service.loadDefinition(root.key, it) }
        then:
        !answer.error
        answer.value.children*.title == ['water', 'snack']
        answer.value.children*.quantity == [1, 2]
        answer.value.root.liveKey == null
        !answer.value.canStart()
        !answer.value.root.canComplete()
        models.writes.empty
        when:
        answer.value.children.clear()
        then:
        thrown(UnsupportedOperationException)
    }

    def 'load is read only and explicit start fills once with complete nested ownership'() {
        expect:
        result { service.load(day, root.key, it) }.value.canStart()
        models.writes.empty
        when:
        def first = result { service.start(day, root.key, it) }
        def second = result { service.start(day, root.key, it) }
        then:
        !first.error && !second.error
        models.writes.size() == 3
        first.value.children.every { it.started && it.canComplete() }
        !first.value.root.canComplete()
        def child = models.keyFromString(first.value.children[0].liveKey)
        child.namespace == 'child-a'
        child.parent.id == 'packing'
        child.parent.parent.keyType == ModelKey.KEY_TYPE_LONG
        models.keyToString(child.parent.parent) == models.keyToString(day.key)
    }

    def 'shared child definitions have independent list progress and immutable render values'() {
        given:
        def other = definition(models, 'outing', ['water'])
        def first = result { service.start(day, root.key, it) }.value
        def second = result { service.start(day, other.key, it) }.value
        when:
        def stored = models.records[first.children[0].liveKey]
        stored.title = 'changed later'
        then:
        first.children[0].title == 'water'
        first.children[0].liveKey != second.children[0].liveKey
    }

    def 'existing terminal progress is preserved by start'() {
        given:
        def started = result { service.start(day, root.key, it) }.value
        def child = models.records[started.children[0].liveKey]
        child.status = QuestStatus.FINISHED
        child.finishedAtMillis = 123L
        models.writes.clear()
        when:
        def again = result { service.start(day, root.key, it) }
        then:
        !again.error
        again.value.children[0].complete
        child.finishedAtMillis == 123L
        models.writes.empty
    }

    def 'explicit inheritance and additional tags are copied without mutating definitions'() {
        given:
        root.tags = ['family'] as String[]
        def reference = root.children().iterator().next()
        reference.inheritTags = true
        reference.additionalTags = ['bag', 'family'] as String[]
        def water = models.records.values().find { it instanceof QuestDefinition && it.key.id == 'water' }
        water.tags = ['item'] as String[]
        models.put(root)
        when:
        def answer = result { service.start(day, root.key, it) }
        then:
        !answer.error
        models.records[answer.value.children[0].liveKey].tags as List == ['family', 'item', 'bag']
        water.tags as List == ['item']
    }

    def 'partial start failure stays visible and retry fills only missing records'() {
        given:
        models.failPersistId = 'snack'
        when:
        def failed = result { service.start(day, root.key, it) }
        def loaded = result { service.load(day, root.key, it) }.value
        then:
        failed.error
        loaded.root.started
        loaded.children*.started == [true, false]
        loaded.canStart()
        when:
        models.failPersistId = null
        models.writes.clear()
        def retry = result { service.start(day, root.key, it) }
        then:
        !retry.error
        models.writes*.key*.id == ['snack']
    }

    def 'existing restricted progress rejects load and start before filling missing siblings'() {
        given:
        def started = result { service.start(day, root.key, it) }.value
        def restricted = models.records[target == 'root' ? started.root.liveKey : started.children[0].liveKey]
        restrict(models, restricted, restriction)
        models.records.remove(started.children[1].liveKey)
        models.writes.clear()
        when:
        def loaded = result { service.load(day, root.key, it) }
        def attempted = result { service.start(day, root.key, it) }
        then:
        loaded.error
        attempted.error
        models.writes.empty
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

    def 'inactive definitions keep missing membership visible without advertising start'() {
        given:
        def inactive = target == 'root' ? root : models.records.values().find { it instanceof QuestDefinition && it.key.id == 'water' }
        inactive.active = false
        models.put(inactive)
        when:
        def loaded = result { service.load(day, root.key, it) }
        def attempted = result { service.start(day, root.key, it) }
        then:
        !loaded.error
        !loaded.value.canStart()
        loaded.value.children*.title == ['water', 'snack']
        loaded.value.children.every { !it.started }
        attempted.error
        models.writes.empty
        where:
        target << ['root', 'child']
    }

    def 'finished root with missing child stays complete and cannot offer finish starting'() {
        given:
        def started = result { service.start(day, root.key, it) }.value
        models.records[started.root.liveKey].status = QuestStatus.FINISHED
        models.records.remove(started.children[1].liveKey)
        models.writes.clear()
        when:
        def loaded = result { service.load(day, root.key, it) }
        def attempted = result { service.start(day, root.key, it) }
        then:
        !loaded.error
        loaded.value.root.complete
        !loaded.value.canStart()
        !loaded.value.children[1].started
        attempted.error
        models.writes.empty
    }

    def 'all unsupported composition validates before any progress writes'() {
        given:
        if (invalid == 'policy') root.completionPolicy = CompletionPolicy.ANY_OF
        if (invalid == 'role') root.children().iterator().next().role = ChildRole.OPTIONAL
        if (invalid == 'duplicate') {
            ChildRef duplicate = models.create(ChildRef)
            duplicate.definitionKey = root.children().iterator().next().definitionKey
            duplicate.quantity = 7
            root.children().add(duplicate)
        }
        if (invalid == 'cycle') root.children().iterator().next().definitionKey = root.key
        if (invalid == 'nested') definition(models, 'water', ['more'])
        if (invalid == 'requirements') root.toComplete = models.create(QuestRequirements)
        if (invalid == 'users-empty' || invalid == 'groups-empty') restrict(models, root, invalid)
        models.put(root)
        when:
        def answer = result { service.start(day, root.key, it) }
        then:
        answer.error
        models.writes.empty
        where:
        invalid << ['policy', 'role', 'duplicate', 'cycle', 'nested', 'requirements', 'users-empty', 'groups-empty']
    }

    def 'read errors and mismatched missing identities never become replacement writes'() {
        given:
        def liveKey = day.key.getChild('lv', root.key.id)
        models.loadErrors[models.keyToString(liveKey)] = errorKind == 'caused' ?
                new ModelNotFoundException(liveKey, new IOException('corrupt')) :
                new ModelNotFoundException(day.key.getChild('lv', 'different'))
        when:
        def answer = result { service.start(day, root.key, it) }
        then:
        answer.error
        models.writes.empty
        where:
        errorKind << ['caused', 'identity']
    }

    def 'same-id definition collision fails without altering existing progress'() {
        given:
        result { service.start(day, root.key, it) }
        def rival = definition(models, 'packing', ['water'], 'different-definition-owner')
        models.writes.clear()
        when:
        def answer = result { service.start(day, rival.key, it) }
        then:
        answer.error
        models.writes.empty
    }

    def 'start waits for each acknowledgment before reporting success'() {
        given:
        models.defer = true
        when:
        def answer = result { service.start(day, root.key, it) }
        then:
        answer.isEmpty()
        models.queued.size() == 1
        when:
        models.ack()
        models.ack()
        then:
        answer.isEmpty()
        when:
        models.ack()
        then:
        answer.value.children.every { it.started }
    }

    def 'incorrect day key type fails before model IO'() {
        given:
        day.key.keyType = ModelKey.KEY_TYPE_STRING
        when:
        def answer = result { service.start(day, root.key, it) }
        then:
        answer.error
        models.loads.empty
        models.writes.empty
    }

    static Map result(Closure action) {
        Map answer = [:]
        action(SuccessHandler.handler({ value -> answer.value = value }, { error -> answer.error = error }))
        answer
    }

    static void restrict(Backend models, HasRequirements model, String restriction) {
        if (restriction == 'requirement') model.toComplete = models.create(QuestRequirements)
        if (restriction.startsWith('users-')) {
            // Existing XApi setAll-prefix parsing misroutes setAllowedUsers to owedUsers.
            // Exercise the canonical persisted field rather than that unrelated setter defect.
            model.setProperty('allowedUsers', X_Jdk.setLinked())
            assert model.getAllowedUsers() != null
            if (restriction.endsWith('nonempty')) model.getAllowedUsers().add('different-owner')
        }
        if (restriction.startsWith('groups-')) {
            model.setProperty('allowedGroups', X_Jdk.setLinked())
            assert model.getAllowedGroups() != null
            if (restriction.endsWith('nonempty')) model.getAllowedGroups().add('different-group')
        }
    }

    static QuestDefinition definition(Backend models, String id, List<String> children = [], String namespace = 'definitions') {
        QuestDefinition value = models.create(QuestDefinition)
        value.key = models.newKey(namespace, 'qdef', id)
        value.title = id
        value.active = true
        value.completionPolicy = CompletionPolicy.ALL_OF
        children.eachWithIndex { childId, i ->
            QuestDefinition child = models.records.values().find { it instanceof QuestDefinition && it.key.namespace == namespace && it.key.id == childId }
            if (child == null) child = definition(models, childId, [], namespace)
            ChildRef ref = models.create(ChildRef)
            ref.definitionKey = child.key
            ref.role = ChildRole.PARALLEL
            ref.startPolicy = ChildStartPolicy.AFTER_PARENT_START
            ref.quantity = i + 1
            value.children().add(ref)
        }
        models.put(value)
        value
    }

    /// Real X_Model values with deterministic callback scheduling/fault injection; no disk/network.
    static class Backend extends ModelServiceJre {
        Map<String, Model> records = [:]
        Map<String, Throwable> loadErrors = [:]
        List<Model> writes = []
        List<String> loads = []
        List<Map> queued = []
        boolean defer
        boolean writeThenFail
        String failPersistId
        String failPersistKind

        Backend() { [QuestDefinition, ChildRef, ModelList, LiveQuest, QuestCompleted, QuestSnapshot, ModelDay].each { register(it) } }
        void put(Model model) { records[keyToString(model.key)] = copy(model) }
        Model copy(Model model) {
            Model copy = create(typeToClass(model.type))
            copy.absorb(model)
            copy.key = keyFromString(keyToString(model.key))
            copy
        }
        @Override void load(Class type, ModelKey key, SuccessHandler callback) {
            String encoded = keyToString(key)
            loads << encoded
            if (loadErrors.containsKey(encoded)) (callback as ErrorHandler).onError(loadErrors[encoded])
            else if (records.containsKey(encoded)) callback.onSuccess(copy(records[encoded]))
            else (callback as ErrorHandler).onError(new ModelNotFoundException(key))
        }
        @Override void persist(Model model, SuccessHandler callback) {
            writes << copy(model)
            if (defer) queued << [model: copy(model), callback: callback]
            else commit(model, callback)
        }
        void ack() {
            def next = queued.remove(0)
            commit(next.model as Model, next.callback as SuccessHandler)
        }
        void commit(Model model, SuccessHandler callback) {
            boolean fails = model.key.id == failPersistId || model.key.kind == failPersistKind
            if (!fails || writeThenFail) put(model)
            if (fails) (callback as ErrorHandler).onError(new IOException('injected write failure'))
            else callback.onSuccess(copy(model))
        }
    }
}
