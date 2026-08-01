package alku.csrp.effect;

import alku.csrp.Config;
import alku.csrp.entity.ParasiticScentEntity;
import alku.csrp.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Periodically deploys a target-following Scent, matching the original Prey effect. */
public final class PreyMobEffect extends MobEffect {
    public PreyMobEffect() {
        super(MobEffectCategory.HARMFUL, 4800055);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level) || entity.tickCount % 40 != 0) {
            return true;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        if (level.getEntities(ModEntities.SCENT.get(), scent -> true).size() >= ParasiticScentEntity.SCENT_CAP) {
            return true;
        }
        for (ParasiticScentEntity scent : level.getEntitiesOfClass(ParasiticScentEntity.class,
                entity.getBoundingBox().inflate(64.0D))) {
            if (scent.getTargetToKill() == entity && scent.getCanFollow()) {
                return true;
            }
        }

        ParasiticScentEntity scent = ModEntities.SCENT.get().create(level);
        if (scent == null) {
            return true;
        }
        scent.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        scent.setScentState(1);
        scent.setTargetToKill(entity, false);
        scent.setScentLife(ParasiticScentEntity.OBSERVER_LIFE_TICKS);
        int phase = Config.evolutionPhase(level);
        scent.increaseDanger(ParasiticScentEntity.scentBonus(phase), true);
        scent.setScentReaction(ParasiticScentEntity.scentReaction(phase), false);
        scent.setCanFollow(true);
        level.addFreshEntity(scent);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
