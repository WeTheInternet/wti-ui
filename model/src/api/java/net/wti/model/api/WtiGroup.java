package net.wti.model.api;

import xapi.annotation.model.*;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelKey;
import xapi.model.user.ModelUser;

///
/// WtiGroup:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 19:14
@IsModel(
        modelType = WtiGroup.MODEL_WTI_GROUP
        ,persistence = @Persistent(strategy= PersistenceStrategy.Remote)
        ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
)
)
public interface WtiGroup extends ModelUser {
    String MODEL_WTI_GROUP = "wg";

    KeyBuilder KEY_BUILDER_GROUP =
            KeyBuilder.build(MODEL_WTI_GROUP).withType(ModelKey.KEY_TYPE_STRING);

    /// Build a WtiGroup key based on username.
    static ModelKey newKey(String username) {
        return KEY_BUILDER_GROUP.buildKey(username);
    }
}
