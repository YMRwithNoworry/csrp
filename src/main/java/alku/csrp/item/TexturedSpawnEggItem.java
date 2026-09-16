package alku.csrp.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;

/** Spawn egg that preserves the colors already painted into its custom texture. */
public final class TexturedSpawnEggItem extends SpawnEggItem {
    public TexturedSpawnEggItem(EntityType<? extends Mob> type, int primaryColor, int secondaryColor,
            Item.Properties properties) {
        super(properties.component(DataComponents.ENTITY_DATA, TypedEntityData.of(type, new CompoundTag())));
    }
}
