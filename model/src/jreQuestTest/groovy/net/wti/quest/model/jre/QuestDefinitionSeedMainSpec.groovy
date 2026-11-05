package net.wti.quest.model.jre

import net.wti.quest.api.ChildRef
import net.wti.quest.api.LiveQuest
import net.wti.quest.api.QuestCompleted
import net.wti.quest.api.QuestDefinition
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll
import xapi.jre.model.ModelServiceJre
import xapi.model.api.Model
import xapi.model.tools.ModelSerializerDefault
import xapi.source.lex.StringCharIterator
import xapi.util.api.SuccessHandler

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import xapi.model.api.ModelKey

class QuestDefinitionSeedMainSpec extends Specification {

    @TempDir File directory

    private String[] arguments() {
        def seedRoot = new File(directory, 'source')
        def input = new File(seedRoot, 'META-INF/models/qdef/packing-checklists.xapi')
        input.parentFile.mkdirs()
        Files.copy(new File(System.getProperty('quest.seed.fixture')).toPath(), input.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        ['--seed-root=' + seedRoot.absolutePath,
         '--model-dir=' + new File(directory, 'models').absolutePath,
         '--namespace=definition-owner',
         '--manifest-out=' + new File(directory, 'wire/lifequest-quests.rpc').absolutePath] as String[]
    }

    private static Object await(Closure action) {
        def result = new CompletableFuture<>()
        action(SuccessHandler.handler({ result.complete(it) }, { result.completeExceptionally(it) }))
        result.get(10, TimeUnit.SECONDS)
    }

    def "the noninteractive command seeds only its explicit XApi root and reruns without replacing edits"() {
        given:
        def args = arguments()

        when:
        def first = QuestDefinitionSeedMain.run(args, new ModelServiceJre())
        def artifact = new File(directory, 'wire/lifequest-quests.rpc')
        def originalManifest = artifact.getText('UTF-8')
        def module = ModelSerializerDefault.deserializeModule(originalManifest)
        def reopened = new ModelServiceJre()
        reopened.setRootDir(new File(directory, 'models'))
        QuestModelModule.create(reopened)
        def key = reopened.newKey('definition-owner', 'qdef', 'packing-list')
        def definition = await { reopened.load(QuestDefinition, key, it) } as QuestDefinition
        definition.title = 'Edited after seeding'
        await { reopened.persist(definition, it) }
        def second = QuestDefinitionSeedMain.run(args, new ModelServiceJre())
        def restored = await { reopened.load(QuestDefinition, key, it) } as QuestDefinition

        then:
        first == 5 // test-tools' unrelated classpath qdefs were not seeded
        second == 0
        module.moduleName == QuestModelModule.NAME
        module.uuid != null
        restored.title == 'Edited after seeding'
        restored.children().collect { it.definitionKey.namespace } == ['definition-owner'] * 3
        artifact.getText('UTF-8') == originalManifest
        !new File(directory, 'models/definition-owner/qdef/test-root-a').exists()
    }

    def "exported module has deterministic complete model closure and preserves exact serialization order"() {
        given:
        def models = new ModelServiceJre()
        def module = QuestModelModule.create(models)

        when:
        def wire = ModelSerializerDefault.serialize(module)
        def decoded = ModelSerializerDefault.deserializeModule(wire)
        def second = ModelSerializerDefault.serialize(QuestModelModule.create(new ModelServiceJre()))

        then:
        wire == second
        decoded.moduleName == QuestModelModule.NAME
        decoded.getManifest('qdef').getMethodData('children').typeParams as List == [ChildRef]
        decoded.getManifest('lv').propertyNames as List == models.findManifest(LiveQuest).propertyNames as List
        decoded.getManifest('dn').propertyNames as List == models.findManifest(QuestCompleted).propertyNames as List
        ['qdef', 'qchild', 'lv', 'dn', 'qrule', 'qreq', 'qsnap', 'dur', 'tanc', 'list', 'day'].every {
            decoded.getManifest(it) != null
        }
        module.manifests.every { manifest ->
            manifest.propertyNames.every { name ->
                def data = manifest.getMethodData(name)
                def types = [data.type.isArray() ? data.type.componentType : data.type] + ((data.typeParams ?: []) as List)
                types.every { type ->
                    type == Model || !Model.isAssignableFrom(type) || decoded.manifests.any { it.modelType == type }
                }
            }
        }
    }

    def "literal plus and escaped punctuation in the explicit seed root survive file URL loading"() {
        given:
        def args = arguments()
        def source = new File(directory, 'source').toPath()
        def renamed = new File(directory, 'source + bag %23 # 水').toPath()
        Files.move(source, renamed)
        args = args.collect { it.startsWith('--seed-root=') ? '--seed-root=' + renamed : it } as String[]

        expect:
        QuestDefinitionSeedMain.run(args, new ModelServiceJre()) == 5
        QuestDefinitionSeedMain.run(args, new ModelServiceJre()) == 0
        new File(directory, 'models/definition-owner/qdef/packing-list').isFile()
    }

    def "a failed manifest publication can retry without replacing already seeded definitions"() {
        given:
        def args = arguments()
        def blockedParent = new File(directory, 'wire')
        blockedParent.setText('blocks the output directory', 'UTF-8')

        when:
        QuestDefinitionSeedMain.run(args, new ModelServiceJre())

        then:
        thrown(IOException)
        new File(directory, 'models/definition-owner/qdef/packing-list').isFile()
        !new File(directory, 'wire/lifequest-quests.rpc').exists()

        when:
        def before = new File(directory, 'models/definition-owner/qdef').listFiles().collectEntries {
            [(it.name): it.bytes.toList()]
        }
        Files.delete(blockedParent.toPath())
        def created = QuestDefinitionSeedMain.run(args, new ModelServiceJre())
        def after = new File(directory, 'models/definition-owner/qdef').listFiles().collectEntries {
            [(it.name): it.bytes.toList()]
        }

        then:
        created == 0
        after == before
        new File(directory, 'wire/lifequest-quests.rpc').isFile()
    }

    def "manifest publication ignores abandoned staging and accepts only identical existing output"() {
        given:
        def artifact = new File(directory, 'wire/policy.rpc').toPath()
        Files.createDirectories(artifact.parent)
        def abandoned = artifact.parent.resolve('.lifequest-manifest-abandoned.tmp')
        Files.write(abandoned, 'incomplete'.getBytes(StandardCharsets.UTF_8))
        def expected = 'complete policy — 水'.getBytes(StandardCharsets.UTF_8)

        when:
        QuestDefinitionSeedMain.publishManifest(artifact, expected)
        QuestDefinitionSeedMain.publishManifest(artifact, expected)

        then:
        Files.readAllBytes(artifact) == expected
        Files.list(artifact.parent).withCloseable { paths ->
            paths.map { it.fileName.toString() }.sorted().toArray().toList()
        } == ['.lifequest-manifest-abandoned.tmp', 'policy.rpc']

        when:
        QuestDefinitionSeedMain.publishManifest(artifact, 'different policy'.getBytes(StandardCharsets.UTF_8))

        then:
        thrown(IOException)
        Files.readAllBytes(artifact) == expected
        Files.list(artifact.parent).withCloseable { it.count() } == 2
    }

    def "omitted child quantities survive source loading and the real exported policy store roundtrip"() {
        given:
        def args = arguments()
        QuestModelModule.create((ModelServiceJre) xapi.model.X_Model.service)
        def authored = [:]
        def resources = new URLClassLoader([new File(directory, 'source').toURI().toURL()] as URL[], (ClassLoader) null)
        new net.wti.quest.model.impl.QuestDefinitionLoaderImpl() {
            @Override protected ClassLoader classLoader() { resources }
        }.loadFromClasspath { name, definition ->
            if (definition.children) authored[definition.key.id] = definition.children().collect { it.quantity }
        }
        resources.close()

        when:
        def created = QuestDefinitionSeedMain.run(args, new ModelServiceJre())
        def module = ModelSerializerDefault.deserializeModule(new File(directory, 'wire/lifequest-quests.rpc').getText('UTF-8'))
        def reopened = new ModelServiceJre()
        reopened.rootDir = new File(directory, 'models')
        module.manifests.each { reopened.register(it.modelType); reopened.registerManifest(it) }
        def scope = ModelServiceJre.registerModule(module)
        def loaded = [:]
        ['packing-list', 'short-outing'].each { id ->
            def definition = await { reopened.load(QuestDefinition, reopened.newKey('definition-owner', 'qdef', id), it) }
            loaded[id] = definition.children().collect { it.quantity }
        }
        println 'Child quantities authored=' + authored + ', actual policy/store reload=' + loaded

        then:
        created == 5
        authored == ['packing-list': [1, 2, 1], 'short-outing': [1, 1]]
        loaded == authored
        loaded.values().flatten().every { it > 0 }

        cleanup:
        scope?.remove()
        resources?.close()
    }

    def "the exact exported policy reads a serialized definition including multiline UTF8 and concrete children"() {
        given:
        def args = arguments()
        QuestDefinitionSeedMain.run(args, new ModelServiceJre())
        def module = ModelSerializerDefault.deserializeModule(new File(directory, 'wire/lifequest-quests.rpc').getText('UTF-8'))
        def models = new ModelServiceJre()
        models.setRootDir(new File(directory, 'models'))
        module.manifests.each { models.register(it.modelType); models.registerManifest(it) }
        def scope = ModelServiceJre.registerModule(module)

        when:
        def key = models.newKey('definition-owner', 'qdef', 'packing-list')
        def source = await { models.load(QuestDefinition, key, it) } as QuestDefinition
        source.description = 'First line\nKid’s bag — 水'
        def encoded = models.serialize(module.getManifest('qdef'), source).toSource()
        def decoded = models.deserialize(module.getManifest('qdef'), new StringCharIterator(encoded)) as QuestDefinition

        then:
        models.keyToString(decoded.key) == models.keyToString(key)
        decoded.description == source.description
        decoded.children().modelType == ChildRef
        decoded.children().collect { it.definitionKey.id } == ['pack-water', 'pack-snack', 'pack-hat']

        cleanup:
        scope?.remove()
    }

    def "a different existing manifest fails before writing any model data"() {
        given:
        def args = arguments()
        def artifact = new File(directory, 'wire/lifequest-quests.rpc')
        artifact.parentFile.mkdirs()
        artifact.setText('previous policy must survive', 'UTF-8')

        when:
        QuestDefinitionSeedMain.run(args, new ModelServiceJre())

        then:
        thrown(IOException)
        artifact.getText('UTF-8') == 'previous policy must survive'
        !new File(directory, 'models').exists()
    }

    def "a dangling manifest entry is rejected before seed writes"() {
        given:
        def args = arguments()
        def artifact = new File(directory, 'wire/lifequest-quests.rpc').toPath()
        Files.createDirectories(artifact.parent)
        Files.createSymbolicLink(artifact, new File(directory, 'absent-policy').toPath())

        when:
        QuestDefinitionSeedMain.run(args, new ModelServiceJre())

        then:
        thrown(IOException)
        Files.isSymbolicLink(artifact)
        !new File(directory, 'models').exists()
        !new File(directory, 'absent-policy').exists()
    }

    def "a disappeared create-conflict artifact is not accepted as identical"() {
        given:
        def artifact = new File(directory, 'conflict.rpc').toPath()
        Files.write(artifact, 'policy'.getBytes(StandardCharsets.UTF_8))
        Files.delete(artifact)

        when:
        QuestDefinitionSeedMain.requireIdenticalManifest(artifact, 'policy'.getBytes(StandardCharsets.UTF_8))

        then:
        thrown(IOException)
    }

    @Unroll
    def "unsafe namespace #namespace cannot write outside its explicit store"() {
        given:
        def args = arguments().collect { it.startsWith('--namespace=') ? '--namespace=' + namespace : it }

        when:
        QuestDefinitionSeedMain.run(args as String[], new ModelServiceJre())

        then:
        thrown(IllegalArgumentException)
        !new File(directory, 'models').exists()
        !new File(directory, 'escape').exists()

        where:
        namespace << ['../escape', 'a/b', 'a\\b', '.', '..']
    }

    def "all authored ids are checked before any earlier definition can be written"() {
        given:
        def args = arguments()
        def input = new File(directory, 'source/META-INF/models/qdef/packing-checklists.xapi')
        input.setText(input.getText('UTF-8').replace('pack-hat', '../escape'), 'UTF-8')

        when:
        QuestDefinitionSeedMain.run(args, new ModelServiceJre())

        then:
        thrown(IllegalArgumentException)
        !new File(directory, 'models').exists()
        !new File(directory, 'escape').exists()
    }

    @Unroll
    def "existing #aliasKind aliases cannot redirect the seed"() {
        given:
        def args = arguments()
        def store = new File(directory, 'models').toPath()
        def outside = new File(directory, 'outside').toPath()
        Files.createDirectories(outside)
        Files.createDirectories(store)
        def alias = aliasKind == 'namespace' ? store.resolve('definition-owner') :
                store.resolve('definition-owner/qdef')
        Files.createDirectories(alias.parent)
        Files.createSymbolicLink(alias, outside)

        when:
        QuestDefinitionSeedMain.run(args, new ModelServiceJre())

        then:
        thrown(IOException)
        Files.list(outside).withCloseable { it.count() } == 0
        !new File(directory, 'wire/lifequest-quests.rpc').exists()

        where:
        aliasKind << ['namespace', 'bucket']
    }

    def "manifest and model store overlap is checked through canonical aliases"() {
        given:
        def args = arguments()
        def store = new File(directory, 'models').toPath()
        Files.createDirectories(store)
        Files.createSymbolicLink(new File(directory, 'wire').toPath(), store)

        when:
        QuestDefinitionSeedMain.run(args, new ModelServiceJre())

        then:
        thrown(IllegalArgumentException)
        Files.list(store).withCloseable { it.count() } == 0
    }

    def "ordinary id punctuation and Unicode namespaces stay legal filename components"() {
        given:
        def args = arguments().collect { it.startsWith('--namespace=') ? '--namespace=family & kids — 水' : it }
        def input = new File(directory, 'source/META-INF/models/qdef/packing-checklists.xapi')
        input.setText(input.getText('UTF-8').replace('pack-water', 'water & snacks = lunch?'), 'UTF-8')

        expect:
        QuestDefinitionSeedMain.run(args as String[], new ModelServiceJre()) == 5
        new File(directory, 'models/family & kids — 水/qdef/water & snacks = lunch?').isFile()
    }

    def "interruption waits for actual producer settlement before returning and restores the signal"() {
        given:
        def args = arguments()
        def backend = new GatedBackend()
        def done = new CompletableFuture<Integer>()
        def interruptRestored = new AtomicBoolean()
        def thread = new Thread({
            try {
                def acknowledged = QuestDefinitionSeedMain.run(args, backend)
                interruptRestored.set(Thread.currentThread().isInterrupted())
                done.complete(acknowledged)
            }
            catch (Throwable error) { done.completeExceptionally(error) }
        })
        thread.start()
        assert backend.entered.await(5, TimeUnit.SECONDS)

        when:
        thread.interrupt()

        then:
        !done.isDone()

        when:
        backend.release.countDown()
        def created = done.get(10, TimeUnit.SECONDS)
        thread.join(5000)

        then:
        created == 5
        !thread.alive
        interruptRestored.get()
        new File(directory, 'wire/lifequest-quests.rpc').isFile()

        cleanup:
        backend?.release?.countDown()
        thread?.join(5000)
    }

    static class GatedBackend extends ModelServiceJre {
        final CountDownLatch entered = new CountDownLatch(1)
        final CountDownLatch release = new CountDownLatch(1)
        boolean gated

        @Override
        void load(Class type, ModelKey key, SuccessHandler callback) {
            if (!gated) {
                gated = true
                entered.countDown()
                Thread.start { release.await(); continueLoad(type, key, callback) }
            } else continueLoad(type, key, callback)
        }

        private void continueLoad(Class type, ModelKey key, SuccessHandler callback) {
            super.load(type, key, callback)
        }
    }

    @Unroll
    def "invalid CLI arguments fail without falling back to a default model directory: #args"() {
        when:
        QuestDefinitionSeedMain.run(args as String[], new ModelServiceJre())

        then:
        thrown(IllegalArgumentException)

        where:
        args << [[], ['--model-dir=x'], ['--wat=x'], ['--namespace=a', '--namespace=b']]
    }
}
