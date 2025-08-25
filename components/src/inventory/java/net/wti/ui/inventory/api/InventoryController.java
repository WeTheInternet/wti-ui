package net.wti.ui.inventory.api;

import net.wti.ui.inventory.event.InventoryChangeEvent;
import net.wti.ui.inventory.model.Inventory;
import net.wti.ui.inventory.model.InventorySlot;
import react.Signal;

import java.util.Map;

/// /// InventoryController:
///
/// Controller for the Inventory model, handling business logic such as stacking,
/// addition, removal of items, and exposing validation for the UI.
/// Integrates with model change listeners to emit InventoryChangeEvent.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 02:29
public class InventoryController {

    private final Inventory inventory;

    private final Signal<InventoryChangeEvent> changes = new Signal<>();

    /// Create an immutable controller for the given Inventory model instance.
    public InventoryController(Inventory inventory) {
        this.inventory = inventory;
        // Whenever any property on any slot changes, rebuild a per-slot event:
        inventory.slots().onGlobalChange((slotProp, oldVal, newVal) -> {
            System.out.println("Change detected: " + slotProp + " : " + oldVal + " -> " + newVal);
            // slotProp is like "slots[3].amount" or "slots[5].item"
            // extract index:
//            Matcher m = Pattern.compile("slots\\[(\\d+)\\]\\.(item|amount)").matcher(slotProp);
//            if (m.matches()) {
//                int idx = Integer.parseInt(m.group(1));
//                InventorySlot slot = inventory.slots().get(idx);
//                IsItemType type = slot.getItem()!=null ? (IsItemType)slot.getItem() : IsItemType.UNKNOWN;
//                int amt = Math.round(slot.getAmount());
//                InventoryChangeEvent.ChangeType typeEnum =
//                        m.group(2).equals("amount") ? InventoryChangeEvent.ChangeType.UPDATE
//                                : InventoryChangeEvent.ChangeType.UPDATE; // treat item‐changes as UPDATE too
//                changes.emit(new InventoryChangeEvent(typeEnum, idx, type, amt));
//            }
        });
    }

    /// @return the bound Inventory model.
    public Inventory getInventory() {
        return inventory;
    }

    /// @return true if the specified item and amount can be added to the inventory.
    public boolean canAdd(IsItemType item, int amount) {
        return inventory.canAdd(item, amount);
    }

    /// Attempt to add the given stacks; returns true on success.
    public boolean addItems(InventorySlot item) {
        // TODO: implement stacking logic via inventory model.
        return false;
    }

    /// Attempt to remove specified items; returns true on success.
    public boolean removeItems(Map<IsItemType, Integer> ingredients) {
        // TODO: convert to float map and call inventory.removeItems(...)
        return false;
    }

    /// Stream of InventoryChangeEvents emitted when the model changes.
    public Signal<InventoryChangeEvent> inventoryChange() {
        return changes;
    }

    /// Expand the bound view (UI operation placeholder).
    public void expand() {
        // TODO: invoke view.expand()
    }

    /// Collapse the bound view (UI operation placeholder).
    public void collapse() {
        // TODO: invoke view.collapse()
    }

    /// Trigger a UI rerender (placeholder).
    public void rerender() {
        // TODO: invoke view.refresh()
    }
}
