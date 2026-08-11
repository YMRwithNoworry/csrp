package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/** Legacy Ancient Drop Pod (EntityDropPod). */
public final class AncientPodEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private static final int DEFAULT_FUSE = 80;

    private final RawAnimation groundedAnimation = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks");
    private final RawAnimation airborneAnimation = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1");
    private byte owner = 62;
    private int fuseTicks = DEFAULT_FUSE;
    private boolean fuseStarted;
    private boolean exploded;

    public AncientPodEntity(EntityType<? extends AncientPodEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!fuseStarted && onGround()) {
            fuseStarted = true;
        }
        if (!fuseStarted || exploded) {
            return;
        }
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0D, getZ(),
                    4, 0.35D, 0.5D, 0.35D, 0.02D);
            if (--fuseTicks <= 0) {
                explodePod(serverLevel);
            }
        }
    }

    public void setOwner(byte owner) {
        this.owner = owner;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 2, this::movementAnimation));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("pod_owner", owner);
        tag.putInt("pod_fuse", fuseTicks);
        tag.putBoolean("pod_fuse_started", fuseStarted);
        tag.putBoolean("pod_exploded", exploded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.contains("pod_owner") ? tag.getByte("pod_owner") : 62;
        fuseTicks = tag.contains("pod_fuse") ? tag.getInt("pod_fuse") : DEFAULT_FUSE;
        fuseStarted = tag.getBoolean("pod_fuse_started");
        exploded = tag.getBoolean("pod_exploded");
    }

    private PlayState movementAnimation(AnimationState<AncientPodEntity> state) {
        // The legacy pod uses status 1 while falling and returns to its normal pose on landing.
        return state.setAndContinue(onGround() ? groundedAnimation : airborneAnimation);
    }

    private void explodePod(ServerLevel level) {
        exploded = true;
        DragonEggAssimilationEntity.assimilateDragonEggs(level, getBoundingBox().inflate(4.0D));
        Level.ExplosionInteraction interaction = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && MobsConfig.ancientPodGriefing()
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level.explode(this, getX(), getY(), getZ(), 4.0F, interaction);
        applyPodEffects(level);
        spawnLingeringCloud(level);
        spawnContents(level);
        discard();
    }

    private void applyPodEffects(ServerLevel level) {
        for (String raw : MobsConfig.ancientPodEffects()) {
            String[] parts = raw.split(";", -1);
            if (parts.length != 3) {
                continue;
            }
            try {
                int duration = Math.max(0, Integer.parseInt(parts[0].trim())) * 20;
                int amplifier = Integer.parseInt(parts[1].trim());
                ResourceLocation id = ResourceLocation.tryParse(parts[2].trim());
                if (id == null) {
                    continue;
                }
                BuiltInRegistries.MOB_EFFECT.getOptional(id).ifPresent(effect -> {
                    for (var target : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                            getBoundingBox().inflate(7.0D), living -> living != this && !(living instanceof Parasite))) {
                        target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                                duration, amplifier, false, false, effect == ModMobEffects.COTH.get()), this);
                    }
                });
            } catch (NumberFormatException ignored) {
                // Ignore malformed hand-edited entries and continue the explosion.
            }
        }
    }

    private void spawnLingeringCloud(ServerLevel level) {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level, getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(getBbWidth() * 2.0F);
        cloud.setWaitTime(5);
        cloud.setDuration(600);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0, false, false, true));
        level.addFreshEntity(cloud);
    }

    private void spawnContents(ServerLevel level) {
        int count = owner == 62 ? MobsConfig.ancientDreadnautPodMaxMobs() : owner == 63 ? 1 : 0;
        int failed = 0;
        int spawned = 0;
        while (spawned < count && failed < 5) {
            Mob mob = createConfiguredMob(level);
            if (mob == null) {
                failed++;
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0D;
            mob.moveTo(getX() + Math.cos(angle) * 1.5D, getY(), getZ() + Math.sin(angle) * 1.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            mob.setTarget(getTarget());
            if (level.addFreshEntity(mob)) {
                spawned++;
            } else {
                failed++;
            }
        }
    }

    private Mob createConfiguredMob(ServerLevel level) {
        var entries = MobsConfig.ancientDreadnautMobList();
        if (entries.isEmpty()) {
            return null;
        }
        double totalWeight = 0.0D;
        for (String raw : entries) {
            totalWeight += entryWeight(raw);
        }
        if (totalWeight <= 0.0D) {
            return null;
        }
        double roll = random.nextDouble() * totalWeight;
        for (String raw : entries) {
            double weight = entryWeight(raw);
            if ((roll -= weight) > 0.0D) {
                continue;
            }
            String id = raw.split(";", -1)[0].trim();
            ResourceLocation location = ResourceLocation.tryParse(id);
            if (location == null) {
                return null;
            }
            if (location.getNamespace().equals("srparasites")) {
                location = ResourceLocation.fromNamespaceAndPath("csrp", location.getPath());
            }
            Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(location).map(type -> type.create(level)).orElse(null);
            return entity instanceof Mob mob ? mob : null;
        }
        return null;
    }

    private static double entryWeight(String raw) {
        String[] parts = raw.split(";", -1);
        if (parts.length < 1) {
            return 0.0D;
        }
        if (parts.length == 1) {
            return 1.0D;
        }
        try {
            return Math.max(0.0D, Double.parseDouble(parts[1].trim()));
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
