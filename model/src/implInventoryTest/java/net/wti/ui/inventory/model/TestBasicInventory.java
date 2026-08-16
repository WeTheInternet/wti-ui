package net.wti.ui.inventory.model;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.ModelList;

@IsModel(
        modelType = TestBasicInventory.MODEL_TYPE,
        persistence = @Persistent(strategy = PersistenceStrategy.Local)
)
public interface TestBasicInventory extends BasicInventory<TestInventoryStack> {

    String MODEL_TYPE = "testBasicInventory";

    @Override
    ModelList<TestInventoryStack> getStacks();

    @Override
    void setStacks(ModelList<TestInventoryStack> stacks);

    default ModelList<TestInventoryStack> stacks() {
        return getOrCreateModelList(
                TestInventoryStack.class,
                this::getStacks,
                this::setStacks
        );
    }
}
