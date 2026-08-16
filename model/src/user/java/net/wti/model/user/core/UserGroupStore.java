package net.wti.model.user.core;

import net.wti.model.api.WtiGroup;
import net.wti.model.api.WtiUser;
import xapi.model.api.ModelKey;

/// UserGroupStore
///
/// Preloaded account/group membership data used for namespace resolution
/// and fast ACL/membership decisions.
public interface UserGroupStore {

    void putUser(WtiUser user);

    void putGroup(WtiGroup group);

    WtiUser findUser(ModelKey userKey);

    WtiGroup findGroup(ModelKey groupKey);

    /// Returns namespace keys for all groups the user belongs to.
    Iterable<ModelKey> findGroupNamespacesForUser(ModelKey userKey);

    /// Optional reset hook for logout/login flows.
    void clear();
}
