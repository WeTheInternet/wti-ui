package net.wti.ui.inventory.model;

import net.wti.ui.api.GfxConstants;
import net.wti.ui.inventory.api.IsItemType;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.collect.api.ObjectTo;
import xapi.fu.Out2;
import xapi.fu.data.ListLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterator;
import xapi.fu.java.X_Jdk;
import xapi.model.api.Model;
import xapi.model.api.ModelList;

import java.util.HashMap;
import java.util.Map;

/**
 * Inventory:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 05/12/2022 @ 8:07 p.m.
 */
@IsModel(
        modelType = Inventory.INVENTORY
        ,persistence = @Persistent(strategy= PersistenceStrategy.Remote)
)
public interface Inventory extends Model {
    String INVENTORY = "inv";
    int MAX_SIZE = 64768;

    int getLimit();
    void setLimit(int limit);

    ModelList<InventorySlot> getSlots();
    void setSlots(ModelList<InventorySlot> slots);

    default ModelList<InventorySlot> slots() {
        return getOrCreateModelList(InventorySlot.class, this::getSlots, this::setSlots);
    }
    default MappedIterable<InventorySlot> slotsNonEmpty() {
        return slots().filter(s->s.getItem() != null);
    }

    default boolean containsEnough(ObjectTo<IsItemType, Float> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return true;
        }
        final Map<IsItemType, Float> needs = new HashMap<>();
        for (Out2<IsItemType, Float> e : ingredients.forEachItem()) {
            needs.put(e.out1(), e.out2());
        }
        final Map<IsItemType, ListLike<InventorySlot>> slots = new HashMap<>();
        for (InventorySlot slot : slotsNonEmpty()) {
            final IsItemType itemType = slot.getItem();
            assert itemType != null;
            if (ingredients.containsKey(itemType)) {
                slots.computeIfAbsent(itemType, i-> X_Jdk.list()).add(slot);
                needs.merge(itemType, -slot.getAmount(), Float::sum);
            }
        }
        for (Float value : needs.values()) {
            if (value > 0) {
                return false;
            }
        }
        return true;
    }


    default boolean removeItems(final ObjectTo<IsItemType, Float> ingredients) {
        if (ingredients == null) {
            return true;
        }
        final Map<IsItemType, Float> allAvail = new HashMap<>();
        final Map<IsItemType, Float> newAmounts = new HashMap<>();
        final Map<IsItemType, ListLike<InventorySlot>> slots = new HashMap<>();
        for (InventorySlot slot : slotsNonEmpty()) {
            final IsItemType itemType = slot.getItem();
            if (ingredients.containsKey(itemType)) {
                slots.computeIfAbsent(itemType, i-> X_Jdk.list()).add(slot);
                allAvail.merge(itemType, slot.getAmount(), Float::sum);
            }
        }
        for (Out2<IsItemType, Float> e : ingredients.forEachItem()) {
            final IsItemType item = e.out1();
            final Float amount = e.out2();
            final Float current = allAvail.get(item);
            if (current == null) {
                return false;
            }
            final float newAmt = current - amount;
            if (newAmt >= 0) {
                newAmounts.put(item, newAmt);
            } else {
                return false;
            }
        }
        itemLoop:
        for (Map.Entry<IsItemType, Float> amt : newAmounts.entrySet()) {
            final float needToBe = amt.getValue();
            final IsItemType type = amt.getKey();

            float curAmt = allAvail.get(type);
            final ListLike<InventorySlot> allSlots = slots.get(type);
            while (curAmt > needToBe) {
                InventorySlot best = null;
                final SizedIterator<InventorySlot> itr = allSlots.iterator();
                while (itr.hasNext()) {
                    final InventorySlot next = itr.next();
                    if (best == null) {
                        best = next;
                    } else {
                        if (next.getAmount() < best.getAmount()) {
                            best = next;
                        }
                    }
                }
                if (best == null) {
                    throw new IllegalStateException("Failed to remove " + ingredients);
                }
                float diff = curAmt - needToBe;
                if (diff > 0) {
                    // this slot can be reduced enough to pay.
                    float removable = Math.min(best.getAmount(), diff);
                    if (removable < diff) {
                        // we need more than this slot has to offer. Remove it and update our amounts.
                        curAmt -= best.getAmount();
                        best.setAmount(0);
                        best.setItem(null);
                        allSlots.removeFirst(best, true);
                    } else {
                        best.setAmount(best.getAmount() - diff);
                        if (best.getAmount() < GfxConstants.TINY) {
                            best.setAmount(0);
                            best.setItem(null);
                            allSlots.removeFirst(best, true);
                        }
                        continue itemLoop;
                    }
                } else {
                    // this slot is not enough to pay the cost. zero it / remove it from model.
                    curAmt -= best.getAmount();
                    best.setAmount(0);
                    best.setItem(null);
                    allSlots.removeFirst(best, true);
                }
            }
        }
        return true;
    }

    default float count(IsItemType type) {
        float cnt = 0;
        // TODO: indexed inventory counts, which we can invalidate w/ property write listeners on our slots.
        for (InventorySlot slot : getSlots()) {
            if (slot.getItem() == type) {
                cnt += slot.getAmount();
            }
        }
        return cnt;
    }

    default boolean canAdd(IsItemType item, int amt) {
        final ModelList<InventorySlot> slots = slots();
        final int limit = getLimit();
        if (slots.size() < limit) {
            // TODO: support large items that eat more than 1 slot
            return true;
        }
        // next, check for empty slots, or slots of desired type w/ enough stack space left
        for (InventorySlot slot : slots) {
            final IsItemType slotType = slot.getItem();
            if (slotType == null) {
                // an empty slot, good enough
                return true;
            }
            if (slotType == item) {
                float diff = slotType.getStackLimit() - slot.getAmount();
                if (diff >= 1) {
                    amt -= diff;
                    if (amt <= 0) {
                        return true;
                    }
                }
            }
        }
        // no luck. not enough space
        return false;
    }

    default void compress() {
        // reduce slot usage as much as possible...
    }

}
