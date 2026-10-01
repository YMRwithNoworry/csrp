package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.ReinforcementSystem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Visible, non-colliding remains; optional parasite data retains the legacy rebuild counter. */
public final class RemainEntity extends Entity {
    public static final int INFESTED_APPEARANCE = 18;
    private static final EntityDataAccessor<Integer> APPEARANCE = SynchedEntityData.defineId(
            RemainEntity.class, EntityDataSerializers.INT);
    private static final int LIFETIME_TICKS = 20 * 60;
    private static final int WORLD_MOB_CAP = 40;
    private static final int WORLD_MOB_CAP_PER_PLAYER = 5;
    private int age;

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
        builder.define(APPEARANCE, 2);
    }

    public static RemainEntity spawn(ServerLevel level, double x, double y, double z,
            String tier, String variant) {
        int appearance = appearance(tier, variant);
        if (appearance < 0) {
            return null;
        }
        if (appearance == INFESTED_APPEARANCE) {
            BlockPos pos = BlockPos.containing(x, y, z);
            var existing = level.getEntitiesOfClass(RemainEntity.class, new AABB(pos),
                    remains -> remains.isInfestedResidue() && remains.blockPosition().equals(pos));
            if (!existing.isEmpty()) {
                return existing.getFirst();
            }
        }
        RemainEntity remains = ModEntities.REMAIN.get().create(level);
        if (remains == null) {
            return null;
        }
        remains.setAppearance(appearance);
        remains.moveTo(x, y, z, level.random.nextInt(4) * 90.0F, 0.0F);
        return level.addFreshEntity(remains) ? remains : null;
    }

    public static int appearance(String tier, String variant) {
        int family = switch (tier) {
            case "infected", "sim" -> 0;
            case "primitive", "pri" -> 1;
            case "adapted", "ada" -> 2;
            case "pure", "preeminent", "ancient", "nexus_si", "nexus_sii", "nexus_siii", "nexus_siv" -> 3;
            case "feral", "fer" -> 4;
            case "assimara", "mar" -> 5;
            case "infested" -> 6;
            default -> 0;
        };
        if (family == 6) {
            return INFESTED_APPEARANCE;
        }
        return family * 3 + switch (variant) {
            case "small" -> 1;
            case "big" -> 2;
            default -> 0;
        };
    }

    public int getAppearance() {
        return entityData.get(APPEARANCE);
    }

    public void setAppearance(int appearance) {
        entityData.set(APPEARANCE, Mth.clamp(appearance, 0, INFESTED_APPEARANCE));
    }

    public boolean isInfestedResidue() {
        return getAppearance() == INFESTED_APPEARANCE;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = getDeltaMovement();
        if (!isNoGravity()) {
            movement = movement.add(0.0D, -0.04D, 0.0D);
        }
        move(MoverType.SELF, movement);
        setDeltaMovement(onGround() ? Vec3.ZERO : movement.scale(0.98D));
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (++age >= LIFETIME_TICKS && !active && !isInfestedResidue()) {
            discard();
            return;
        }
        if (isInfestedResidue() && onGround()) {
            if (random.nextInt(4096) < serverLevel.getGameRules().getInt(GameRules.RULE_RANDOMTICKING)) {
                ReinforcementSystem.tryFromResidue(serverLevel, blockPosition(), random);
            }
            for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox(), living -> !(living instanceof Parasite))) {
                if (living instanceof Player player && player.isShiftKeyDown()) {
                    continue;
                }
                living.setDeltaMovement(living.getDeltaMovement().multiply(0.84D, 1.0D, 0.86D));
                if (!living.hasEffect(ModMobEffects.COTH) && !living.hasEffect(ModMobEffects.REPEL)) {
                    InfectionMechanics.applyCoth(living, null);
                }
            }
        }
        if (!active || parasite == null) {
            return;
        }

        count += plus;
        if (count > goal) {
            rebuildParasite();
            return;
        }
        if (count % 10 == 0) {
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
        if (!serverLevel.addFreshEntity(rebuilt)) {
            return;
        }
        serverLevel.broadcastEntityEvent(this, (byte) 18);
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
        setAppearance(tag.contains("appearance") ? tag.getInt("appearance") : 2);
        age = Math.max(0, tag.getInt("remainsAge"));
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
        tag.putInt("appearance", getAppearance());
        tag.putInt("remainsAge", age);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
