package alku.csrp.effect;

import net.minecraft.world.entity.LivingEntity;

/** Marks its victim with the legacy glow while client hooks distort the GUI. */
public final class DistortedEnlightenmentMobEffect extends MarkerMobEffect {
    private static final String OWNED_GLOW_TAG = "csrp_distorted_owned_glow";

    public DistortedEnlightenmentMobEffect() {
        super(true, 8970751);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && !entity.isCurrentlyGlowing()) {
            entity.getPersistentData().putBoolean(OWNED_GLOW_TAG, true);
            entity.setGlowingTag(true);
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public static void clearOwnedGlow(LivingEntity entity) {
        if (entity.getPersistentData().getBoolean(OWNED_GLOW_TAG)) {
            entity.setGlowingTag(false);
            entity.getPersistentData().remove(OWNED_GLOW_TAG);
        }
    }
}
