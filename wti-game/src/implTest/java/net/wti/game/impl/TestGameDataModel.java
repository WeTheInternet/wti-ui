package net.wti.game.impl;

import net.wti.game.api.GameDataModel;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;

@IsModel(
        modelType = TestGameDataModel.MODEL_TYPE,
        persistence = @Persistent(strategy = PersistenceStrategy.Local)
)
public interface TestGameDataModel extends GameDataModel {

    String MODEL_TYPE = "testGameData";

    String getDisplayName();
    TestGameDataModel setDisplayName(String displayName);
}
