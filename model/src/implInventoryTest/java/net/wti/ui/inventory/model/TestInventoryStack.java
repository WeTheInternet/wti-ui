package net.wti.ui.inventory.model;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;

@IsModel(
        modelType = TestInventoryStack.MODEL_TYPE,
        persistence = @Persistent(strategy = PersistenceStrategy.Local)
)
public interface TestInventoryStack extends BasicStack {

    String MODEL_TYPE = "testInventoryStack";

    String getItemId();

    TestInventoryStack setItemId(String itemId);

    @Override
    TestInventoryStack setCount(int count);
}
