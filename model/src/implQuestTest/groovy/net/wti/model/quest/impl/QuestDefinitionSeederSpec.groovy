package net.wti.model.quest.impl

import net.wti.quest.api.ChildRef
import net.wti.quest.api.LiveQuest
import net.wti.quest.api.QuestCompleted
import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.QuestStatus
import net.wti.quest.model.impl.QuestDefinitionLoaderImpl
import net.wti.quest.model.impl.QuestDefinitionSeeder
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll
import xapi.jre.model.ModelServiceJre
import xapi.model.X_Model
import xapi.model.api.Model
import xapi.model.api.ModelKey
import xapi.model.api.ModelNotFoundException
import xapi.util.api.ErrorHandler
import xapi.util.api.SuccessHandler

import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class QuestDefinitionSeederSpec extends Specification {

    @TempDir File directory

    def setupSpec() {
        def service = X_Model.getService() as ModelServiceJre
        [QuestDefinition, ChildRef].each { service.register(it); service.getOrMakeModelManifest(it) }
    }

    private ModelServiceJre backend() {
        def service = new ModelServiceJre()
        service.setRootDir(directory)
        [QuestDefinition, ChildRef, LiveQuest, QuestCompleted].each {
            service.register(it)
            service.getOrMakeModelManifest(it)
        }
        service
    }

    private static List<QuestDefinition> definitions() {
        def result = []
        new QuestDefinitionLoaderImpl().loadFromClasspath { resource, definition ->
            if (resource.endsWith('packing-checklists.xapi')) result << definition
        }
        assert result.size() == 5
        result
    }

    private static Map outcome(Closure action) {
        def future = new CompletableFuture<Map>()
        action(SuccessHandler.handler({ value -> future.complete([value: value]) },
                { error -> future.complete([error: error]) }))
        future.get(10, TimeUnit.SECONDS)
    }

    private static Map seed(ModelServiceJre service, String namespace, List<QuestDefinition> definitions) {
        outcome { callback -> new QuestDefinitionSeeder(service).seedMissing(namespace, definitions, callback) }
    }

    private static Model load(ModelServiceJre service, Class type, ModelKey key) {
        def result = outcome { callback -> service.load(type, key, callback) }
        assert !result.error
        result.value as Model
    }

    private static void persist(ModelServiceJre service, Model model) {
        assert !outcome { callback -> service.persist(model, callback) }.error
    }

    private Map snapshot() {
        def files = [:]
        directory.eachFileRecurse { file ->
            if (file.isFile()) files[directory.toPath().relativize(file.toPath()).toString()] =
                    MessageDigest.getInstance('SHA-256').digest(Files.readAllBytes(file.toPath())).encodeHex().toString()
        }
        files
    }

    def "real XApi definitions reopen with concrete references qualified to the explicit definition namespace"() {
        given:
        def authored = definitions()

        when:
        def seeded = seed(backend(), 'definitions-alpha', authored)
        def reopened = backend()
        def key = reopened.newKey('definitions-alpha', 'qdef', 'packing-list')
        def packing = load(reopened, QuestDefinition, key) as QuestDefinition
        def refs = packing.children().collect { it }

        then:
        seeded == [value: 5]
        reopened.keyToString(packing.key) == reopened.keyToString(key)
        packing.children().modelType == ChildRef
        refs*.definitionKey*.namespace == ['definitions-alpha'] * 3
        refs*.definitionKey*.id == ['pack-water', 'pack-snack', 'pack-hat']
        refs[1].quantity == 2
        refs.every { load(reopened, QuestDefinition, it.definitionKey) != null }
        authored.every { it.key.namespace == '' }
        authored.find { it.key.id == 'packing-list' }.children().every { it.definitionKey.namespace == '' }
    }

    def "reruns preserve edited definitions and terminal progress byte-for-byte after a fresh backend opens"() {
        given:
        def initial = backend()
        assert seed(initial, 'definitions-alpha', definitions()).value == 5
        def packing = load(initial, QuestDefinition, initial.newKey('definitions-alpha', 'qdef', 'packing-list'))
        packing.title = 'Family edited title'
        packing.children().iterator().next().quantity = 7
        persist(initial, packing)
        def day = initial.newKey('progress-owner', 'dy', '7')
        def live = initial.create(LiveQuest)
        live.key = initial.newKey('progress-owner', 'lv', 'packing-list').setParent(day)
        live.parentDayKey = day
        live.liveKey = 'packing-list'
        live.dayIndex = 7
        live.status = QuestStatus.FINISHED
        live.finishedAtMillis = 12345L
        live.updatedAtMillis = 12345L
        persist(initial, live)
        def completed = initial.create(QuestCompleted)
        completed.key = initial.newKey('progress-owner', 'dn', 'packing-list').setParent(day)
        completed.instanceKey = live.key
        completed.dayIndex = 7L
        completed.occurredAtMillis = 12345L
        persist(initial, completed)
        def before = snapshot()

        when:
        def seeded = seed(backend(), 'definitions-alpha', definitions())
        def reopened = backend()
        def oldLive = load(reopened, LiveQuest, live.key) as LiveQuest

        then:
        seeded == [value: 0]
        snapshot() == before
        (load(reopened, QuestDefinition, packing.key) as QuestDefinition).title == 'Family edited title'
        oldLive.status == QuestStatus.FINISHED
        oldLive.finishedAtMillis == 12345L
        reopened.keyToString(oldLive.key) == reopened.keyToString(live.key)
        reopened.keyToString((load(reopened, QuestCompleted, completed.key) as QuestCompleted).instanceKey) ==
                reopened.keyToString(live.key)
    }

    def "a partial seed fills only missing keys and identical ids in another namespace stay independent"() {
        given:
        def subset = definitions().findAll { it.key.id != 'pack-hat' }
        assert seed(backend(), 'definitions-alpha', subset).value == 4
        def before = snapshot()

        when:
        def filled = seed(backend(), 'definitions-alpha', definitions())
        def other = seed(backend(), 'definitions-beta', definitions())

        then:
        filled == [value: 1]
        other == [value: 5]
        before.every { path, hash -> snapshot()[path] == hash }
        (load(backend(), QuestDefinition, backend().newKey('definitions-beta', 'qdef', 'packing-list')) as QuestDefinition)
                .children().every { it.definitionKey.namespace == 'definitions-beta' }
    }

    def "corrupt persisted data fails the seed without replacing the file or creating later definitions"() {
        given:
        def broken = new File(directory, 'definitions-alpha/qdef/pack-water')
        broken.parentFile.mkdirs()
        broken.setText('not a serialized model', 'UTF-8')
        def before = snapshot()

        when:
        def result = seed(backend(), 'definitions-alpha', definitions())

        then:
        result.error != null
        !result.containsKey('value')
        snapshot() == before
    }

    def "success waits for acknowledged persistence rather than returning when the write starts"() {
        given:
        def service = new DeferredBackend()
        def results = []
        def errors = []
        new QuestDefinitionSeeder(service).seedMissing('definitions-alpha', definitions().take(1),
                SuccessHandler.handler({ results << it }, { errors << it }))

        expect:
        results.empty
        service.pending.size() == 1

        when:
        service.pending.remove(0).call() // missing load -> pending persist

        then:
        results.empty
        errors.empty
        service.pending.size() == 1

        when:
        service.pending.remove(0).call()

        then:
        results == [1]
        errors.empty
    }

    @Unroll
    def "#mode failure is never treated as permission to seed"() {
        given:
        def service = new DeferredBackend(mode: mode)
        def results = []
        def errors = []
        new QuestDefinitionSeeder(service).seedMissing('definitions-alpha', definitions().take(1),
                SuccessHandler.handler({ results << it }, { errors << it }))

        when:
        service.pending.remove(0).call()

        then:
        errors.size() == 1
        results.empty
        service.writes == 0
        service.pending.empty

        where:
        mode << ['storage', 'wrapped-missing', 'wrong-missing-key', 'null-result']
    }

    def "write failure is reported without successful batch completion"() {
        given:
        def service = new DeferredBackend(mode: 'write-failure')
        def results = []
        def errors = []
        new QuestDefinitionSeeder(service).seedMissing('definitions-alpha', definitions().take(1),
                SuccessHandler.handler({ results << it }, { errors << it }))

        when:
        service.pending.remove(0).call()
        service.pending.remove(0).call()

        then:
        errors.size() == 1
        results.empty
        service.writes == 1
    }

    def "duplicate source identities fail validation before any backend request"() {
        given:
        def service = new DeferredBackend()
        def same = definitions().first()

        when:
        def result = seed(service, 'definitions-alpha', [same, same])

        then:
        result.error instanceof IllegalArgumentException
        service.pending.empty
        service.writes == 0
    }

    static class DeferredBackend extends ModelServiceJre {
        List<Closure> pending = []
        String mode = ''
        int writes

        @Override
        void load(Class type, ModelKey key, SuccessHandler callback) {
            pending << {
                if (mode == 'null-result') callback.onSuccess(null)
                else if (mode == 'storage') (callback as ErrorHandler).onError(new IOException('read denied'))
                else if (mode == 'wrapped-missing')
                    (callback as ErrorHandler).onError(new ModelNotFoundException(key, new IOException('read denied')))
                else if (mode == 'wrong-missing-key')
                    (callback as ErrorHandler).onError(new ModelNotFoundException(
                            newKey(key.namespace, key.kind, key.id).setParent(newKey(key.namespace, 'other', 'parent'))))
                else (callback as ErrorHandler).onError(new ModelNotFoundException(key))
            }
        }

        @Override
        void persist(Model model, SuccessHandler callback) {
            writes++
            pending << {
                if (mode == 'write-failure') (callback as ErrorHandler).onError(new IOException('write denied'))
                else callback.onSuccess(model)
            }
        }
    }
}
