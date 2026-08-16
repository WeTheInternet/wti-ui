package net.wti.model.user.spi;

///
/// WtiUserLoader:
///
import net.wti.model.user.core.UserGroupStore;
import xapi.fu.In2;
import xapi.model.api.ModelKey;
import net.wti.model.api.WtiUser;

/// WtiUserLoader
///
/// Loads users into an external store and/or callback stream.
public interface WtiUserLoader {

    void loadInto(UserGroupStore store);

    void loadFromClasspath(In2<String, WtiUser> callback);

    ModelKey keyForUsername(String username);
}
