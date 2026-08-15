package alku.csrp.item;

import net.minecraft.world.item.Item;

/** Bottle filled by collecting a parasite fog block. */
public final class FogBottleItem extends Item {
    public FogBottleItem(Properties properties) {
        super(properties.stacksTo(16));
    }
}
