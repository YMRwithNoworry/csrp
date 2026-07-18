package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;

/** Common hostile and fire-vulnerable state for legacy Marauderized infected forms. */
public abstract class MarauderizedParasiteEntity extends HijackedParasiteEntity {
    private static final float BLEED_CHANCE = 0.15F;

    protected MarauderizedParasiteEntity(EntityType<? extends MarauderizedParasiteEntity> type, Level level,
                                         int experience) {
        super(type, level, experience);
    }

    protected static AttributeSupplier.Builder createMarauderizedAttributes(double health, double armor,
                                                                              double damage,
                                                                              double knockbackResistance,
                                                                              double movementSpeed,
                                                                              double followRange) {
        return createAttributes(health, armor, damage, knockbackResistance, movementSpeed, followRange);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new MeleeAttackGoal(this, meleeSpeed(), false));
    }

    protected double meleeSpeed() {
        return 1.2D;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof LivingEntity target && random.nextFloat() < BLEED_CHANCE) {
            target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
        }
        return hit;
    }
}
