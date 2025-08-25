package net.wti.ui.inventory.api;

/// ItemType:
///
/// A specific implementation of `IsItemType`, suitable for use in end-user models
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 03:29
public enum ItemType implements IsItemType {
    COPPER_COIN(NO_LIMIT),
    SILVER_COIN(NO_LIMIT),
    GOLD_COIN(NO_LIMIT),
//    GOLD_COIN(
//            ()->SpriteSheetCoins.image, new Rect(352, 800, 32, 32), 50, SHOP_PURCHASE
//    )
//    ,
//    SILVER_COIN(
//            ()->SpriteSheetCoins.image, new Rect(384, 800, 32, 32), 50, SHOP_PURCHASE
//    )
//    ,
//    COPPER_COIN(
//            ()->SpriteSheetCoins.image, new Rect(416, 800, 32, 32), 50, SHOP_PURCHASE
//    )
//    ,
//    EMERALD(
//            ()->SpriteSheetGems.image, new Rect(316, 800, 32, 32), 50, SPECIAL_PURCHASE
//    )
//    ,
//    DIAMOND(
//            ()->SpriteSheetGems.image, new Rect(216, 800, 32, 32), 50, SPECIAL_PURCHASE
//    )
// ...
    ;

    private final int stackLimit;

    ItemType(final int stackLimit) {
        this.stackLimit = stackLimit;
    }

//    @SuppressWarnings("UnnecessaryModifier")
//    private ItemType (Out1<LoadableImage> img, Rect rect, int stackLimit, ItemPurpose ... purposes) {
//        this.stackLimit = stackLimit;
//        this.purposes = EnumSet.noneOf(ItemPurpose.class);
//        for (ItemPurpose purpose : purposes) {
//            this.purposes.add(purpose);
//        }
//        this.inventoryImg = new ResettableLazy<>(()->
//                rect.getImage(img.out1().get())
//        );
//    }
//
//    public void maybeResetImage () {
//        if (inventoryImg.isResolved()) {
//            final Region img = inventoryImg.out1();
//            if (img.isLoaded()) {
//                final Tile tile = img.tile();
//                if (tile.isLoaded()) {
//                    final Texture tex = tile.texture();
//                    if (tex.disposed()) {
//                        // the underlying bits for the image were GC'd.
//                        // just tell the lazy to rebuild the image the next time it is accessed
//                        inventoryImg.reset();
//                    }
//                }
//            }
//        }
//    }

//    @Override
//    public Region getInventoryImage () {
//        // sometimes our images get GC'd... but we have to be on gfx thread to check / reset our lazy image provider
//        // so, we'll just make the actual consumers of the image pay to recheck the lazy-state in the thread where the
//        // actual bits of image data are rendered.
//        maybeResetImage();
//        return inventoryImg.out1();
//    }

    @Override
    public int getStackLimit () {
        return stackLimit;
    }

    @Override
    public Img getImage() {
        return null;
    }

//
//    @Override
//    public EnumSet<ItemPurpose> getPurposes () {
//        return purposes;
//    }

}
