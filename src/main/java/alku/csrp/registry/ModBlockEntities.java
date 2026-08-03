package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.block.entity.ParasiteLootBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Csrp.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParasiteLootBlockEntity>> PARASITE_LOOT =
            BLOCK_ENTITIES.register("parasite_loot", () -> new BlockEntityType<>(
                    ParasiteLootBlockEntity::new,
                    Set.of(ModBlocks.PARASITE_LOOT_COMMON.get(), ModBlocks.PARASITE_LOOT_UNCOMMON.get(),
                            ModBlocks.PARASITE_LOOT_RARE.get()), null));

    private ModBlockEntities() {
    }
}
