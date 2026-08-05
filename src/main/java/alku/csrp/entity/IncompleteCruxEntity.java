package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/** Legacy CruxB growth form that matures into a full Crux after a random 20-60 second interval. */
public final class IncompleteCruxEntity extends CrudeParasiteEntity {
    private static final int MIN_GROW_TICKS = 20 * 20;
    private static final int MAX_GROW_TICKS = 60 * 20;
    private static final int BURST_TICKS = 70;
    private final RawAnimation ANIMATION = ParasiteAnimations.loop(this, "idle");

    private int growthDuration;
    private int growthTicks;
    private int burstTicks = -1;

    public IncompleteCruxEntity(EntityType<? extends IncompleteCruxEntity> type, Level level) {
        super(type, level);
        growthDuration = MIN_GROW_TICKS + random.nextInt(MAX_GROW_TICKS - MIN_GROW_TICKS + 1);
        xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 21.0).add(Attributes.ARMOR, 3.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }

        if (burstTicks >= 0) {
            getNavigation().stop();
            if (++burstTicks >= BURST_TICKS) {
                transformIntoCrux();
            }
            return;
        }

        growthTicks++;
        if (growthTicks > growthDuration) {
            burstTicks = 0;
            return;
        }
        if (getHealth() < getMaxHealth()) {
            setHealth(Math.min(getMaxHealth(), getHealth() + 0.007F));
        }
    }

    public float getGrowthProgress() {
        return Math.min(1.0F, growthTicks / (float) growthDuration);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("crux_growth_duration", growthDuration);
        tag.putInt("crux_growth_ticks", growthTicks);
        tag.putInt("crux_burst_ticks", burstTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("crux_growth_duration", Tag.TAG_INT)) {
            growthDuration = Math.max(1, tag.getInt("crux_growth_duration"));
        }
        if (tag.contains("crux_growth_ticks", Tag.TAG_INT)) {
            growthTicks = Math.max(0, tag.getInt("crux_growth_ticks"));
        }
        if (tag.contains("crux_burst_ticks", Tag.TAG_INT)) {
            burstTicks = tag.getInt("crux_burst_ticks");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ANIMATION)));
    }

    private void transformIntoCrux() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        CruxEntity adult = ModEntities.CRUX.get().create(serverLevel);
        if (adult != null) {
            adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                adult.setTarget(target);
            }
            serverLevel.addFreshEntity(adult);
        }
        discard();
    }
}
