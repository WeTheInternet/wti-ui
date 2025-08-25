package net.wti.ui.inventory.impl

import net.wti.ui.inventory.api.InventoryController
import net.wti.ui.inventory.api.IsItemType
import net.wti.ui.inventory.model.Inventory
import net.wti.ui.inventory.model.InventorySlot
import spock.lang.Specification
import xapi.model.X_Model

/// Spock specification for InventoryController.
class InventoryControllerSpec extends Specification {

    def "canAdd returns false when inventory is full"() {
        given:
        Inventory model = X_Model.create(Inventory.class)
        model.setLimit(1)
        InventorySlot slot = X_Model.create(InventorySlot.class)
        slot.setItem(Mock(IsItemType))
        slot.setAmount(1)
        model.slots().add(slot)
        def ctl = new InventoryController(model)

        expect:
        !ctl.canAdd(Mock(IsItemType), 1)
    }
}