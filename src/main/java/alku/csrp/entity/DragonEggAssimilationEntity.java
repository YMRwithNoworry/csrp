package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class DragonEggAssimilationEntity extends Entity {
    public static final int ANIMATION_DURATION = 100;
    private static final String ADVANCEMENT_CRITERION = "dragon_egg_assimilated";
    private static final ResourceLocation ADVANCEMENT_ID =
            new ResourceLocation(Csrp.MODID, "again");
    private static final EntityDataAccessor<Integer> ANIMATION_TICKS = SynchedEntityData.defineId(
            DragonEggAssimilationEntity.class, EntityDataSerializers.INT);

    public DragonEggAssimilationEntity(EntityType<? extends DragonEggAssimilationEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static void assimilateDragonEggs(Level level, AABB damageArea) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos min = BlockPos.containing(damageArea.minX, damageArea.minY, damageArea.minZ);
        BlockPos max = BlockPos.containing(damageArea.maxX, damageArea.maxY, damageArea.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!serverLevel.hasChunkAt(pos) || !serverLevel.getBlockState(pos).is(Blocks.DRAGON_EGG)) {
                continue;
            }
            DragonEggAssimilationEntity animation = ModEntities.DRAGON_EGG_ASSIMILATION.get().create(serverLevel);
            if (animation == null) {
                continue;
            }
            animation.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            serverLevel.addFreshEntity(animation);
            serverLevel.playSound(null, animation.blockPosition(), ModSounds.SIM_ADVENTURER_MELT.get(),
                    SoundSource.HOSTILE, 1.5F, 0.7F);
        }
    }

    @Override
    protected void defineSynchedData() {
        builder.define(ANIMATION_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        int ticks = getAnimationTicks() + 1;
        entityData.set(ANIMATION_TICKS, ticks);
        if (level().isClientSide) {
            spawnAssimilationParticles(ticks);
            return;
        }
        if (ticks == 55) {
            level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, 2.5F, 0.65F);
        }
        if (ticks >= ANIMATION_DURATION && level() instanceof ServerLevel serverLevel) {
            finishAssimilation(serverLevel);
        }
    }

    private void spawnAssimilationParticles(int ticks) {
        int count = ticks < 50 ? 1 : 3;
        double radius = 0.35D + ticks / (double) ANIMATION_DURATION * 0.8D;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double y = random.nextDouble() * 1.2D;
            level().addParticle(i == 0 ? ParticleTypes.PORTAL : ParticleTypes.REVERSE_PORTAL,
                    getX() + Math.cos(angle) * radius, getY() + y, getZ() + Math.sin(angle) * radius,
                    -Math.cos(angle) * 0.05D, 0.03D, -Math.sin(angle) * 0.05D);
        }
    }

    private void finishAssimilation(ServerLevel level) {
        AssimilatedDragonEntity dragon = ModEntities.SIM_DRAGONE.get().create(level);
        if (dragon != null) {
            dragon.setPos(getX(), getY() + 0.5D, getZ());
            dragon.setYRot(getYRot());
            dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()),
                    MobSpawnType.TRIGGERED, null);
            level.addFreshEntity(dragon);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.8D, getZ(),
                120, 1.2D, 1.0D, 1.2D, 0.15D);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 1.8F, 0.65F);
        awardNearbyPlayers(level);
        discard();
    }

    private void awardNearbyPlayers(ServerLevel level) {
        AdvancementHolder advancement = level.getServer().getAdvancements().get(ADVANCEMENT_ID);
        if (advancement == null) {
            return;
        }
        for (ServerPlayer player : level.getPlayers(player -> player.distanceToSqr(this) <= 4096.0D)) {
            player.getAdvancements().award(advancement, ADVANCEMENT_CRITERION);
        }
    }

    public int getAnimationTicks() {
        return entityData.get(ANIMATION_TICKS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(ANIMATION_TICKS, tag.getInt("animation_ticks"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("animation_ticks", getAnimationTicks());
    }
}
