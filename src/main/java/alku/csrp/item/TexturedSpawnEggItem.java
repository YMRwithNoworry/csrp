package alku.csrp.item;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** Spawn egg that preserves the colors already painted into its custom texture. */
public final class TexturedSpawnEggItem extends SpawnEggItem {
    public TexturedSpawnEggItem(EntityType<? extends Mob> type, int primaryColor, int secondaryColor,
            Item.Properties properties) {
        super(type, primaryColor, secondaryColor, properties);
    }

    @Override
    public int getColor(int tintIndex) {
        return 0xFFFFFF;
    }
}
