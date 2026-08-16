package net.wti.model.api;

import xapi.annotation.model.*;
import xapi.fu.data.SetLike;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelKey;
import xapi.model.user.ModelUser;

///
/// WtiUser:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 19:13
@IsModel(
    modelType = WtiUser.MODEL_WTI_USER
    ,persistence = @Persistent(strategy= PersistenceStrategy.Remote)
    ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
    )
)
public interface WtiUser extends ModelUser {
    String MODEL_WTI_USER = "wu";

    KeyBuilder KEY_BUILDER_USER =
            KeyBuilder.build(MODEL_WTI_USER).withType(ModelKey.KEY_TYPE_STRING);

    /// Build a WtiUser key based on username.
    static ModelKey newKey(String username) {
        return KEY_BUILDER_USER.buildKey(username);
    }


    SetLike<String> getGroups();
    default SetLike<String> groups() {
        return getOrCreateSet(this::getGroups, this::setGroups);
    }
    @SuppressWarnings("UnusedReturnValue")
    WtiUser setGroups(SetLike<String> groups);
}
