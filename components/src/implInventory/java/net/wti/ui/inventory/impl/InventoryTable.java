package net.wti.ui.inventory.impl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.ui.inventory.api.InventoryController;
import net.wti.ui.inventory.api.IsInventoryTable;
import net.wti.ui.inventory.api.IsItemType;
import net.wti.ui.inventory.event.SlotClickEvent;
import net.wti.ui.inventory.model.Inventory;
import net.wti.ui.inventory.model.InventorySlot;
import react.Connection;
import react.Signal;
import react.Slot;
import xapi.model.api.ModelList;

/// InventoryTable:
///
/// Concrete Table implementing IsInventoryTable with Scene2D.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 03:50
public class InventoryTable extends Table implements IsInventoryTable {
    private final InventoryController controller;
    private final Skin skin;
    private final Signal<SlotClickEvent> slotClicks = new Signal<>();
    private boolean expanded;
    private Connection connection;

    /// Construct an InventoryTable bound to the given controller and UI skin.
    public InventoryTable(Skin skin, InventoryController controller) {
        super(skin);
        this.skin = skin;
        this.controller = controller;
    }

    @Override
    public void setInventory(Inventory inventory) {
        // Listen to any change on the inventory model and refresh UI
        inventory.onGlobalChange((prop, oldVal, newVal) -> refresh());
        buildGrid();
    }

    private void buildGrid() {
        clear();
        int limit = controller.getInventory().getLimit();
        int columns = Math.max(1, Gdx.graphics.getWidth() / 80);
        final ModelList<InventorySlot> slots = controller.getInventory().slots();
        int i = 0;
        for (InventorySlot slot : slots) {
            i++;

            // Use UNKNOWN when no item present
            IsItemType type = slot.getItem() != null
                    ? slot.getItem()
                    : IsItemType.UNKNOWN;
            int amt = Math.round(slot.getAmount());

            InventorySlotActor cell = new InventorySlotActor(i, type, amt, skin);
            connection = cell.clickEvents().connect(new Slot<SlotClickEvent>() {
                @Override
                public void onEmit(final SlotClickEvent evt) {
                    slotClicks.emit(evt);
                }
            });
            add(cell).pad(4);

            if ((i + 1) % columns == 0) row();
        }

        if (expanded) expand(); else collapse();
    }

    @Override public Signal<SlotClickEvent> slotClickEvents() { return slotClicks; }

    @Override public void refresh() {
        buildGrid();
    }

    @Override public void expand() {
        expanded = true;
        // e.g., change background or show details
        setBackground("inventory-expanded");
    }

    @Override public void collapse() {
        expanded = false;
        setBackground("inventory-compact");
    }

    @Override public boolean isExpanded() {
        return expanded;
    }

    @Override public void dispose() {
        clear();
        if (connection != null) {
            connection.close();
            connection = null;
        }
        slotClicks.clearConnections();
    }
}