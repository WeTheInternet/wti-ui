package net.wti.model.quest.impl

import net.wti.model.test.AbstractModelLoaderSpec
import net.wti.quest.api.QuestDefinition
import net.wti.quest.api.ChildRef
import net.wti.quest.api.ChildRole
import net.wti.quest.api.ChildStartPolicy
import net.wti.quest.api.CompletionPolicy
import net.wti.quest.model.impl.QuestDefinitionLoaderImpl
import spock.lang.Unroll
import xapi.jre.model.ModelServiceJre
import xapi.model.X_Model

///
/// QuestDefinitionLoaderImplSpec:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 23:36
class QuestDefinitionLoaderImplSpec extends AbstractModelLoaderSpec {

    def "template titles preserve quotes and backslashes as literal authored text"() {
        given:
        QuestDefinition result

        when:
        new InlineLoader(source: '<QuestDefinition id = "morning" title = `Morning "ready" \\ list` />')
                .loadFromClasspath { resource, value -> result = value }

        then:
        result.title == 'Morning "ready" \\ list'
    }

    def "authored member identity survives reordering and actual wire roundtrip"() {
        given:
        def read = { String children ->
            QuestDefinition result
            new InlineLoader(source: '<QuestDefinition children = [' + children +
                    '] id = "morning" title = "Morning" />').loadFromClasspath { resource, value -> result = value }
            X_Model.deserialize(QuestDefinition, X_Model.serialize(QuestDefinition, result))
        }
        def a = '<ChildRef memberId = "clothes" definitionKey = "ready" />'
        def b = '<ChildRef memberId = "teeth" definitionKey = "ready" />'

        when:
        def original = read(a + ',' + b)
        def reordered = read(b + ',' + a)

        then:
        original.children().collect { it.key.id } == ['clothes', 'teeth']
        reordered.children().collect { it.key.id } == ['teeth', 'clothes']
        original.children().collect { it.definitionKey.id } == ['ready', 'ready']
        original.children().every { it.key.parent == null && it.key.namespace == '' }
    }

    @Unroll
    def "authored member ids reject duplicate and invalid identities: #members"() {
        when:
        new InlineLoader(source: '<QuestDefinition id = "morning" children = [' + members + '] />')
                .loadFromClasspath { resource, value -> }

        then:
        thrown(IllegalArgumentException)

        where:
        members << [
                '<ChildRef memberId = "same" definitionKey = "a" />, <ChildRef memberId = "same" definitionKey = "b" />',
                '<ChildRef memberId = "../escape" definitionKey = "a" />',
                '<ChildRef memberId = "" definitionKey = "a" />',
                '<ChildRef memberId = "0" definitionKey = "a" />, <ChildRef definitionKey = "b" />'
        ]
    }

    static class ExposedLoader extends QuestDefinitionLoaderImpl {
        Object manifestFor(final Class type) { findManifest(type) }
        String metaInfFor(final String suffix) { metaInfPath(suffix) }
    }

    static class InlineLoader extends QuestDefinitionLoaderImpl {
        String source

        @Override
        protected void loadAllFromClasspath(String suffix, String fileSuffix, ResourceStreamConsumer consumer) {
            consumer.accept('inline-checklist.xapi', new ByteArrayInputStream(source.getBytes('UTF-8')))
        }
    }

    void setupSpec() {
        final ModelServiceJre srv = X_Model.getService() as ModelServiceJre
        srv.register(QuestDefinition)
        srv.getOrMakeModelManifest(QuestDefinition)
        srv.register(ChildRef)
        srv.getOrMakeModelManifest(ChildRef)
    }

    def "loader resolves qdef path"() {
        expect:
        new ExposedLoader().metaInfFor("models/qdef") == "META-INF/models/qdef"
    }

    def "loader can resolve QuestDefinition manifest"() {
        expect:
        new ExposedLoader().manifestFor(QuestDefinition) != null
    }

    @Unroll
    def "fixture '#fixture' exists on classpath"() {
        expect:
        Thread.currentThread().contextClassLoader.getResource("META-INF/models/qdef/${fixture}") != null

        where:
        fixture << ["test-quests.xapi", "packing-checklists.xapi"]
    }

    def "loads test quest definitions with auto default semantics"() {
        given:
        def loaded = [] as List<QuestDefinition>
        def loader = new QuestDefinitionLoaderImpl()

        when:
        loader.loadFromClasspath { String resourceName, QuestDefinition qdef ->
            if (resourceName?.endsWith("test-quests.xapi")) {
                loaded.add(qdef)
            }
        }

        then:
        !loaded.isEmpty()
        loaded.every { it?.key != null }
        loaded.every { String.valueOf(it.key.id)?.trim() }

        and: "auto() defaults true when null"
        loaded.every { it.auto() || Boolean.FALSE.equals(it.getAuto()) }
    }

    def "packing lists load ordered reusable references with canonical defaults and metadata"() {
        given:
        def definitions = [:]

        when:
        new QuestDefinitionLoaderImpl().loadFromClasspath { resource, definition ->
            if (resource.endsWith('packing-checklists.xapi')) definitions[definition.key.id] = definition
        }
        def packing = definitions['packing-list'] as QuestDefinition
        def refs = packing.children().collect { it }
        def outing = definitions['short-outing'] as QuestDefinition

        then:
        definitions.size() == 5
        packing.completionPolicy == CompletionPolicy.ALL_OF
        packing.minimumRequired == 0
        refs*.definitionKey*.id == ['pack-water', 'pack-snack', 'pack-hat']
        refs.every { it instanceof ChildRef }
        refs*.role == [ChildRole.PARALLEL] * 3
        refs*.startPolicy == [ChildStartPolicy.AFTER_PARENT_START] * 3
        refs*.quantity == [1, 2, 1]
        refs[1].additionalTags as List == ['food', 'bag']
        refs[2].inheritTags
        outing.children().collect { it.definitionKey.id } == ['pack-water', 'pack-hat']
        outing.children().iterator().next().definitionKey == refs[0].definitionKey
        !outing.children().iterator().next().is(refs[0])
        definitions['pack-water'].auto() == false
        definitions['pack-water'].children().size() == 0
    }

    def "composition survives real XApi serialization with its concrete ChildRef type"() {
        given:
        QuestDefinition packing
        new QuestDefinitionLoaderImpl().loadFromClasspath { resource, definition ->
            if (definition.key.id == 'packing-list') packing = definition
        }

        when:
        def encoded = X_Model.serialize(QuestDefinition, packing)
        def reopened = X_Model.deserialize(QuestDefinition, encoded)
        def refs = reopened.children().collect { it }

        then:
        reopened.key == packing.key
        reopened.completionPolicy == CompletionPolicy.ALL_OF
        reopened.children().modelType == ChildRef
        refs*.definitionKey*.id == ['pack-water', 'pack-snack', 'pack-hat']
        refs[1].quantity == 2
        refs[2].quantity == 1
        refs[1].additionalTags as List == ['food', 'bag']
        refs[2].inheritTags
    }

    @Unroll
    def "tag arrays preserve each authored string and scalar tags retain legacy separators: #tagSource"() {
        given:
        QuestDefinition loaded
        def loader = new InlineLoader(source: '<QuestDefinition id = "tags" tags = ' + tagSource +
                ' children = [<ChildRef definitionKey = "water" additionalTags = ' + tagSource + ' />] />')

        when:
        loader.loadFromClasspath { resource, definition -> loaded = definition }
        def reopened = X_Model.deserialize(QuestDefinition, X_Model.serialize(QuestDefinition, loaded))

        then:
        loaded.tags as List == expected
        loaded.children().iterator().next().additionalTags as List == expected
        reopened.tags as List == expected
        reopened.children().iterator().next().additionalTags as List == expected

        where:
        tagSource                           | expected
        '["food, bag", "water", "水, 茶"]' | ['food, bag', 'water', '水, 茶']
        '[]'                                | []
        '[""]'                              | ['']
        '"food, bag"'                       | ['food', 'bag']
    }

    @Unroll
    def "explicit #policy policy and reference metadata load without creating progress"() {
        given:
        QuestDefinition loaded
        def loader = new InlineLoader(source: """
<QuestDefinition id = "packing" completionPolicy = "$policy" minimumRequired = 2
    children = [<ChildRef definitionKey = "water" role = "SEQUENTIAL"
        startPolicy = "AFTER_N_SIBLINGS_COMPLETE" startAfterNSiblingsComplete = 1
        quantity = 3 inheritTags = false additionalTags = ["travel"] />] />
""")

        when:
        loader.loadFromClasspath { resource, definition -> loaded = definition }
        def ref = loaded.children().iterator().next()

        then:
        loaded.completionPolicy == CompletionPolicy.valueOf(policy)
        loaded.minimumRequired == 2
        ref.role == ChildRole.SEQUENTIAL
        ref.startPolicy == ChildStartPolicy.AFTER_N_SIBLINGS_COMPLETE
        ref.startAfterNSiblingsComplete == 1
        ref.quantity == 3
        ref.inheritTags == false
        ref.definitionKey == QuestDefinition.newKey('water')

        where:
        policy << ['ALL_OF', 'ANY_OF', 'WEIGHTED']
    }

    @Unroll
    def "invalid composition '#attributes' fails visibly with source context"() {
        given:
        def loader = new InlineLoader(source: '<QuestDefinition id = "bad" ' + attributes + ' />')

        when:
        loader.loadFromClasspath { resource, definition -> }

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('inline-checklist.xapi')

        where:
        attributes << [
                'completionPolicy = "SOMETIMES"',
                'minimumRequired = -1',
                'minimumRequired = "several"',
                'children = "water"',
                'children = [<QuestDefinition id = "water" />]',
                'children = [<ChildRef />]',
                'children = [<ChildRef definitionKey = "" />]',
                'children = [<ChildRef definitionKey = "water" role = "SIDEWAYS" />]',
                'children = [<ChildRef definitionKey = "water" startPolicy = "DONE" />]',
                'children = [<ChildRef definitionKey = "water" quantity = 0 />]',
                'children = [<ChildRef definitionKey = "water" inheritTags = "maybe" />]',
                'children = [<ChildRef definitionKey = "water" done = true />]'
        ]
    }
}
