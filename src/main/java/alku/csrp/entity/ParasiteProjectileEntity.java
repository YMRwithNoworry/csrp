package alku.csrp.entity;

import alku.csrp.block.SrpWebBlock;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ParasiteProjectileEntity extends Entity {
    private static final int ELVIA_NADE_START_DELAY_TICKS = 3;
    private static final int ELVIA_NADE_FUSE_TICKS = 4;
    private static final int ELVIA_NADE_DURATION_TICKS = 60;
    private static final int ACID_NADE_FUSE_TICKS = 3;
    private static final int ACID_NADE_DURATION_TICKS = 60;
    private static final int YELLOWEYE_NADE_START_DELAY_TICKS = 3;
    private static final int YELLOWEYE_NADE_FUSE_TICKS = 3;
    private static final int YELLOWEYE_NADE_DURATION_TICKS = 60;

    public enum Mode {
        BOMB,
        SPINE,
        WEB,
        METEOR,
        LIGHT,
        ACID,
        VOMIT,
        NEEDLE,
        WITHER,
        LENCIA_BALL,
        ELVIA_BALL,
        ELVIA_NADE,
        YELLOWEYE_SPINE,
        YELLOWEYE_NADE,
        ALAFHA_BALL,
        ANGED_BALL,
        ANCIENT_BALL,
        DRAGON_MISSILE,
        SALIVA_EFFECT,
        BIOMASS_BALL,
        HOMING
    }

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOMING_TARGET = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NADE_ARMED = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> NADE_FUSE_PROGRESS = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ACID_NADE_ARMED = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACID_NADE_FUSE_PROGRESS = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private float damage = 4.0F;
    private double radius = 1.0;
    private int maximumLifetime = 80;
    private Vec3 acceleration = Vec3.ZERO;
    private boolean accelerating;
    private int nadeIgnitionTicks;
    private int nadeFuseTicks;
    private int nadeDamageTicks;
    private int acidNadeTicks;
    private int acidNadeFuseTicks;
    private int acidDamageTicks;
    private int webKind;

    public ParasiteProjectileEntity(EntityType<? extends ParasiteProjectileEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ParasiteProjectileEntity(EntityType<? extends ParasiteProjectileEntity> type, Level level, Mode defaultMode) {
        this(type, level);
        entityData.set(MODE, defaultMode.ordinal());
    }

    public void configure(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 target,
                          double speed, float damage, double radius, int maximumLifetime) {
        configure(owner, mode, start, target, speed, damage, radius, maximumLifetime, null);
    }

    public void configure(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 target,
                          double speed, float damage, double radius, int maximumLifetime,
                          LivingEntity homingTarget) {
        ownerId = owner.getUUID();
        entityData.set(MODE, mode.ordinal());
        entityData.set(HOMING_TARGET, homingTarget == null ? 0 : homingTarget.getId());
        this.damage = damage;
        this.radius = radius;
        this.maximumLifetime = maximumLifetime;
        setPos(start);
        Vec3 direction = target.subtract(start);
        if (direction.lengthSqr() > 0.001) {
            setDeltaMovement(direction.normalize().scale(speed));
        }
    }

    public void configureAccelerating(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 accelerationDirection,
                                      float damage, double radius) {
        configureLegacyFireball(owner, mode, start, accelerationDirection, damage, radius, Integer.MAX_VALUE);
    }

    public void configureLegacyFireball(PrimitiveParasiteEntity owner, Mode mode, Vec3 start,
                                       Vec3 accelerationDirection, float damage, double radius,
                                       int maximumLifetime) {
        ownerId = owner.getUUID();
        entityData.set(MODE, mode.ordinal());
        entityData.set(HOMING_TARGET, 0);
        entityData.set(NADE_ARMED, false);
        entityData.set(NADE_FUSE_PROGRESS, 0);
        this.damage = damage;
        this.radius = radius;
        this.maximumLifetime = Math.max(1, maximumLifetime);
        setPos(start);
        acceleration = accelerationDirection.lengthSqr() > 0.001D
                ? accelerationDirection.normalize().scale(0.1D) : Vec3.ZERO;
        accelerating = true;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        Mode mode = getMode();
        PrimitiveParasiteEntity owner = owner();
        boolean armedNade = mode == Mode.ELVIA_NADE && entityData.get(NADE_ARMED);
        boolean armedAcidNade = mode == Mode.ACID && entityData.get(ACID_NADE_ARMED);
        boolean armedYelloweyeNade = mode == Mode.YELLOWEYE_NADE && entityData.get(ACID_NADE_ARMED);
        if (!level().isClientSide && (owner == null || !owner.isAlive())
                && !armedNade && !armedAcidNade && !armedYelloweyeNade) {
            discard();
            return;
        }
        if (armedNade) {
            tickElviaNade(owner);
            return;
        }
        if (armedAcidNade) {
            tickAcidNade(owner);
            return;
        }
        if (armedYelloweyeNade) {
            tickYelloweyeNade(owner);
            return;
        }

        Vec3 start = position();
        Vec3 movement = steerTowardsHomingTarget(owner, getDeltaMovement(), mode);
        Vec3 end = start.add(movement);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        setPos(end.x, end.y, end.z);

        if (level().isClientSide) {
            ParticleOptions particle = switch (mode) {
                case BOMB, METEOR -> ParticleTypes.FLAME;
                case LIGHT, HOMING, WITHER -> ParticleTypes.SOUL_FIRE_FLAME;
                case SPINE, NEEDLE -> ParticleTypes.CRIT;
                case WEB -> ParticleTypes.WHITE_ASH;
                case ACID, YELLOWEYE_SPINE, YELLOWEYE_NADE, ALAFHA_BALL, ANGED_BALL,
                        ANCIENT_BALL, SALIVA_EFFECT, BIOMASS_BALL -> ParticleTypes.ITEM_SLIME;
                case DRAGON_MISSILE -> ParticleTypes.DRAGON_BREATH;
                case VOMIT -> ParticleTypes.WITCH;
                case LENCIA_BALL, ELVIA_BALL -> ParticleTypes.EXPLOSION;
                case ELVIA_NADE -> ParticleTypes.ITEM_SLIME;
            };
            level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            return;
        }

        if (mode == Mode.BOMB || mode == Mode.METEOR || mode == Mode.ACID || mode == Mode.VOMIT) {
            setDeltaMovement(movement.add(0.0, -0.025, 0.0));
        } else if (accelerating) {
            setDeltaMovement(movement.add(acceleration).scale(0.95D));
        }

        LivingEntity hit = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.65),
                        target -> canCollideWith(owner, mode, target))
                .stream().findFirst().orElse(null);
        if (mode != Mode.HOMING && blockHit.getType() != HitResult.Type.MISS
                || hit != null || tickCount >= maximumLifetime) {
            if (mode == Mode.WEB && hit == null && blockHit.getType() == HitResult.Type.BLOCK) {
                placeWeb(BlockPos.containing(blockHit.getLocation()));
            }
            impact(owner, mode, hit);
        }
    }

    private boolean canCollideWith(PrimitiveParasiteEntity owner, Mode mode, LivingEntity target) {
        if (owner == null || target == owner || !target.isAlive()) {
            return false;
        }
        return isLegacyProjectile(mode) || owner.isValidParasiteTarget(target);
    }

    private static boolean isLegacyProjectile(Mode mode) {
        return mode == Mode.LENCIA_BALL || mode == Mode.ELVIA_BALL || mode == Mode.ELVIA_NADE;
    }

    private Vec3 steerTowardsHomingTarget(PrimitiveParasiteEntity owner, Vec3 movement, Mode mode) {
        if (level().isClientSide || (mode != Mode.LIGHT && mode != Mode.HOMING)
                || tickCount < (mode == Mode.HOMING ? 1 : 10) || owner == null) {
            return movement;
        }
        Entity entity = level().getEntity(entityData.get(HOMING_TARGET));
        if (!(entity instanceof LivingEntity target) || !owner.isValidParasiteTarget(target)) {
            return movement;
        }
        Vec3 destination = mode == Mode.HOMING
                ? new Vec3(target.getX(), target.getY() - target.getBbHeight() * 1.5D, target.getZ())
                : target.getEyePosition();
        Vec3 direction = destination.subtract(position());
        if (direction.lengthSqr() < 0.001D) {
            return movement;
        }
        double steering = mode == Mode.HOMING ? 0.075D : 0.42D;
        double retained = mode == Mode.HOMING ? 1.0D : 0.78D;
        return movement.scale(retained).add(direction.normalize().scale(steering));
    }

    private void impact(PrimitiveParasiteEntity owner, Mode mode, LivingEntity directHit) {
        if (owner == null) {
            discard();
            return;
        }
        if (isLegacyProjectile(mode)) {
            impactLegacyProjectile(owner, mode, directHit);
            return;
        }
        if (mode == Mode.ACID || mode == Mode.YELLOWEYE_NADE) {
            spawnNade(owner, mode == Mode.YELLOWEYE_NADE ? NadeEntity.Kind.YELLOWEYE : NadeEntity.Kind.ACID);
            return;
        }
        if (mode == Mode.YELLOWEYE_SPINE) {
            impactYelloweyeSpine(owner, directHit);
            return;
        }
        if (mode != Mode.WEB) {
            boolean launch = mode == Mode.BOMB || mode == Mode.METEOR || mode == Mode.ACID;
            owner.hurtNearby(this, radius, damage, launch);
        }
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), owner::isValidParasiteTarget)) {
            switch (mode) {
                case SPINE -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
                    if (owner instanceof DeterrentParasiteEntity deterrent
                            && deterrent.getKind() == DeterrentParasiteEntity.Kind.SENTRY) {
                        deterrent.applySentrySpineEffects(target);
                    }
                }
                case WEB -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), owner);
                    if (webKind >= 1) {
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
                    }
                }
                case NEEDLE -> target.addEffect(new MobEffectInstance(ModMobEffects.NEEDLER, 180, 0), owner);
                case WITHER -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1), owner);
                case ANCIENT_BALL -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), owner);
                case LIGHT -> {
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0), owner);
                }
                case HOMING -> {
                }
                case VOMIT, ALAFHA_BALL, ANGED_BALL, SALIVA_EFFECT -> {
                    target.addEffect(new MobEffectInstance(ModMobEffects.VOMIT, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1), owner);
                }
                case DRAGON_MISSILE -> {
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 200, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0), owner);
                }
                case BOMB -> {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0), owner);
                    target.igniteForSeconds(4.0F);
                }
                case METEOR -> target.igniteForSeconds(4.0F);
            }
        }
        if (mode == Mode.BOMB || mode == Mode.METEOR) {
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(radius));
            spawnLingeringCothCloud(owner);
            if (mode == Mode.BOMB && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                level().explode(owner, getX(), getY(), getZ(), (float) Math.max(1.5D, radius),
                        Level.ExplosionInteraction.MOB);
            }
        } else if (mode == Mode.VOMIT || mode == Mode.ALAFHA_BALL || mode == Mode.ANGED_BALL
                || mode == Mode.SALIVA_EFFECT) {
            spawnLingeringVomitCloud(owner);
            if (mode == Mode.ALAFHA_BALL && owner instanceof DraconiteEntity) {
                spawnOrbBoom(owner, 15, 1);
            }
        } else if (mode == Mode.ANCIENT_BALL) {
            spawnLingeringAncientCloud(owner);
        } else if (mode == Mode.WITHER || mode == Mode.DRAGON_MISSILE) {
            spawnLingeringWitherCloud(owner);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(mode == Mode.LIGHT || mode == Mode.WITHER ? ParticleTypes.SOUL_FIRE_FLAME
                            : mode == Mode.ACID || mode == Mode.VOMIT ? ParticleTypes.WITCH
                            : mode == Mode.WEB ? ParticleTypes.WHITE_ASH : ParticleTypes.EXPLOSION,
                    getX(), getY(), getZ(), 12, radius * 0.25, radius * 0.25, radius * 0.25, 0.02);
        }
        discard();
    }

    private void spawnOrbBoom(PrimitiveParasiteEntity owner, int fuse, int waitStart) {
        OrbBoomEntity orb = ModEntities.ORB_BOOM.get().create(level());
        if (orb == null) {
            return;
        }
        orb.configure(owner, fuse, waitStart);
        orb.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        level().addFreshEntity(orb);
    }

    private void impactYelloweyeSpine(PrimitiveParasiteEntity owner, LivingEntity directHit) {
        if (directHit != null) {
            directHit.hurt(damageSources().mobProjectile(this, owner), damage);
            directHit.addEffect(new MobEffectInstance(MobEffects.POISON,
                    MobsConfig.yelloweyePoisonDurationTicks(), MobsConfig.yelloweyePoisonAmplifier(),
                    false, false), owner);
            damageArmor(directHit, MobsConfig.yelloweyeGearDamage());
            owner.applyPrimitiveMinimumDamage(directHit);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(),
                    6, 0.08D, 0.08D, 0.08D, 0.02D);
        }
        discard();
    }

    private static void damageArmor(LivingEntity target, double percentage) {
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = target.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()
                    && armor.getMaxDamage() * 0.1D < armor.getMaxDamage() - armor.getDamageValue()) {
                armor.hurtAndBreak(Math.max(1, (int) (armor.getMaxDamage() * percentage)), target, slot);
            }
        }
    }

    private void placeWeb(BlockPos pos) {
        if (level().isClientSide || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockState state = level().getBlockState(pos);
        if (!state.canBeReplaced()) {
            return;
        }
        SrpWebBlock.Kind kind = SrpWebBlock.Kind.values()[Math.max(0, Math.min(webKind, 2))];
        level().setBlockAndUpdate(pos, ModBlocks.SRP_WEB.get().defaultBlockState()
                .setValue(SrpWebBlock.KIND, kind)
                .setValue(SrpWebBlock.AGE, 0));
    }

    private void impactLegacyProjectile(PrimitiveParasiteEntity owner, Mode mode, LivingEntity directHit) {
        if ((mode == Mode.LENCIA_BALL || mode == Mode.ELVIA_BALL) && isProtectedParasite(directHit)) {
            discard();
            return;
        }
        if (mode == Mode.ELVIA_NADE) {
            spawnNade(owner, NadeEntity.Kind.ELVIA);
            return;
        }
        if (directHit != null) {
            directHit.hurt(damageSources().mobProjectile(this, owner), damage);
        }
        if (mode == Mode.LENCIA_BALL) {
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(10.0D));
            level().explode(owner, getX(), getY(), getZ(), 10.0F, Level.ExplosionInteraction.MOB);
        }
        discard();
    }

    private void spawnNade(PrimitiveParasiteEntity owner, NadeEntity.Kind kind) {
        NadeEntity nade = ModEntities.NADE.get().create(level());
        if (nade != null) {
            nade.configure(owner, kind);
            nade.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            level().addFreshEntity(nade);
        }
        discard();
    }

    private static boolean isProtectedParasite(LivingEntity target) {
        return target instanceof Parasite
                && (!(target instanceof DeterrentParasiteEntity deterrent)
                || deterrent.getKind() != DeterrentParasiteEntity.Kind.SEIZER);
    }

    private void tickElviaNade(PrimitiveParasiteEntity owner) {
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            for (int index = 0; index < 5; index++) {
                level().addParticle(ParticleTypes.SMOKE, getRandomX(1.0D), getRandomY(), getRandomZ(1.0D),
                        0.0D, 0.0D, 0.0D);
            }
            for (int index = 0; index < 2; index++) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(1.0D), getRandomY(), getRandomZ(1.0D),
                        0.0D, 0.0D, 0.0D);
            }
            return;
        }
        nadeIgnitionTicks++;
        if (nadeIgnitionTicks == 2) {
            playSound(ModSounds.NADE_IGNITE.get(), 1.0F, 1.0F);
        }
        if (nadeIgnitionTicks <= ELVIA_NADE_START_DELAY_TICKS) {
            return;
        }
        nadeFuseTicks++;
        entityData.set(NADE_FUSE_PROGRESS, Math.min(nadeFuseTicks, ELVIA_NADE_FUSE_TICKS - 1));
        if (nadeFuseTicks < ELVIA_NADE_FUSE_TICKS) {
            return;
        }
        nadeDamageTicks++;
        if (owner != null && owner.isAlive()) {
            AABB damageArea = new AABB(getX() - 1.45D, getY(), getZ() - 1.45D,
                    getX() + 1.45D, getY() + 1.46D, getZ() + 1.45D);
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), damageArea);
            float attackDamage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, damageArea,
                    owner::isValidParasiteTarget)) {
                target.hurt(damageSources().mobAttack(owner), attackDamage);
            }
        }
        if (nadeDamageTicks > ELVIA_NADE_DURATION_TICKS) {
            discard();
        }
    }

    private void armAcidNade() {
        setDeltaMovement(Vec3.ZERO);
        entityData.set(ACID_NADE_ARMED, true);
        entityData.set(ACID_NADE_FUSE_PROGRESS, 0);
        acidNadeTicks = 0;
        acidNadeFuseTicks = 0;
        acidDamageTicks = 0;
    }

    private void tickAcidNade(PrimitiveParasiteEntity owner) {
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            for (int index = 0; index < 4; index++) {
                level().addParticle(ParticleTypes.ITEM_SLIME, getRandomX(getRenderWidth()),
                        getY() + random.nextDouble() * getRenderHeight(), getRandomZ(getRenderWidth()),
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }

        acidNadeTicks++;
        if (acidNadeTicks == 2) {
            playSound(ModSounds.NADE_IGNITE.get(), 1.0F, 1.0F);
        }
        entityData.set(ACID_NADE_FUSE_PROGRESS, Math.min(acidNadeTicks, ACID_NADE_FUSE_TICKS));
        if (acidNadeTicks < ACID_NADE_FUSE_TICKS || owner == null || !owner.isAlive()) {
            if (acidNadeTicks > ACID_NADE_FUSE_TICKS + ACID_NADE_DURATION_TICKS) {
                discard();
            }
            return;
        }

        acidDamageTicks++;
        double halfWidth = getRenderWidth() * 0.5D;
        AABB damageArea = new AABB(getX() - halfWidth, getY(), getZ() - halfWidth,
                getX() + halfWidth, getY() + getRenderHeight(), getZ() + halfWidth);
        float frameDamage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, damageArea,
                owner::isValidParasiteTarget)) {
            target.invulnerableTime = 0;
            target.hurt(damageSources().mobAttack(owner), frameDamage);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), owner);
            target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 60, 0), owner);
        }
        if (acidDamageTicks >= ACID_NADE_DURATION_TICKS) {
            discard();
        }
    }

    private void tickYelloweyeNade(PrimitiveParasiteEntity owner) {
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            for (int index = 0; index < 4; index++) {
                level().addParticle(ParticleTypes.ITEM_SLIME, getRandomX(getRenderWidth()),
                        getY() + random.nextDouble() * getRenderHeight(), getRandomZ(getRenderWidth()),
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }

        acidNadeTicks++;
        if (acidNadeTicks == 2) {
            playSound(ModSounds.NADE_IGNITE.get(), 1.0F, 1.0F);
        }
        if (acidNadeTicks <= YELLOWEYE_NADE_START_DELAY_TICKS) {
            return;
        }
        acidNadeFuseTicks++;
        entityData.set(ACID_NADE_FUSE_PROGRESS,
                Math.min(acidNadeFuseTicks, YELLOWEYE_NADE_FUSE_TICKS));
        if (acidNadeFuseTicks < YELLOWEYE_NADE_FUSE_TICKS || owner == null || !owner.isAlive()) {
            if (acidNadeFuseTicks > YELLOWEYE_NADE_FUSE_TICKS + YELLOWEYE_NADE_DURATION_TICKS) {
                discard();
            }
            return;
        }

        acidDamageTicks++;
        double halfWidth = getRenderWidth() * 0.5D;
        AABB damageArea = new AABB(getX() - halfWidth, getY(), getZ() - halfWidth,
                getX() + halfWidth, getY() + getRenderHeight(), getZ() + halfWidth);
        float frameDamage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, damageArea,
                owner::isValidParasiteTarget)) {
            target.invulnerableTime = 0;
            target.hurt(damageSources().magic(), frameDamage);
            owner.applyPrimitiveMinimumDamage(target);
        }
        if (acidDamageTicks > YELLOWEYE_NADE_DURATION_TICKS) {
            discard();
        }
    }

    private void spawnLingeringCothCloud(PrimitiveParasiteEntity owner) {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius + 1.0D));
        cloud.setDuration(60);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 0, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringVomitCloud(PrimitiveParasiteEntity owner) {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius));
        cloud.setDuration(70);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VOMIT, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringWitherCloud(PrimitiveParasiteEntity owner) {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius + 0.75D));
        cloud.setDuration(100);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringAncientCloud(PrimitiveParasiteEntity owner) {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius(1.2F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setWaitTime(5);
        cloud.setDuration(600);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 0, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0, false, false));
        level().addFreshEntity(cloud);
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, Mode.SPINE.ordinal());
        builder.define(HOMING_TARGET, 0);
        builder.define(NADE_ARMED, false);
        builder.define(NADE_FUSE_PROGRESS, 0);
        builder.define(ACID_NADE_ARMED, false);
        builder.define(ACID_NADE_FUSE_PROGRESS, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        entityData.set(MODE, sanitizeMode(tag.getInt("mode")));
        entityData.set(HOMING_TARGET, tag.getInt("homing_target"));
        entityData.set(NADE_ARMED, tag.getBoolean("nade_armed"));
        entityData.set(NADE_FUSE_PROGRESS, tag.getInt("nade_fuse_progress"));
        entityData.set(ACID_NADE_ARMED, tag.getBoolean("acid_nade_armed"));
        entityData.set(ACID_NADE_FUSE_PROGRESS, tag.getInt("acid_nade_fuse_progress"));
        damage = tag.getFloat("damage");
        radius = tag.getDouble("radius");
        maximumLifetime = tag.getInt("maximum_lifetime");
        acceleration = new Vec3(tag.getDouble("acceleration_x"), tag.getDouble("acceleration_y"),
                tag.getDouble("acceleration_z"));
        accelerating = tag.getBoolean("accelerating");
        nadeIgnitionTicks = tag.getInt("nade_ignition_ticks");
        nadeFuseTicks = tag.getInt("nade_fuse_ticks");
        nadeDamageTicks = tag.getInt("nade_damage_ticks");
        acidNadeTicks = tag.getInt("acid_nade_ticks");
        acidNadeFuseTicks = tag.getInt("acid_nade_fuse_ticks");
        acidDamageTicks = tag.getInt("acid_damage_ticks");
        webKind = tag.getInt("web_kind");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putInt("mode", entityData.get(MODE));
        tag.putInt("homing_target", entityData.get(HOMING_TARGET));
        tag.putBoolean("nade_armed", entityData.get(NADE_ARMED));
        tag.putInt("nade_fuse_progress", entityData.get(NADE_FUSE_PROGRESS));
        tag.putBoolean("acid_nade_armed", entityData.get(ACID_NADE_ARMED));
        tag.putInt("acid_nade_fuse_progress", entityData.get(ACID_NADE_FUSE_PROGRESS));
        tag.putFloat("damage", damage);
        tag.putDouble("radius", radius);
        tag.putInt("maximum_lifetime", maximumLifetime);
        tag.putDouble("acceleration_x", acceleration.x);
        tag.putDouble("acceleration_y", acceleration.y);
        tag.putDouble("acceleration_z", acceleration.z);
        tag.putBoolean("accelerating", accelerating);
        tag.putInt("nade_ignition_ticks", nadeIgnitionTicks);
        tag.putInt("nade_fuse_ticks", nadeFuseTicks);
        tag.putInt("nade_damage_ticks", nadeDamageTicks);
        tag.putInt("acid_nade_ticks", acidNadeTicks);
        tag.putInt("acid_nade_fuse_ticks", acidNadeFuseTicks);
        tag.putInt("acid_damage_ticks", acidDamageTicks);
        tag.putInt("web_kind", webKind);
    }

    public Mode getMode() {
        return Mode.values()[sanitizeMode(entityData.get(MODE))];
    }

    public boolean isLegacyProjectileMode() {
        return isLegacyProjectile(getMode());
    }

    public boolean shouldRenderAsBillboard() {
        return isLegacyProjectileMode() || getMode() == Mode.ACID
                || getMode() == Mode.YELLOWEYE_SPINE
                || getMode() == Mode.YELLOWEYE_NADE && !entityData.get(ACID_NADE_ARMED)
                || getMode() == Mode.ALAFHA_BALL || getMode() == Mode.ANGED_BALL
                || getMode() == Mode.ANCIENT_BALL || getMode() == Mode.DRAGON_MISSILE
                || getMode() == Mode.SALIVA_EFFECT || getMode() == Mode.BIOMASS_BALL;
    }

    public float getRenderWidth() {
        if (getMode() == Mode.ACID || getMode() == Mode.YELLOWEYE_NADE) {
            return 0.5F + entityData.get(ACID_NADE_FUSE_PROGRESS) * 0.8F;
        }
        if (getMode() == Mode.YELLOWEYE_SPINE) {
            return 0.5F;
        }
        if (getMode() != Mode.ELVIA_NADE || !entityData.get(NADE_ARMED)) {
            return 0.3F;
        }
        return 0.5F + entityData.get(NADE_FUSE_PROGRESS) * 0.8F;
    }

    public float getRenderHeight() {
        if (getMode() == Mode.ACID || getMode() == Mode.YELLOWEYE_NADE) {
            return 0.5F + entityData.get(ACID_NADE_FUSE_PROGRESS) * 0.32F;
        }
        if (getMode() == Mode.YELLOWEYE_SPINE) {
            return 0.5F;
        }
        if (getMode() != Mode.ELVIA_NADE || !entityData.get(NADE_ARMED)) {
            return 0.3F;
        }
        return 0.5F + entityData.get(NADE_FUSE_PROGRESS) * 0.32F;
    }

    public void setWebKind(int kind) {
        this.webKind = kind;
    }

    public boolean isYelloweyeNadeArmed() {
        return getMode() == Mode.YELLOWEYE_NADE && entityData.get(ACID_NADE_ARMED);
    }

    private static int sanitizeMode(int modeIndex) {
        return modeIndex >= 0 && modeIndex < Mode.values().length ? modeIndex : Mode.SPINE.ordinal();
    }
}
