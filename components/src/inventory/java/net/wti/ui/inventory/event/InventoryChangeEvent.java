package net.wti.ui.inventory.event;

import net.wti.ui.inventory.api.IsItemType;

/// InventoryChangeEvent:
///
/// Event fired whenever the Inventory model changes (add/remove/update).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 02:24
public class InventoryChangeEvent {

    public enum ChangeType { ADD, REMOVE, UPDATE }

    private final ChangeType type;
    private final int slotIndex;
    private final IsItemType itemType;
    private final int amount;

    /// Create an inventory change event.
    public InventoryChangeEvent(ChangeType type, int slotIndex, IsItemType itemType, int amount) {
        this.type = type;
        this.slotIndex = slotIndex;
        this.itemType = itemType;
        this.amount = amount;
    }

    public ChangeType getType() { return type; }
    public int getSlotIndex() { return slotIndex; }
    public IsItemType getItemType() { return itemType; }
    public int getAmount() { return amount; }
}
