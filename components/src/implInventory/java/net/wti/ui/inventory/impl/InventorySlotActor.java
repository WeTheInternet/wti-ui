package net.wti.ui.inventory.impl;


import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.wti.ui.inventory.api.IsItemType;
import net.wti.ui.inventory.event.SlotClickEvent;
import net.wti.ui.view.api.IsView;
import react.Signal;

/// InventorySlotActor:
///
/// Actor representing a single inventory slot in Scene2D. Implements a clickable view cell.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 03:45
public class InventorySlotActor extends Table implements IsView {

    private final int slotIndex;
    private final Image icon;
    private final Label amountLabel;
    private final Signal<SlotClickEvent> clicks = new Signal<>();

    /// Construct a slot actor for the given index, item type, and initial amount.
    public InventorySlotActor(int slotIndex, IsItemType type, int amount, Skin skin) {
        super(skin);
        this.slotIndex = slotIndex;
        this.icon = new Image(type.getImage().getRegion());
        this.amountLabel = new Label(Integer.toString(amount), skin);

        /// Layout: icon above amount
        add(icon).center().row();
        add(amountLabel).center();

        /// Click listener emits a SlotClickEvent
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                clicks.emit(new SlotClickEvent(slotIndex, type, amount));
            }
        });
    }

    @Override
    public void refresh() {
        // Could update icon/label based on new model state
        amountLabel.setText(amountLabel.getText().toString());
    }

    @Override
    public void dispose() {
        // nothing to dispose for Table children, but detach listeners if needed
        clearListeners();
    }

    /// @return a stream of click events from this slot.
    public Signal<SlotClickEvent> clickEvents() {
        return clicks;
    }
}
