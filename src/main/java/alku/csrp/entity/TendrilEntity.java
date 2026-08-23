package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Detached body tendril used by the original multipart parasites. */
public final class TendrilEntity extends Monster implements GeoEntity, Parasite {
    public static final int SHYCO = 1;
    public static final int NOGLA = 2;
    public static final int CANRA = 3;
    public static final int BANO = 4;
    public static final int ESOR = 5;
    public static final int ANGED = 6;
    public static final int DRAGON_LEFT_WING = 7;
    public static final int DRAGON_RIGHT_WING = 8;

    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(
            TendrilEntity.class, EntityDataSerializers.INT);
    private final RawAnimation idleAnimation = ParasiteAnimations.loop(this, "idle");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public TendrilEntity(EntityType<? extends TendrilEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        // The original removes its wander and parasite-follow goals.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SKIN, SHYCO);
        // Legacy builder.define(SKIN, SHYCO); parasitetype stores the synchronized skin.
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    public void setSkin(int skin) {
        entityData.set(SKIN, Math.max(SHYCO, Math.min(DRAGON_RIGHT_WING, skin)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasitetype", getSkin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("parasitetype")) {
            setSkin(tag.getInt("parasitetype"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 0,
                state -> state.setAndContinue(idleAnimation)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
