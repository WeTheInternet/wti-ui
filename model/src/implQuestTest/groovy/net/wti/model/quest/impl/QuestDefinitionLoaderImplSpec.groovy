package net.wti.model.quest.impl

import net.wti.model.test.AbstractModelLoaderSpec
import net.wti.quest.api.QuestDefinition
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

    static class ExposedLoader extends QuestDefinitionLoaderImpl {
        Object manifestFor(final Class type) { findManifest(type) }
        String metaInfFor(final String suffix) { metaInfPath(suffix) }
    }

    void setupSpec() {
        final ModelServiceJre srv = X_Model.getService() as ModelServiceJre
        srv.register(QuestDefinition)
        srv.getOrMakeModelManifest(QuestDefinition)
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
        fixture << ["test-quests.xapi"]
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
}