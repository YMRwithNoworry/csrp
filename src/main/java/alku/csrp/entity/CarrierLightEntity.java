package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public final class CarrierLightEntity extends CarrierEntity {
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");

    public CarrierLightEntity(EntityType<? extends CarrierLightEntity> type, Level level) {
        super(type, level, 70, 7.0, 2, 1200, 500);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ARMOR, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 25.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static boolean checkCarrierLightSpawnRules(EntityType<? extends Monster> type,
                                                       ServerLevelAccessor level, MobSpawnType spawnType,
                                                       BlockPos pos, RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 1 && phase <= 4
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
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
        return MobsConfig.carrierLightGriefing();
    }

    @Override
    protected List<? extends String> spawnTable() {
        return MobsConfig.carrierLightMobTable();
    }

    @Override
    protected float cloudRadiusMultiplier() {
        return 2.5F;
    }

    @Override
    protected int cloudWaitTime() {
        return isVariant() ? 4 : 10;
    }

    @Override
    protected SoundEvent explosionSound() {
        return ModSounds.RATHOL_BOOM.get();
    }
}
