package net.wti.ui.inventory.model;

import net.wti.ui.inventory.api.IsItemType;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.Model;

/**
 * InventorySlot:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 05/12/2022 @ 8:05 p.m.
 */
@IsModel(
        modelType = InventorySlot.INVENTORY_SLOT
        ,persistence = @Persistent(strategy= PersistenceStrategy.Inline)
)
public interface InventorySlot extends Model {
    String INVENTORY_SLOT = "slot";

    IsItemType getItem();
    void setItem(IsItemType itemType);

    float getAmount();
    void setAmount(float amount);

}
