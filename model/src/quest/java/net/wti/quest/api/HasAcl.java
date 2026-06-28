package net.wti.quest.api;

import xapi.fu.data.SetLike;
import xapi.model.api.Model;

///
/// HasAcl:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/06/2026 @ 20:37
public interface HasAcl extends Model {

    SetLike<String> getAllowedUsers();
    default SetLike<String> allowedUsers() {
        return getOrCreateSet(this::getAllowedUsers, this::setAllowedUsers);
    }
    HasAcl setAllowedUsers(SetLike<String> allowedUsers);

    SetLike<String> getAllowedGroups();
    default SetLike<String> allowedGroup() {
        return getOrCreateSet(this::getAllowedGroups, this::setAllowedGroups);
    }
    HasAcl setAllowedGroups(SetLike<String> allowedGroups);

    default boolean meetsAcl(final String user, final String group) {
        final SetLike<String> allowedUsers = getAllowedUsers();
        final SetLike<String> allowedGroups = getAllowedGroups();
        if (allowedUsers != null) {
            if (!allowedUsers.contains(user)) {
                return false;
            }
        }
        if (allowedGroups != null) {
            return allowedGroups.contains(group);
        }
        return true;
    }
    default void copyAclsFrom(final HasAcl source) {
        final SetLike<String> users = source.getAllowedUsers();
        if (users != null && !users.isEmpty()) {
            setAllowedUsers(users);
        }
        final SetLike<String> groups = source.getAllowedGroups();
        if (groups != null && !groups.isEmpty()) {
            setAllowedGroups(groups);
        }
    }
}
