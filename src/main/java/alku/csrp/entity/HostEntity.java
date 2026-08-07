package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class HostEntity extends AbstractHostEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return true;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        return 0.95F;
    }
    public static final int BURROW_DURATION_TICKS = 80;
    private static final float MAX_BURIED_TIMER = 4.8F;
    private static final float BURIED_TIMER_STEP = 0.08F;
    private static final EntityDataAccessor<Float> BURIED_TIMER =
            SynchedEntityData.defineId(HostEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> MOUTH_OPEN =
            SynchedEntityData.defineId(HostEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int HOST_TO_HOSTII_KILLS = 40;
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation BURROW = ParasiteAnimations.loop(this, "get_burrow_timer");
    private final RawAnimation OPEN_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_open_1");
    private final RawAnimation OPEN_ATTACK = ParasiteAnimations.play(this,
            "get_attack_timer.get_open_1");
    private final RawAnimation OPEN_BURROW = ParasiteAnimations.loop(this,
            "get_burrow_timer.get_open_1");
    private final RawAnimation BURROWED = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_burrowed_1");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "get_attack_timer");
    private final RawAnimation BURROWED_ATTACK =
            ParasiteAnimations.play(this, "get_attack_timer.get_burrowed_1");
    private final RawAnimation BURROWED_BURROW = ParasiteAnimations.loop(this,
            "get_burrow_timer.get_burrowed_1");
    private final RawAnimation BURROWED_OPEN_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_burrowed_1.get_open_1");
    private final RawAnimation BURROWED_OPEN_ATTACK = ParasiteAnimations.play(this,
            "get_attack_timer.get_burrowed_1.get_open_1");
    private final RawAnimation BURROWED_OPEN_BURROW = ParasiteAnimations.loop(this,
            "get_burrow_timer.get_burrowed_1.get_open_1");

    public HostEntity(EntityType<? extends HostEntity> type, Level level) {
        super(type, level, 0.12, 3.0, 4.0, BURROW_DURATION_TICKS, 50, 40);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createHostAttributes(50.0, 7.0, 10.0, 0.12, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURIED_TIMER, MAX_BURIED_TIMER);
        builder.define(MOUTH_OPEN, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        float timer = getBuriedTimer();
        timer = isBurrowed() ? Math.max(0.0F, timer - BURIED_TIMER_STEP)
                : Math.min(MAX_BURIED_TIMER, timer + BURIED_TIMER_STEP);
        entityData.set(BURIED_TIMER, timer);
        entityData.set(MOUTH_OPEN, isBurrowed() && timer <= 0.0F);
    }

    @Override
    protected void performRangedAttack(LivingEntity target) {
        performBombAttack(target);
    }

    private void performBombAttack(LivingEntity target) {
        spawnProjectile(ParasiteProjectileEntity.Mode.BOMB, target, 0.75, 12.0F, 4.0, 80);
    }

    @Override
    protected void performShockwave() {
        super.performShockwave();
    }

    @Override
    protected void summonMinions() {
        summonRupters();
    }

    @Override
    protected void triggerAttackAnimation() {
        String trigger = isBurrowed()
                ? isMouthOpen() ? "burrowed_open_attack" : "burrowed_attack"
                : isMouthOpen() ? "open_attack" : "attack";
        triggerAnim("attack_controller", trigger);
    }

    private float getBuriedTimer() {
        return entityData.get(BURIED_TIMER);
    }

    private boolean isMouthOpen() {
        return entityData.get(MOUTH_OPEN);
    }

    private boolean isBurrowTransitioning() {
        float timer = getBuriedTimer();
        return timer > 0.0F && timer < MAX_BURIED_TIMER;
    }

    private void summonRupters() {
        spawnMinions(ModEntities.RUPTER, RupterEntity.class, 4);
    }

    @Override
    protected void onParasiteKill(ServerLevel level, LivingEntity victim, int kills) {
        if (kills <= HOST_TO_HOSTII_KILLS) {
            return;
        }
        HostIIEntity hostII = ModEntities.HOSTII.get().create(level);
        if (hostII == null) {
            return;
        }
        hostII.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        level.addFreshEntity(hostII);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("buried_timer", getBuriedTimer());
        tag.putBoolean("mouth_open", isMouthOpen());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(BURIED_TIMER, tag.contains("buried_timer")
                ? tag.getFloat("buried_timer") : MAX_BURIED_TIMER);
        entityData.set(MOUTH_OPEN, tag.getBoolean("mouth_open"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> {
                    if (isBurrowTransitioning()) {
                        if (isBurrowed()) {
                            return state.setAndContinue(isMouthOpen()
                                    ? BURROWED_OPEN_BURROW : BURROWED_BURROW);
                        }
                        return state.setAndContinue(isMouthOpen() ? OPEN_BURROW : BURROW);
                    }
                    if (isBurrowed()) {
                        return state.setAndContinue(isMouthOpen() ? BURROWED_OPEN_IDLE : BURROWED);
                    }
                    if (isMouthOpen()) {
                        return state.setAndContinue(OPEN_IDLE);
                    }
                    return state.setAndContinue(AGE_IN_TICKS);
                }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("open_attack", OPEN_ATTACK)
                .triggerableAnim("burrowed_attack", BURROWED_ATTACK)
                .triggerableAnim("burrowed_open_attack", BURROWED_OPEN_ATTACK));
    }
}
