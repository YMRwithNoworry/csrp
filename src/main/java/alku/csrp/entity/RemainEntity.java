package alku.csrp.entity;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

/** Invisible rebuild counter stored inside a parasite remains block. */
public final class RemainEntity extends Entity {
    private static final int WORLD_MOB_CAP = 40;
    private static final int WORLD_MOB_CAP_PER_PLAYER = 5;

    private int plus;
    private int count;
    private int goal;
    private boolean active;
    private String parasite;
    private float health;
    private byte skin;

    public RemainEntity(EntityType<? extends RemainEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount % 20 == 0
                && (!level().getBlockState(blockPosition()).is(ModBlocks.INFESTED_REMAINS)
                || parasite == null)) {
            discard();
            return;
        }
        if (!active) {
            return;
        }

        count += plus;
        if (count > goal) {
            rebuildParasite();
            return;
        }
        if (count % 10 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.broadcastEntityEvent(this, (byte) 18);
        }
    }

    private void rebuildParasite() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (countParasites(serverLevel) > WORLD_MOB_CAP
                + serverLevel.players().size() * WORLD_MOB_CAP_PER_PLAYER) {
            count = 0;
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(parasite);
        if (id == null) {
            return;
        }
        if (id.getNamespace().equals("srparasites")) {
            id = ResourceLocation.fromNamespaceAndPath("csrp", id.getPath());
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        Entity created = entityType == null ? null : entityType.create(serverLevel);
        if (!(created instanceof Mob rebuilt)) {
            return;
        }

        rebuilt.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        if (!serverLevel.noCollision(rebuilt)) {
            rebuilt.discard();
            return;
        }
        rebuilt.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        rebuilt.setHealth(rebuilt.getMaxHealth() * health);
        rebuilt.addEffect(new MobEffectInstance(ModMobEffects.DEBAR, 400, 0, false, false), this);
        applyLegacySkin(rebuilt);
        serverLevel.playSound(null, blockPosition(), ModSounds.get("summoner.resurrect"),
                SoundSource.HOSTILE, 1.0F, 1.0F);
        serverLevel.addFreshEntity(rebuilt);
        serverLevel.broadcastEntityEvent(this, (byte) 18);
        serverLevel.setBlockAndUpdate(blockPosition(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        discard();
    }

    private void applyLegacySkin(Mob rebuilt) {
        rebuilt.getPersistentData().putByte("parasiteskin", skin);
    }

    private static int countParasites(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Parasite) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != 18) {
            super.handleEntityEvent(id);
            return;
        }
        ParticleOptions particle = ModParticles.BIOMASS.get();
        for (int i = 0; i < 2; i++) {
            level().addParticle(particle,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 2.0D,
                    getY() + 0.5D + random.nextDouble() * (getBbHeight() + 0.5D),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 2.0D,
                    random.nextGaussian() * 0.02D,
                    random.nextGaussian() * 0.02D,
                    random.nextGaussian() * 0.02D);
        }
    }

    public void setParasite(String parasite) {
        this.parasite = parasite;
    }

    public void setGoal(int goal) {
        this.goal = goal;
    }

    public void setPlus(int plus) {
        if (this.plus < plus) {
            this.plus = plus;
            active = true;
        }
    }

    public void setSkin(byte skin) {
        this.skin = skin;
    }

    public void setHealth(float health) {
        if (this.health < health) {
            this.health = health;
        }
    }

    public boolean isActive() {
        return active;
    }

    public int getProgress() {
        return count;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        parasite = tag.contains("parasiteparasite") ? tag.getString("parasiteparasite") : null;
        active = tag.getBoolean("parasiteactive");
        count = tag.getInt("parasitepoint");
        plus = tag.getInt("parasiteplus");
        goal = tag.getInt("parasitegoal");
        skin = tag.getByte("parasiteskin");
        health = tag.getFloat("parasitehealth");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (parasite != null) {
            tag.putString("parasiteparasite", parasite);
        }
        tag.putBoolean("parasiteactive", active);
        tag.putInt("parasitepoint", count);
        tag.putInt("parasiteplus", plus);
        tag.putInt("parasitegoal", goal);
        tag.putByte("parasiteskin", skin);
        tag.putFloat("parasitehealth", health);
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
