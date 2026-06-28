package net.wti.model.user.impl;

import net.wti.model.api.WtiGroup;
import net.wti.model.api.WtiUser;
import net.wti.model.user.core.UserGroupStore;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.model.api.ModelKey;

///
/// InMemoryUserGroupStore:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 09:41
public class InMemoryUserGroupStore implements UserGroupStore {

    private final MapLike<String, WtiUser> users = X_Jdk.mapOrderedInsertion();
    private final MapLike<String, WtiGroup> groups = X_Jdk.mapOrderedInsertion();

    @Override
    public void putUser(final WtiUser user) {
        if (user != null && user.getKey() != null) {
            users.put(user.getKey().getId(), user);
        }
    }

    @Override
    public void putGroup(final WtiGroup group) {
        if (group != null && group.getKey() != null) {
            groups.put(group.getKey().getId(), group);
        }
    }

    @Override
    public WtiUser findUser(final ModelKey userKey) {
        return userKey == null ? null : users.get(userKey.getId());
    }

    @Override
    public WtiGroup findGroup(final ModelKey groupKey) {
        return groupKey == null ? null : groups.get(groupKey.getId());
    }

    @Override
    public Iterable<ModelKey> findGroupNamespacesForUser(final ModelKey userKey) {
        final WtiUser user = findUser(userKey);
        final ListLike<ModelKey> out = X_Jdk.listArray();
        if (user == null || user.getGroups() == null) {
            return out;
        }
        for (String group : user.getGroups()) {
            if (group != null && !group.trim().isEmpty()) {
                out.add(WtiGroup.newKey(group));
            }
        }
        return out;
    }

    @Override
    public void clear() {
        users.clear();
        groups.clear();
    }
}