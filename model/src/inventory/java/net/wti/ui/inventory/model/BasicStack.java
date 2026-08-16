package net.wti.ui.inventory.model;

import xapi.model.api.Model;

/// Minimal persistent shape shared by inventories which store counted stacks.
///
/// The inherited `ModelKey` is the stable identity of a stack. Concrete stack
/// models deliberately own item identity and any consumer-specific metadata;
/// this contract only states what makes the model a stack.
public interface BasicStack extends Model {

    /// Returns the number of items represented by this stack.
    int getCount();

    /// Sets the number of items represented by this stack.
    ///
    /// Validation such as positive counts and maximum stack sizes belongs to
    /// the service mutating an inventory, where the item type and policy are
    /// available.
    BasicStack setCount(int count);
}
