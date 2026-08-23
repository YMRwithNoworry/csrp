package alku.csrp.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Legacy Indeaf status suppresses horizontal movement. */
public final class IndeafMobEffect extends MarkerMobEffect {
    public IndeafMobEffect() {
        super(true, 16768256);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            var velocity = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0D, velocity.y, 0.0D);
            if (entity instanceof Player player) {
                player.setSprinting(false);
            }
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
