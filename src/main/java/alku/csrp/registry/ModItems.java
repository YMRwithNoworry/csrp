package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Csrp.MODID);

    public static final DeferredItem<SpawnEggItem> BUGLIN_SPAWN_EGG = ITEMS.registerItem(
            "buglin_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.BUGLIN.get(), 0x8B1E1E, 0xE1B85B, properties),
            new Item.Properties());

    public static final DeferredItem<SpawnEggItem> RUPTER_SPAWN_EGG = ITEMS.registerItem(
            "rupter_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.RUPTER.get(), 0x6E1717, 0xD8B45B, properties),
            new Item.Properties());

    public static final DeferredItem<Item> RUPTER_VISCERA = ITEMS.registerSimpleItem(
            "rupter_viscera", new Item.Properties());

    public static final DeferredItem<BlockItem> TUNNEL = ITEMS.registerSimpleBlockItem(
            "tunnel", ModBlocks.TUNNEL);

    private ModItems() {
    }
}
