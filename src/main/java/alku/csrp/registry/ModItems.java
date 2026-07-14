package alku.csrp.registry;

import alku.csrp.Csrp;
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

    private ModItems() {
    }
}
