package net.wti.ui.inventory.api;

import xapi.model.api.IsEnumerable;

/// IsItemType:
///
/// Represents an item type in the inventory system without tying to a specific graphics library.
/// Concrete enums (e.g., ItemType) should implement this by wrapping a libGDX TextureRegion.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 02:33
public interface IsItemType extends IsEnumerable {

    IsItemType UNKNOWN = new IsItemType() {
        @Override
        public String name() {
            return "?";
        }

        @Override
        public Img getImage() {
            return null;
        }

        @Override
        public int getStackLimit() {
            return 0;
        }

        @Override
        public int ordinal() {
            return -1;
        }
    };

    /// A sentinel value to mean "no limit"
    int NO_LIMIT = Integer.MIN_VALUE;

    /// @return unique identifier (name) of the item type.
    String name();

    /// @return maximum stack size (>= 1).
    int getStackLimit();

    /// @return icon for this item type.
    Img getImage();
}
