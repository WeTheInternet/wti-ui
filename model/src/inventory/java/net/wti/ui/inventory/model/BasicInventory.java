package net.wti.ui.inventory.model;

import xapi.model.api.Model;
import xapi.model.api.ModelList;

/// Minimal model for an inventory composed of stable, counted stacks.
///
/// Capacity is the maximum number of occupied stack entries. Zero means
/// unlimited, and the setter normalizes negative values to zero. Capacity
/// derivation and enforcement, equipment, hotbars, ticking, and game sessions
/// remain consumer policy.
///
/// XApi's runtime manifest inspection erases an unresolved `T` to its bound,
/// and it inspects inherited abstract methods before their concrete
/// redeclarations. These generic accessors are default property bridges so XApi
/// skips them while building a manifest. Every concrete `@IsModel` subtype must
/// redeclare `getStacks()` and `void setStacks(ModelList)` with its concrete
/// stack type. The subtype should also provide a helper which calls
/// `getOrCreateModelList` with that concrete class literal.
///
/// @param <T> the consumer's concrete stack model
public interface BasicInventory<T extends BasicStack> extends Model {

    int UNLIMITED_CAPACITY = 0;

    /// Returns the maximum number of occupied stack entries, or zero when unlimited.
    int getCapacity();

    /// Sets the maximum number of occupied stack entries.
    ///
    /// Negative values are normalized to the canonical unlimited value, zero.
    default void setCapacity(int capacity) {
        setProperty("capacity", Math.max(UNLIMITED_CAPACITY, capacity));
    }

    /// Returns true when no stack-slot limit is declared.
    default boolean isUnlimited() {
        return getCapacity() <= UNLIMITED_CAPACITY;
    }

    /// Returns true when a positive stack-slot limit is declared.
    default boolean isBounded() {
        return getCapacity() > UNLIMITED_CAPACITY;
    }

    default ModelList<T> getStacks() {
        return getProperty("stacks");
    }

    default void setStacks(ModelList<T> stacks) {
        setProperty("stacks", stacks);
    }
}
