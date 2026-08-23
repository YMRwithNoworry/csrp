package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** Shared EntityAIInfectedSearch and EntityCanMelt behavior from SRParasites 1.10.7. */
final class AssimilatedMeltSystem {
    static final int KILL_THRESHOLD = 10;
    private static final int REQUIRED_NEARBY_ASSIMILATED = 3;
    private static final int MINIMUM_MERGE_PHASE = 1;
    private static final DustParticleOptions MELT_GOLD = new DustParticleOptions(
            new Vector3f(127.0F / 255.0F, 106.0F / 255.0F, 0.0F), 1.0F);
    private static final DustParticleOptions MELT_RED = new DustParticleOptions(
            new Vector3f(127.0F / 255.0F, 0.0F, 0.0F), 1.0F);

    private AssimilatedMeltSystem() {
    }

    static boolean tryStartGroup(Mob source, int parasiteKills) {
        if (parasiteKills <= KILL_THRESHOLD || source.getTarget() != null
                || !(source instanceof MeltableAssimilated sourceMeltable)
                || !sourceMeltable.canMelt() || sourceMeltable.isMelting()
                || !(source.level() instanceof ServerLevel serverLevel)
                || Config.evolutionPhase(serverLevel) < MINIMUM_MERGE_PHASE) {
            return false;
        }

        double followRange = source.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        AABB searchBox = source.getBoundingBox().inflate(followRange);
        int movingFleshCount = 0;
        for (MovingFleshEntity flesh : serverLevel.getEntitiesOfClass(
                MovingFleshEntity.class, searchBox, Entity::isAlive)) {
            if (source.hasLineOfSight(flesh)) {
                movingFleshCount += flesh.getMergeCount();
            }
        }
        if (movingFleshCount >= 1 && movingFleshCount <= 3) {
            sourceMeltable.melt();
            return true;
        }

        List<Mob> candidates = serverLevel.getEntitiesOfClass(Mob.class, searchBox,
                entity -> isCandidate(source, entity));
        if (candidates.size() < REQUIRED_NEARBY_ASSIMILATED) {
            return false;
        }
        for (int index = 0; index < REQUIRED_NEARBY_ASSIMILATED; index++) {
            ((MeltableAssimilated) candidates.get(index)).melt();
        }
        sourceMeltable.melt();
        return true;
    }

    static void freeze(Mob entity) {
        entity.getNavigation().stop();
        entity.setTarget(null);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    static void sendMeltParticles(ServerLevel level, Mob entity) {
        double y = entity.getY() + entity.getBbHeight() * 0.5D;
        level.sendParticles(MELT_GOLD, entity.getX(), y, entity.getZ(), 2,
                entity.getBbWidth() * 0.25D, entity.getBbHeight() * 0.2D,
                entity.getBbWidth() * 0.25D, 0.0D);
        level.sendParticles(MELT_RED, entity.getX(), y, entity.getZ(), 2,
                entity.getBbWidth() * 0.25D, entity.getBbHeight() * 0.2D,
                entity.getBbWidth() * 0.25D, 0.0D);
    }

    static boolean spawnMovingFlesh(Mob source, int mergeValue) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        MovingFleshEntity flesh = ModEntities.MOVINGFLESH.get().create(serverLevel);
        if (flesh == null) {
            return false;
        }
        flesh.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        flesh.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(source.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        flesh.setMergeValue(mergeValue);
        flesh.setCustomName(source.getCustomName());
        flesh.setCustomNameVisible(source.isCustomNameVisible());
        if (source.isPersistenceRequired()) {
            flesh.setPersistenceRequired();
        }
        if (!serverLevel.addFreshEntity(flesh)) {
            return false;
        }
        source.discard();
        return true;
    }

    private static boolean isCandidate(Mob source, Mob entity) {
        return entity != source && entity.isAlive() && source.hasLineOfSight(entity)
                && entity.getTarget() == null
                && entity instanceof MeltableAssimilated meltable
                && meltable.canMelt() && !meltable.isMelting();
    }
}
