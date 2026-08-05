package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.block.entity.DispatcherNidusBlockEntity;
import alku.csrp.block.entity.ParasiteLootBlockEntity;
import alku.csrp.block.entity.RelayTerminalBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RelayTerminalBlockEntity>> RELAY_TERMINAL =
            BLOCK_ENTITIES.register("relay_terminal", () -> new BlockEntityType<>(
                    RelayTerminalBlockEntity::new, Set.of(ModBlocks.RELAY_BASE.get()), null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DispatcherNidusBlockEntity>> DISPATCHER_NIDUS =
            BLOCK_ENTITIES.register("dispatcher_nidus", () -> new BlockEntityType<>(
                    DispatcherNidusBlockEntity::new, Set.of(ModBlocks.DISPATCHER_NIDUS.get()), null));

    private ModBlockEntities() {
    }
}
