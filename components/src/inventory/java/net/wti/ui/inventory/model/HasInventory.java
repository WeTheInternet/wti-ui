package net.wti.ui.inventory.model;

import xapi.annotation.model.KeyOnly;
import xapi.model.X_Model;
import xapi.model.api.HasModelKey;

/// HasInventory:
///
/// An abstraction for models which contain an `Inventory`
///
/// Created by James X. Nelson (James@WeTheInter.net) on 05/12/2022 @ 8:08 p.m.
public interface HasInventory extends HasModelKey {

    @KeyOnly(autoSave = true)
    Inventory getInventory();
    void setInventory(Inventory inventory);

    default Inventory inventory() {
        Inventory inv = getInventory();
        if (inv == null) {
            inv = X_Model.create(Inventory.class);
            inv.setKey(getKey().getChild(Inventory.INVENTORY, "i"));
            setInventory(inv);
        }
        return inv;
    }
}
