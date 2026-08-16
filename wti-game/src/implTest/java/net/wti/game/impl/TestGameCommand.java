package net.wti.game.impl;

import net.wti.game.api.GameCommand;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;

@IsModel(
        modelType = TestGameCommand.MODEL_TYPE,
        persistence = @Persistent(strategy = PersistenceStrategy.Local)
)
public interface TestGameCommand extends GameCommand {

    String MODEL_TYPE = "testGameCommand";

    String getInventoryInstanceId();
    TestGameCommand setInventoryInstanceId(String inventoryInstanceId);
}
