package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Invisible, ground-following minimum-damage wave used by Hosts and Kyphosis. */
public final class WaveEntity extends PathfinderMob implements Parasite {
    private float minimumDamage = 0.1F;
    private int range = 1;
    private int durationSeconds = 1;

    public WaveEntity(EntityType<? extends WaveEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    public void configure(double baseDamage, float minimumDamage, int range,
                          int durationSeconds, LivingEntity target) {
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseDamage);
        }
        this.minimumDamage = Math.max(0.0F, minimumDamage);
        this.range = range;
        this.durationSeconds = Math.max(1, durationSeconds);
        setTarget(target);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnGroundParticles();
            return;
        }

        LivingEntity target = getTarget();
        if (target != null && !target.isAlive()) {
            discard();
            return;
        }
        if (tickCount > 40) {
            if (getX() == xo || getZ() == zo || tickCount > 20 * durationSeconds) {
                discard();
                return;
            }
        }
        if (!level().getFluidState(blockPosition()).isEmpty()) {
            discard();
            return;
        }

        AABB damageArea = new AABB(getX() - getBbWidth() * 0.5D, getY(),
                getZ() - getBbWidth() * 0.5D, getX() + getBbWidth() * 0.5D,
                getY() + getBbHeight(), getZ() + getBbWidth() * 0.5D).inflate(0.4D, 0.2D, 0.4D);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != this && !(entity instanceof Parasite))) {
            applyMinimumDamage(victim);
        }
    }

    private void applyMinimumDamage(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage()
                || !target.isAlive()
                || target instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        float amount = minimumDamage;
        MobEffectInstance viral = target.getEffect(ModMobEffects.VIRAL);
        if (viral != null) {
            amount += minimumDamage * (viral.getAmplifier() + 1);
        }
        if (amount <= 0.0F) {
            return;
        }
        float absorptionDamage = Math.min(target.getAbsorptionAmount(), amount * 0.5F);
        if (absorptionDamage > 0.0F) {
            target.setAbsorptionAmount(target.getAbsorptionAmount() - absorptionDamage);
        }
        target.setHealth(Math.max(0.0F, target.getHealth() - (amount - absorptionDamage)));
        level().broadcastEntityEvent(target, (byte) 2);
        if (target.getHealth() <= 0.0F) {
            target.die(damageSources().mobAttack(this));
        }
    }

    private void spawnGroundParticles() {
        BlockState ground = level().getBlockState(blockPosition().below());
        if (ground.isAir()) {
            return;
        }
        for (int index = 0; index < 15; index++) {
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    getX() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(), getY(),
                    getZ() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(),
                    random.nextGaussian() * 0.02D, random.nextGaussian() + 20.0D,
                    random.nextGaussian() * 0.02D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        discard();
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        minimumDamage = Math.max(0.0F, tag.getFloat("minimum_damage"));
        range = tag.getInt("range");
        durationSeconds = Math.max(1, tag.getInt("duration_seconds"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("minimum_damage", minimumDamage);
        tag.putInt("range", range);
        tag.putInt("duration_seconds", durationSeconds);
    }
}
