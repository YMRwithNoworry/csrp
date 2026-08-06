package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Legacy deterrent parasites are stationary control units. They share fire
 * weakness and high per-source damage adaptation while each form retains its
 * original battlefield role.
 */
public final class DeterrentParasiteEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Integer> SPECIAL_ACTION = SynchedEntityData.defineId(
            DeterrentParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEIZER_TARGET_ID = SynchedEntityData.defineId(
            DeterrentParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SENTRY_PARASITE_STATUS = SynchedEntityData.defineId(
            DeterrentParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SENTRY_STILL_ANI = SynchedEntityData.defineId(
            DeterrentParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_ADAPTATION_HITS = 6;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 10;
    private static final float ADAPTATION_PER_HIT = 0.16F;
    private static final float ADAPTATION_LEARN_CHANCE = 0.85F;
    private static final float FIRE_SUPPRESSION_CHANCE = 0.50F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation ATTACK_TIMER = ParasiteAnimations.loop(this, "get_attack_timer");
    private final RawAnimation SEIZER_HOLD = ParasiteAnimations.loop(this, "idle.get_targeted_entity_1");
    private final RawAnimation SENTRY_ATTACK = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation SENTRY_FAST_ATTACK = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation SENTRY_SPECIAL = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");

    private final Kind kind;
    private int abilityCooldown;
    private int lifetimeTicks;
    private int attackFlashTicks;
    private int wormMinimumPayload = 3;
    private int wormMaximumPayload = 3;
    private final List<String> wormPayloadTypes = new ArrayList<>();
    private UUID dispatchTarget;
    private String dispatchEntityId;
    private UUID seizerTarget;

    public DeterrentParasiteEntity(EntityType<? extends DeterrentParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
        setNoAi(kind == Kind.DISPATCHER_TENTACLE);
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case KYPHOSIS -> {
                goalSelector.addGoal(1, new KyphosisWaveGoal());
                goalSelector.addGoal(2, new StationaryMeleeGoal(8.0D));
            }
            case SEIZER -> goalSelector.addGoal(1, new SeizerHoldGoal());
            case SENTRY -> {
                goalSelector.addGoal(1, new SentrySpineGoal());
                goalSelector.addGoal(2, new StationaryMeleeGoal(3.0D));
            }
            case WORM -> goalSelector.addGoal(1, new WormEruptionGoal());
            case DISPATCHER_TENTACLE -> {
                // Dispatcher tentacles are directed by their summoner instead of normal target AI.
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPECIAL_ACTION, SpecialAction.NONE.ordinal());
        builder.define(SEIZER_TARGET_ID, 0);
        builder.define(SENTRY_PARASITE_STATUS, 0);
        builder.define(SENTRY_STILL_ANI, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (abilityCooldown > 0) {
            abilityCooldown--;
        }
        if (attackFlashTicks > 0) {
            attackFlashTicks--;
        }
        if (activeKind() == Kind.DISPATCHER_TENTACLE) {
            tickDispatcherTentacle();
        } else if (activeKind() == Kind.SEIZER) {
            maintainSeizerGrip();
        } else if (activeKind() == Kind.SENTRY) {
            tickSentry();
        } else if (activeKind() == Kind.KYPHOSIS) {
            breakBlocksTowardsTarget(7.0F, 3.0D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        Kind activeKind = activeKind();
        if (activeKind == Kind.DISPATCHER_TENTACLE || activeKind == Kind.SEIZER || activeKind == Kind.WORM) {
            return false;
        }
        boolean hit = super.doHurtTarget(entity);
        if (!hit || !(entity instanceof LivingEntity target)) {
            return hit;
        }
        attackFlashTicks = 12;
        triggerAnim("attack_controller", "attack");
        if (activeKind == Kind.KYPHOSIS) {
            hurtNearby(target, 2.0D, 35.0F, true);
            target.push(0.0D, 0.5D, 0.0D);
        }
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (activeKind() == Kind.SEIZER && source.getDirectEntity() instanceof ParasiteProjectileEntity) {
            LivingEntity heldTarget = getSeizerTarget();
            if (heldTarget != null) {
                heldTarget.hurt(source, amount * 2.0F);
            }
            return false;
        }
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return MAX_ADAPTATION_HITS;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return ADAPTATION_PER_HIT;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return MAX_LEARNABLE_DAMAGE_SOURCES;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return ADAPTATION_LEARN_CHANCE;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return FIRE_SUPPRESSION_CHANCE;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (activeKind() == Kind.SEIZER) {
            clearSeizerTarget();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("deterrent_ability_cooldown", abilityCooldown);
        tag.putInt("deterrent_lifetime", lifetimeTicks);
        tag.putInt("deterrent_attack_flash", attackFlashTicks);
        tag.putInt("deterrent_worm_minimum", wormMinimumPayload);
        tag.putInt("deterrent_worm_maximum", wormMaximumPayload);
        ListTag payloadTypes = new ListTag();
        for (String type : wormPayloadTypes) {
            payloadTypes.add(StringTag.valueOf(type));
        }
        tag.put("deterrent_worm_types", payloadTypes);
        if (dispatchTarget != null) {
            tag.putUUID("deterrent_dispatch_target", dispatchTarget);
        }
        if (dispatchEntityId != null) {
            tag.putString("deterrent_dispatch_entity", dispatchEntityId);
        }
        if (seizerTarget != null) {
            tag.putUUID("deterrent_seizer_target", seizerTarget);
        }
        if (activeKind() == Kind.SENTRY) {
            tag.putInt("sentry_parasite_status", entityData.get(SENTRY_PARASITE_STATUS));
            tag.putBoolean("sentry_still_ani", entityData.get(SENTRY_STILL_ANI));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        abilityCooldown = tag.getInt("deterrent_ability_cooldown");
        lifetimeTicks = tag.getInt("deterrent_lifetime");
        attackFlashTicks = tag.getInt("deterrent_attack_flash");
        wormMinimumPayload = tag.contains("deterrent_worm_minimum") ? tag.getInt("deterrent_worm_minimum") : 3;
        wormMaximumPayload = tag.contains("deterrent_worm_maximum") ? tag.getInt("deterrent_worm_maximum") : 3;
        wormPayloadTypes.clear();
        ListTag payloadTypes = tag.getList("deterrent_worm_types", Tag.TAG_STRING);
        for (int index = 0; index < payloadTypes.size(); index++) {
            wormPayloadTypes.add(payloadTypes.getString(index));
        }
        dispatchTarget = tag.hasUUID("deterrent_dispatch_target") ? tag.getUUID("deterrent_dispatch_target") : null;
        dispatchEntityId = tag.contains("deterrent_dispatch_entity")
                ? tag.getString("deterrent_dispatch_entity") : null;
        seizerTarget = tag.hasUUID("deterrent_seizer_target") ? tag.getUUID("deterrent_seizer_target") : null;
        if (activeKind() == Kind.SENTRY) {
            entityData.set(SENTRY_PARASITE_STATUS, tag.getInt("sentry_parasite_status"));
            entityData.set(SENTRY_STILL_ANI, tag.getBoolean("sentry_still_ani"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    public void setDispatchTarget(LivingEntity target) {
        dispatchTarget = target == null ? null : target.getUUID();
    }

    /** Configures a Dispatcher-created tentacle to deploy a registered parasite type. */
    public void setDispatchEntity(EntityType<? extends Mob> type) {
        dispatchEntityId = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    public void setDispatchEntity(ResourceLocation type) {
        dispatchEntityId = type == null ? null : type.toString();
    }

    public void setLifetimeTicks(int lifetimeTicks) {
        this.lifetimeTicks = Math.max(0, lifetimeTicks);
    }

    /** Configures a scent-created Worm to throw this many parasite waves. */
    public void setWormPayload(int minPayload, int maxPayload) {
        if (activeKind() != Kind.WORM) {
            return;
        }
        wormMinimumPayload = Math.max(0, minPayload);
        wormMaximumPayload = Math.max(wormMinimumPayload, maxPayload);
    }

    /** Configures the exact registered parasite pool launched by this Worm. */
    public void setWormPayloadTypes(List<ResourceLocation> types) {
        if (activeKind() != Kind.WORM) {
            return;
        }
        wormPayloadTypes.clear();
        types.stream().map(ResourceLocation::toString).distinct().forEach(wormPayloadTypes::add);
    }

    public Kind getKind() {
        return activeKind();
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.KYPHOSIS.get()) return Kind.KYPHOSIS;
        if (type == ModEntities.SEIZER.get()) return Kind.SEIZER;
        if (type == ModEntities.SENTRY.get()) return Kind.SENTRY;
        if (type == ModEntities.WORM.get()) return Kind.WORM;
        return Kind.DISPATCHER_TENTACLE;
    }

    private PlayState movementAnimation(AnimationState<DeterrentParasiteEntity> state) {
        if (activeKind() == Kind.SENTRY) {
            int status = getSentryParasiteStatus();
            // 状态 1: 攻击动画（触手摆动频率增加）
            if (status == 1) {
                return state.setAndContinue(SENTRY_ATTACK);
            }
            // 状态 2: 更快速的攻击动画
            else if (status == 2) {
                return state.setAndContinue(SENTRY_FAST_ATTACK);
            }
            // 状态 9: 特殊状态（仅触手动画）
            else if (status == 9) {
                return state.setAndContinue(SENTRY_SPECIAL);
            }
            // 状态 0: 默认空闲动画
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(switch (specialAction()) {
            case KYPHOSIS_WAVE, WORM_ERUPTION -> ATTACK_TIMER;
            case SEIZER_HOLD -> SEIZER_HOLD;
            case DISPATCHER_DEPLOY -> ATTACK;
            case NONE -> IDLE;
        });
    }

    private void tickDispatcherTentacle() {
        if (lifetimeTicks == 40) {
            setSpecialAction(SpecialAction.DISPATCHER_DEPLOY);
        }
        if (++lifetimeTicks < 60) {
            return;
        }
        if (dispatchEntityId != null && spawnDispatchedEntity()) {
            discard();
            return;
        }
        LivingEntity target = findEntity(dispatchTarget);
        if (target == null || distanceToSqr(target) > 1024.0D) {
            discard();
            return;
        }
        target.teleportTo(getX(), getY(), getZ());
        target.setDeltaMovement(Vec3.ZERO);
        if (isOnFire()) {
            target.setHealth(Math.max(1.0F, target.getMaxHealth() * 0.5F));
            target.igniteForSeconds(8.0F);
        }
        level().addParticle(ParticleTypes.PORTAL, getX(), getY() + getBbHeight() * 0.5D, getZ(), 0.0D, 0.2D, 0.0D);
        discard();
    }

    private boolean spawnDispatchedEntity() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        net.minecraft.resources.ResourceLocation entityId = net.minecraft.resources.ResourceLocation.tryParse(dispatchEntityId);
        if (entityId == null) {
            return false;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null || !(type.create(serverLevel) instanceof Mob mob)) {
            return false;
        }
        mob.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        mob.setTarget(getTarget());
        if (isOnFire()) {
            mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * 0.5F));
            mob.igniteForSeconds(8.0F);
        }
        serverLevel.addFreshEntity(mob);
        return true;
    }

    private void maintainSeizerGrip() {
        LivingEntity target = getSeizerTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 25.0D || !hasLineOfSight(target)) {
            clearSeizerTarget();
            return;
        }
        if (entityData.get(SEIZER_TARGET_ID) != target.getId()) {
            entityData.set(SEIZER_TARGET_ID, target.getId());
            setSpecialAction(SpecialAction.SEIZER_HOLD);
        }
        Vec3 pull = position().subtract(target.position());
        if (pull.lengthSqr() > 0.001D) {
            pull = pull.normalize().scale(0.50D);
            target.push(pull.x, 0.0D, pull.z);
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6, false, false), this);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2, false, false), this);
    }

    private LivingEntity getSeizerTarget() {
        if (level().isClientSide) {
            Entity target = level().getEntity(entityData.get(SEIZER_TARGET_ID));
            return target instanceof LivingEntity living && living.isAlive() ? living : null;
        }
        LivingEntity target = findEntity(seizerTarget);
        return target != null && isValidParasiteTarget(target) ? target : null;
    }

    private void setSeizerTarget(LivingEntity target) {
        seizerTarget = target == null ? null : target.getUUID();
        entityData.set(SEIZER_TARGET_ID, target == null ? 0 : target.getId());
        setSpecialAction(target == null ? SpecialAction.NONE : SpecialAction.SEIZER_HOLD);
    }

    private void clearSeizerTarget() {
        setSeizerTarget(null);
    }

    private void tickSentry() {
        breakBlocksTowardsTarget(7.0F, 3.0D);

        // 自动恢复到默认状态
        if (tickCount % 20 == 0) {
            int status = getSentryParasiteStatus();
            if (status == 1 || status == 2 || status == 9) {
                setSentryParasiteStatus(0);
            }
        }
    }

    private int getSentryParasiteStatus() {
        return entityData.get(SENTRY_PARASITE_STATUS);
    }

    private void setSentryParasiteStatus(int status) {
        entityData.set(SENTRY_PARASITE_STATUS, status);
    }

    private boolean getSentryStillAni() {
        return entityData.get(SENTRY_STILL_ANI);
    }

    private void setSentryStillAni(boolean stillAni) {
        entityData.set(SENTRY_STILL_ANI, stillAni);
    }

    private SpecialAction specialAction() {
        int value = entityData.get(SPECIAL_ACTION);
        SpecialAction[] actions = SpecialAction.values();
        return value >= 0 && value < actions.length ? actions[value] : SpecialAction.NONE;
    }

    private void setSpecialAction(SpecialAction action) {
        entityData.set(SPECIAL_ACTION, action.ordinal());
    }

    private LivingEntity findEntity(UUID id) {
        if (id == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    protected void hurtNearby(Entity center, double radius, float damage, boolean launch) {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), center.getBoundingBox().inflate(radius));
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            if (!target.hurt(damageSources().mobAttack(this), damage)) {
                continue;
            }
            if (launch) {
                Vec3 push = target.position().subtract(position());
                if (push.lengthSqr() > 0.001D) {
                    push = push.normalize().scale(0.65D);
                    target.push(push.x, 0.55D, push.z);
                }
            }
        }
    }

    private void fireSpine(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.SPINE, start, target.getEyePosition(),
                1.0D, 25.0F, 0.85D, 70, target);
        level().addFreshEntity(projectile);
    }

    void applySentrySpineEffects(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 0, false, false), this);
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = target.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()) {
                armor.hurtAndBreak(Math.max(1, armor.getMaxDamage() * 4 / 100), target, slot);
            }
        }
    }

    private void breakBlocksTowardsTarget(float maximumHardness, double range) {
        if (abilityCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        direction = direction.normalize();
        BlockPos origin = BlockPos.containing(getX() + direction.x * range, getY() + 1.0D,
                getZ() + direction.z * range);
        for (BlockPos position : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(position);
            float hardness = state.getDestroySpeed(level(), position);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > maximumHardness) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), position, this)) {
                abilityCooldown = 20;
            }
            return;
        }
    }

    private void summonWormMinions() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = wormMinimumPayload + random.nextInt(wormMaximumPayload - wormMinimumPayload + 1);
        for (int index = 0; index < count; index++) {
            Mob minion = createWormMinion(serverLevel);
            if (minion == null) {
                continue;
            }
            minion.moveTo(getX(), getY() + getBbHeight() + 0.5D, getZ(), getYRot(), 0.0F);
            minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(minion.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            minion.setTarget(getTarget());
            minion.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false), this);
            minion.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 15, false, false), this);
            minion.setDeltaMovement(wormLaunchVelocity());
            serverLevel.addFreshEntity(minion);
        }
    }

    private Mob createWormMinion(ServerLevel level) {
        if (wormPayloadTypes.isEmpty()) {
            return switch (random.nextInt(3)) {
                case 0 -> ModEntities.PRI_ARACHNIDA.get().create(level);
                case 1 -> ModEntities.PRI_REEKER.get().create(level);
                default -> ModEntities.PRI_LONGARMS.get().create(level);
            };
        }
        ResourceLocation id = ResourceLocation.tryParse(wormPayloadTypes.get(random.nextInt(wormPayloadTypes.size())));
        if (id == null) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        return type != null && type.create(level) instanceof Mob mob ? mob : null;
    }

    private Vec3 wormLaunchVelocity() {
        double x = random.nextFloat();
        double y = random.nextFloat();
        double z = random.nextFloat();
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-6D) {
            return new Vec3(0.0D, 0.6D, 0.0D);
        }
        double scale = 0.5D / (length / 4.0D + 0.1D);
        scale *= random.nextFloat() * random.nextFloat() + 0.3F;
        x = Math.min(x / length * scale, 0.03D) * (random.nextDouble() * 2.0D - 1.0D);
        y = Math.min(y / length * scale * 6.0D, 1.2D);
        z = Math.min(z / length * scale, 0.03D) * (random.nextDouble() * 2.0D - 1.0D);
        return new Vec3(x, y, z);
    }

    private final class KyphosisWaveGoal extends Goal {
        private int chargeTicks;

        private KyphosisWaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && target != null && hasLineOfSight(target)
                    && distanceToSqr(target) <= 256.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 60 && getTarget() != null;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
            setSpecialAction(SpecialAction.KYPHOSIS_WAVE);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (++chargeTicks == 20 || chargeTicks == 40) {
                Vec3 direction = target.position().subtract(position());
                if (direction.lengthSqr() > 0.001D) {
                    direction = direction.normalize();
                    for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                            getBoundingBox().expandTowards(direction.scale(12.0D)).inflate(1.25D),
                            DeterrentParasiteEntity.this::isValidParasiteTarget)) {
                        if (hasLineOfSight(victim)) {
                            victim.hurt(damageSources().mobAttack(DeterrentParasiteEntity.this), 12.0F);
                            break;
                        }
                    }
                }
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.5D, getZ(), 0.0D, 0.05D, 0.0D);
            }
        }

        @Override
        public void stop() {
            abilityCooldown = 180;
            setSpecialAction(SpecialAction.NONE);
        }
    }

    private final class SeizerHoldGoal extends Goal {
        private SeizerHoldGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return seizerTarget == null && target != null && hasLineOfSight(target)
                    && distanceToSqr(target) <= 25.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                setSeizerTarget(target);
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }

    private final class SentrySpineGoal extends Goal {
        private SentrySpineGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && target != null && hasLineOfSight(target)
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 576.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);

            // 根据目标距离和血量决定攻击状态
            double distanceSqr = distanceToSqr(target);
            float healthPercent = getHealth() / getMaxHealth();

            if (healthPercent < 0.3F) {
                // 低血量时使用快速攻击（状态2）
                setSentryParasiteStatus(2);
            } else if (distanceSqr < 64.0D) {
                // 近距离使用攻击状态1
                setSentryParasiteStatus(1);
            } else {
                // 远距离保持默认状态
                setSentryParasiteStatus(0);
            }

            setSentryStillAni(false);

            fireSpine(target);
            fireSpine(target);
            fireSpine(target);
            triggerAnim("attack_controller", "attack");
            abilityCooldown = 60;
        }
    }

    private final class WormEruptionGoal extends Goal {
        private int chargeTicks;

        private WormEruptionGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return abilityCooldown <= 0 && tickCount >= 100;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 80;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
            setSpecialAction(SpecialAction.WORM_ERUPTION);
        }

        @Override
        public void tick() {
            if (++chargeTicks == 40) {
                summonWormMinions();
            }
        }

        @Override
        public void stop() {
            discard();
        }
    }

    private final class StationaryMeleeGoal extends Goal {
        private final double reachSqr;
        private int cooldown;

        private StationaryMeleeGoal(double reach) {
            reachSqr = reach * reach;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) <= reachSqr;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            doHurtTarget(target);
            cooldown = 20;
        }
    }

    public enum Kind {
        DISPATCHER_TENTACLE(10.0D, 10.0D, 2.0D, 0.0D, 16.0D, 0),
        KYPHOSIS(50.0D, 15.0D, 15.0D, 0.0D, 20.0D, 36),
        SEIZER(15.0D, 10.0D, 6.0D, 0.0D, 6.0D, 0),
        SENTRY(30.0D, 10.0D, 5.0D, 0.0D, 32.0D, 36),
        WORM(10.0D, 10.0D, 0.0D, 0.0D, 16.0D, 0);

        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double movementSpeed;
        private final double followRange;
        private final int experience;

        Kind(double maxHealth, double armor, double attackDamage, double movementSpeed,
             double followRange, int experience) {
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.followRange = followRange;
            this.experience = experience;
        }
    }

    private enum SpecialAction {
        NONE,
        KYPHOSIS_WAVE,
        WORM_ERUPTION,
        SEIZER_HOLD,
        DISPATCHER_DEPLOY
    }
}
