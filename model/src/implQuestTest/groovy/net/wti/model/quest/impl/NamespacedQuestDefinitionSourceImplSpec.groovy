package net.wti.model.quest.impl

import net.wti.model.api.WtiGroup
import net.wti.model.api.WtiUser
import net.wti.model.quest.api.QuestDefinitionStore
import net.wti.model.user.impl.InMemoryUserGroupStore
import net.wti.quest.api.QuestDefinition
import net.wti.quest.model.impl.InMemoryQuestDefinitionStore
import net.wti.quest.model.impl.NamespacedQuestDefinitionSourceImpl
import spock.lang.Specification
import xapi.model.X_Model
import xapi.model.api.ModelKey

///
/// NamespacedQuestDefinitionSourceImplSpec:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 23:39
class NamespacedQuestDefinitionSourceImplSpec extends Specification {

    def "user namespace overrides group and root by id"() {
        given:
        final QuestDefinitionStore defs = new InMemoryQuestDefinitionStore()
        final InMemoryUserGroupStore users = new InMemoryUserGroupStore()

        final ModelKey rootNs = WtiGroup.newKey("root")
        final ModelKey groupNs = WtiGroup.newKey("kid")
        final ModelKey userNs = WtiUser.newKey("alyx")

        def user = X_Model.create(WtiUser)
        user.setKey(userNs)
        user.groups().add("kid")
        users.putUser(user)

        def group = X_Model.create(WtiGroup)
        group.setKey(groupNs)
        users.putGroup(group)

        ((InMemoryQuestDefinitionStore) defs).put(rootNs, qdef("q123", "root value"))
        ((InMemoryQuestDefinitionStore) defs).put(groupNs, qdef("q123", "group value"))
        ((InMemoryQuestDefinitionStore) defs).put(userNs, qdef("q123", "user value"))

        and:
        def source = new NamespacedQuestDefinitionSourceImpl(defs, users, rootNs)

        when:
        def seen = [:] as LinkedHashMap<String, String>
        source.streamDefinitionsForUser(userNs, { ModelKey ns, QuestDefinition qdef ->
            seen.put(String.valueOf(qdef.key.id), qdef.title)
        }, {})

        then:
        seen.size() == 1
        seen["q123"] == "user value"
    }

    def "group namespace overrides root when user has no override"() {
        given:
        final QuestDefinitionStore defs = new InMemoryQuestDefinitionStore()
        final InMemoryUserGroupStore users = new InMemoryUserGroupStore()

        final ModelKey rootNs = WtiGroup.newKey("root")
        final ModelKey groupNs = WtiGroup.newKey("kid")
        final ModelKey userNs = WtiUser.newKey("mia")

        def user = X_Model.create(WtiUser)
        user.setKey(userNs)
        user.groups().add("kid")
        users.putUser(user)

        def group = X_Model.create(WtiGroup)
        group.setKey(groupNs)
        users.putGroup(group)

        ((InMemoryQuestDefinitionStore) defs).put(rootNs, qdef("qA", "root A"))
        ((InMemoryQuestDefinitionStore) defs).put(groupNs, qdef("qA", "group A"))

        and:
        def source = new NamespacedQuestDefinitionSourceImpl(defs, users, rootNs)

        when:
        def seen = [:] as LinkedHashMap<String, String>
        source.streamDefinitionsForUser(userNs, { ModelKey ns, QuestDefinition qdef ->
            seen.put(String.valueOf(qdef.key.id), qdef.title)
        }, {})

        then:
        seen["qA"] == "group A"
    }

    private static QuestDefinition qdef(final String id, final String name) {
        def q = X_Model.create(QuestDefinition)
        q.setKey(QuestDefinition.newKey(id))
        q.setTitle(name)
        q.setActive(true)
        q.setAuto(true)
        return q
    }
}