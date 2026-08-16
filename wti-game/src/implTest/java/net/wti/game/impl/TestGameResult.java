package net.wti.game.impl;

import net.wti.game.api.GameCommandResult;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;

@IsModel(
        modelType = TestGameResult.MODEL_TYPE,
        persistence = @Persistent(strategy = PersistenceStrategy.Local)
)
public interface TestGameResult extends GameCommandResult {

    String MODEL_TYPE = "testGameResult";

    String getEventType();
    TestGameResult setEventType(String eventType);

    int getRemainingCount();
    TestGameResult setRemainingCount(int remainingCount);
}
