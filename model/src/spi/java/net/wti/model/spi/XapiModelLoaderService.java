package net.wti.model.spi;

import xapi.fu.In2;
import xapi.model.api.Model;

///
/// XapiModelLoaderService:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2026 @ 12:11
public interface XapiModelLoaderService <M extends Model> {

    void loadFromClasspath(In2<String, M> callback);

}
