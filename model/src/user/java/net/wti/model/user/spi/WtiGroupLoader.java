package net.wti.model.user.spi;

///
/// WtiGroupLoader:
///
///
import net.wti.model.api.WtiGroup;
import net.wti.model.user.core.UserGroupStore;
import xapi.fu.In2;
import xapi.model.api.ModelKey;

/// WtiGroupLoader
///
/// Loads groups into an external store and/or callback stream.
public interface WtiGroupLoader {

    void loadInto(UserGroupStore store);

    void loadFromClasspath(In2<String, WtiGroup> callback);

    ModelKey keyForGroup(String groupName);
}
