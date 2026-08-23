package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.effect.EffectStacking;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModFluidTypes;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Dead Blood contact effects: rapid chip damage with Viral and Corrosion for
 * non-parasites, direct healing for parasites.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DeadBloodFluidEvents {
    private DeadBloodFluidEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide
                || !entity.isInFluidType(ModFluidTypes.DEAD_BLOOD.get())) {
            return;
        }
        long time = entity.level().getGameTime();
        if (entity instanceof Parasite) {
            if (time % 10L == 0L) {
                entity.heal(1.0F);
            }
            return;
        }
        if (time % 4L == 0L) {
            entity.hurt(entity.damageSources().magic(), 0.5F);
        }
        if (time % 20L == 0L) {
            EffectStacking.apply(entity, ModMobEffects.VIRAL.get(), 100, 0);
            EffectStacking.apply(entity, ModMobEffects.CORROSION.get(), 100, 0);
        }
    }
}
