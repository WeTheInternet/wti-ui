package net.wti.ui.inventory.event;

import net.wti.ui.inventory.api.IsItemType;

/// SlotClickEvent:
///
/// Event fired when a user clicks an inventory slot.
/// Carries the slot index, item type, and current amount.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 02:36
public class SlotClickEvent {

    private final int slotIndex;
    private final IsItemType itemType;
    private final int amount;

    /// Construct a slot click event.
    public SlotClickEvent(int slotIndex, IsItemType itemType, int amount) {
        this.slotIndex = slotIndex;
        this.itemType = itemType;
        this.amount = amount;
    }

    public int getSlotIndex() { return slotIndex; }
    public IsItemType getItemType() { return itemType; }
    public int getAmount() { return amount; }
}
