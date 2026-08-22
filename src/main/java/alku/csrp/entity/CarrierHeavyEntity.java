package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

public final class CarrierHeavyEntity extends CarrierEntity {
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");

    public CarrierHeavyEntity(EntityType<? extends CarrierHeavyEntity> type, Level level) {
        super(type, level, 70, 7.0, 2, 1200, 600);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ARMOR, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 25.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected RawAnimation ageAnimation() {
        return AGE_IN_TICKS;
    }

    @Override
    protected RawAnimation limbSwingAnimation() {
        return LIMB_SWING;
    }

    @Override
    protected boolean griefingEnabled() {
        return MobsConfig.carrierHeavyGriefing();
    }

    @Override
    protected List<? extends String> spawnTable() {
        return MobsConfig.carrierHeavyMobTable();
    }

    @Override
    protected void onVariantActivated() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.3D);
        }
    }

    @Override
    protected SoundEvent explosionSound() {
        return ModSounds.RATHOL_BOOM.get();
    }
}
