package net.wti.model.user.impl

import net.wti.model.api.WtiGroup
import net.wti.model.api.WtiUser
import spock.lang.Specification
import xapi.jre.model.ModelServiceJre
import xapi.model.X_Model

///
/// WtiUserGroupLoaderImplSpec:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 23:40
class WtiUserGroupLoaderImplSpec extends Specification {

    void setupSpec() {
        final ModelServiceJre srv = X_Model.getService() as ModelServiceJre
        srv.register(WtiGroup)
        srv.register(WtiUser)
        srv.getOrMakeModelManifest(WtiGroup)
        srv.getOrMakeModelManifest(WtiUser)
    }

    def "group loader reads wg fixtures"() {
        given:
        def store = new InMemoryUserGroupStore()
        def loader = new WtiGroupLoaderImpl()

        when:
        loader.loadInto(store)

        then:
        store.findGroup(WtiGroup.newKey("test-admin")) != null
        store.findGroup(WtiGroup.newKey("test-kid")) != null
    }

    def "user loader reads wu fixtures and user->group membership"() {
        given:
        def store = new InMemoryUserGroupStore()
        new WtiGroupLoaderImpl().loadInto(store)
        def userLoader = new WtiUserLoaderImpl()

        when:
        userLoader.loadInto(store)
        def dad = store.findUser(WtiUser.newKey("test-dad"))

        then:
        dad != null
        dad.groups().contains("test-admin")
    }

    def "group namespaces for user are returned as ModelKeys keyed by group id"() {
        given:
        def store = new InMemoryUserGroupStore()
        new WtiGroupLoaderImpl().loadInto(store)
        new WtiUserLoaderImpl().loadInto(store)

        when:
        def keys = store.findGroupNamespacesForUser(WtiUser.newKey("test-kid-1"))
        def ids = [] as List<String>
        for (def k : keys) {
            ids.add(String.valueOf(k.id))
        }

        then:
        ids.contains("test-kid")
    }
}