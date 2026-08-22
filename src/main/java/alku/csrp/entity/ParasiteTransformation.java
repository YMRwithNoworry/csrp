package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/** Shared immediate transformations used by the original creative evolution tools. */
public final class ParasiteTransformation {
    private ParasiteTransformation() {
    }

    public static boolean canEvolve(LivingEntity source) {
        return source instanceof Parasite && evolutionType(source) != null;
    }

    public static boolean evolve(LivingEntity source) {
        if (!(source.level() instanceof ServerLevel level) || !(source instanceof Parasite)) {
            return false;
        }
        EntityType<?> targetType = evolutionType(source);
        if (targetType == null) {
            return false;
        }
        boolean transformed = replace(level, source, targetType);
        if (transformed && source.getType() == ModEntities.BUGLIN.get()) {
            source.playSound(ModSounds.BUGLIN_GROW.get(), 1.0F, 1.0F);
        }
        return transformed;
    }

    public static boolean devolve(LivingEntity source) {
        if (!(source.level() instanceof ServerLevel level) || !(source instanceof Parasite)) {
            return false;
        }
        EntityType<?> targetType = devolutionType(source);
        if (targetType != null && !replace(level, source, targetType)) {
            return false;
        }
        if (targetType == null) {
            source.discard();
        }
        return true;
    }

    private static EntityType<?> evolutionType(LivingEntity source) {
        EntityType<?> type = source.getType();
        if (type == ModEntities.BUGLIN.get()) return ModEntities.RUPTER.get();
        if (type == ModEntities.RUPTER.get()) return ModEntities.MANGLER.get();
        if (type == ModEntities.MOVINGFLESH.get()) return randomPrimitive(source);
        if (type == ModEntities.SIM_ADVENTURER.get()) return ModEntities.THRALL.get();
        if (type == ModEntities.HOST.get()) return ModEntities.HOSTII.get();
        if (type == ModEntities.CRUX_INCOMPLETE.get()) return ModEntities.CRUX.get();

        EntityType<?> nexus = nextNexusStage(type);
        if (nexus != null) {
            return nexus;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!id.getNamespace().equals(Csrp.MODID)) {
            return null;
        }
        String path = id.getPath();
        if (path.startsWith("pri_")) {
            return registeredType("ada_" + path.substring("pri_".length())).orElse(null);
        }
        if (path.startsWith("sim_") && !path.endsWith("_head") && !path.equals("sim_wolf")) {
            return registeredType("fer_" + path.substring("sim_".length())).orElse(null);
        }
        return null;
    }

    private static EntityType<?> devolutionType(LivingEntity source) {
        EntityType<?> type = source.getType();
        if (type == ModEntities.RUPTER.get()) return ModEntities.BUGLIN.get();
        if (type == ModEntities.MANGLER.get()) return ModEntities.RUPTER.get();
        if (type == ModEntities.THRALL.get()) return ModEntities.SIM_ADVENTURER.get();
        if (type == ModEntities.HOSTII.get()) return ModEntities.HOST.get();
        if (type == ModEntities.CRUX.get()) return ModEntities.CRUX_INCOMPLETE.get();
        if (type == ModEntities.ADA_VERMIN.get()) return ModEntities.MOVINGFLESH.get();

        EntityType<?> nexus = previousNexusStage(type);
        if (nexus != null) {
            return nexus;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!id.getNamespace().equals(Csrp.MODID)) {
            return null;
        }
        String path = id.getPath();
        if (path.startsWith("ada_")) {
            if (type == ModEntities.ADA_BURROWER.get() || type == ModEntities.ADA_DEVOURER.get()
                    || type == ModEntities.ADA_TOZOON.get()) {
                return null;
            }
            return registeredType("pri_" + path.substring("ada_".length())).orElse(null);
        }
        if (path.startsWith("pri_")) {
            if (type == ModEntities.PRI_BURROWER.get() || type == ModEntities.PRI_DEVOURER.get()
                    || type == ModEntities.PRI_TOZOON.get()) {
                return null;
            }
            return ModEntities.MOVINGFLESH.get();
        }
        if (path.startsWith("fer_")) {
            return registeredType("sim_" + path.substring("fer_".length())).orElse(null);
        }
        return null;
    }

    private static EntityType<?> nextNexusStage(EntityType<?> type) {
        if (type == ModEntities.BECKON_SI.get()) return ModEntities.BECKON_SII.get();
        if (type == ModEntities.BECKON_SII.get()) return ModEntities.BECKON_SIII.get();
        if (type == ModEntities.BECKON_SIII.get()) return ModEntities.BECKON_SIV.get();
        if (type == ModEntities.DISPATCHER_SI.get()) return ModEntities.DISPATCHER_SII.get();
        if (type == ModEntities.DISPATCHER_SII.get()) return ModEntities.DISPATCHER_SIII.get();
        if (type == ModEntities.DISPATCHER_SIII.get()) return ModEntities.DISPATCHER_SIV.get();
        if (type == ModEntities.ROOTER_SI.get()) return ModEntities.ROOTER_SII.get();
        if (type == ModEntities.ROOTER_SII.get()) return ModEntities.ROOTER_SIII.get();
        if (type == ModEntities.ROOTER_SIII.get()) return ModEntities.ROOTER_SIV.get();
        return null;
    }

    private static EntityType<?> previousNexusStage(EntityType<?> type) {
        if (type == ModEntities.BECKON_SII.get()) return ModEntities.BECKON_SI.get();
        if (type == ModEntities.BECKON_SIII.get()) return ModEntities.BECKON_SII.get();
        if (type == ModEntities.BECKON_SIV.get()) return ModEntities.BECKON_SIII.get();
        if (type == ModEntities.DISPATCHER_SII.get()) return ModEntities.DISPATCHER_SI.get();
        if (type == ModEntities.DISPATCHER_SIII.get()) return ModEntities.DISPATCHER_SII.get();
        if (type == ModEntities.DISPATCHER_SIV.get()) return ModEntities.DISPATCHER_SIII.get();
        if (type == ModEntities.ROOTER_SII.get()) return ModEntities.ROOTER_SI.get();
        if (type == ModEntities.ROOTER_SIII.get()) return ModEntities.ROOTER_SII.get();
        if (type == ModEntities.ROOTER_SIV.get()) return ModEntities.ROOTER_SIII.get();
        return null;
    }

    private static EntityType<?> randomPrimitive(LivingEntity source) {
        return switch (source.getRandom().nextInt(12)) {
            case 0 -> ModEntities.PRI_LONGARMS.get();
            case 1 -> ModEntities.PRI_SUMMONER.get();
            case 2 -> ModEntities.PRI_VERMIN.get();
            case 3 -> ModEntities.PRI_VISCERA.get();
            case 4 -> ModEntities.PRI_ARACHNIDA.get();
            case 5 -> ModEntities.PRI_BOLSTER.get();
            case 6 -> ModEntities.PRI_BURROWER.get();
            case 7 -> ModEntities.PRI_DEVOURER.get();
            case 8 -> ModEntities.PRI_MANDUCATER.get();
            case 9 -> ModEntities.PRI_REEKER.get();
            case 10 -> ModEntities.PRI_TOZOON.get();
            default -> ModEntities.PRI_YELLOWEYE.get();
        };
    }

    private static Optional<EntityType<?>> registeredType(String path) {
        ResourceLocation id = new ResourceLocation(Csrp.MODID, path);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                ? Optional.of(BuiltInRegistries.ENTITY_TYPE.get(id)) : Optional.empty();
    }

    private static boolean replace(ServerLevel level, LivingEntity source, EntityType<?> targetType) {
        Entity created = targetType.create(level);
        if (!(created instanceof Mob replacement)) {
            return false;
        }
        replacement.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(source.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        if (source instanceof Mob sourceMob) {
            replacement.setTarget(sourceMob.getTarget());
            if (sourceMob.isPersistenceRequired()) {
                replacement.setPersistenceRequired();
            }
        }
        if (source instanceof PrimitiveParasiteEntity primitiveSource
                && replacement instanceof PrimitiveParasiteEntity primitiveReplacement) {
            primitiveSource.copyDamageAdaptationsTo(primitiveReplacement);
        }
        if (!level.addFreshEntity(replacement)) {
            return false;
        }
        source.discard();
        return true;
    }
}
