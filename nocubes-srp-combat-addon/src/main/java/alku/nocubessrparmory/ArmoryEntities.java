package alku.nocubessrparmory;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/**
 * Registers the original 13 MCreator projectile entity IDs. Each one is a real
 * {@link ArmoryArrowEntity} so commands and data packs that reference
 * {@code nocubessrparmory:entitybullet*} keep working after the port.
 */
final class ArmoryEntities {
    static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, NoCubesSrpCombatAddon.MOD_ID);

    private static final Map<ArmoryLauncherItem.Kind,
            DeferredHolder<EntityType<?>, EntityType<ArmoryArrowEntity>>> TYPES =
            new EnumMap<>(ArmoryLauncherItem.Kind.class);

    static {
        for (ArmoryLauncherItem.Kind kind : ArmoryLauncherItem.Kind.values()) {
            TYPES.put(kind, ENTITIES.register(kind.entityId, () -> EntityType.Builder
                    .<ArmoryArrowEntity>of(ArmoryArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ResourceLocation.fromNamespaceAndPath(
                            NoCubesSrpCombatAddon.MOD_ID, kind.entityId).toString())));
        }
    }

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    static EntityType<ArmoryArrowEntity> get(ArmoryLauncherItem.Kind kind) {
        return TYPES.get(kind).get();
    }

    static ArmoryLauncherItem.Kind kindOf(EntityType<?> type) {
        for (Map.Entry<ArmoryLauncherItem.Kind,
                DeferredHolder<EntityType<?>, EntityType<ArmoryArrowEntity>>> entry : TYPES.entrySet()) {
            if (entry.getValue().isBound() && entry.getValue().get() == type) return entry.getKey();
        }
        return ArmoryLauncherItem.Kind.TWISTED_BOMB;
    }

    static ArmoryArrowEntity create(ArmoryLauncherItem.Kind kind, Level level,
            LivingEntity shooter, ItemStack launcher) {
        return new ArmoryArrowEntity(get(kind), level, shooter, kind, launcher);
    }

    private ArmoryEntities() {}
}
