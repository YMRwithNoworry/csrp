package alku.csrp.item;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

/**
 * Port of SRParasites 1.10.9's {@code item/ItemMobSpawner}.
 *
 * <p>The original spawn item spawned its mob at the clicked block/ray-traced position and consumed
 * itself outside creative mode, exactly like a spawn egg; only its artwork differed, and the
 * original Tabula-era models are {@code parent: item/generated}, so the texture paints the item
 * while this class supplies the spawn behaviour. The tint is forced to white so the painted
 * artwork is shown as-is, matching {@link TexturedSpawnEggItem}.</p>
 */
public final class ItemMobSpawner extends ForgeSpawnEggItem {
    public ItemMobSpawner(Supplier<? extends EntityType<? extends Mob>> type, Item.Properties properties) {
        super(type, 0xFFFFFF, 0xFFFFFF, properties);
    }

    @Override
    public int getColor(int tintIndex) {
        return 0xFFFFFF;
    }
}
