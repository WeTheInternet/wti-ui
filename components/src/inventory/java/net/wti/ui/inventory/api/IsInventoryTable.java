package net.wti.ui.inventory.api;

import net.wti.ui.inventory.event.SlotClickEvent;
import net.wti.ui.inventory.model.Inventory;
import net.wti.ui.view.api.IsCollapsibleView;
import react.Signal;

/// IsInventoryTable:
///
/// Abstraction for an inventory UI component built on libGDX Scene2D.
/// Renders a grid of slots based on an Inventory model, handles user clicks,
/// and supports expand/collapse and refresh operations.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 02:32
public interface IsInventoryTable extends IsCollapsibleView {

    /// Bind the Inventory model to this table, register listeners, and populate slots.
    void setInventory(Inventory inventory);

    /// Emits SlotClickEvent whenever a user clicks on a slot.
    Signal<SlotClickEvent> slotClickEvents();
}