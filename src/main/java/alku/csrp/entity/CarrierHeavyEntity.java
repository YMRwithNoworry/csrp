package alku.csrp.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.RawAnimation;

public final class CarrierHeavyEntity extends CarrierEntity {
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");

    public CarrierHeavyEntity(EntityType<? extends CarrierHeavyEntity> type, Level level) {
        super(type, level, 70, 6, 7.0, 1, 1200, 600);
        xpReward = 18;
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
    protected int gnatSpawnCount() {
        return 6;
    }
}
