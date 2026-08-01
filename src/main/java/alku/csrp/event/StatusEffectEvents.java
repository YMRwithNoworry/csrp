package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Runtime hooks for SRP effects whose behavior crosses entity boundaries. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class StatusEffectEvents {
    private StatusEffectEvents() {
    }

    @SubscribeEvent
    public static void distortedEnlightenmentDamage(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        if (attacker == null) {
            return;
        }
        boolean victimDistorted = victim.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT);
        boolean attackerDistorted = attacker instanceof net.minecraft.world.entity.LivingEntity living
                && living.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT);
        if (victimDistorted && attacker instanceof Parasite) {
            event.setAmount(event.getAmount() * (attacker instanceof DraconiteEntity ? 5.0F : 0.8F));
        }
        if (attackerDistorted && victim instanceof Parasite) {
            event.setAmount(event.getAmount() * 0.8F);
        }
        if (attacker instanceof LivingEntity living) {
            var muscleOut = living.getEffect(ModMobEffects.MUSCLEOUT);
            if (muscleOut != null) {
                event.setAmount(event.getAmount() * 0.09F * (muscleOut.getAmplifier() + 1));
            }
        }
    }

    @SubscribeEvent
    public static void suppressDebarDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Parasite && event.getEntity().hasEffect(ModMobEffects.DEBAR)) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void applyTheSign(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !hasSignCharm(player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(ModMobEffects.THE_SIGN, 40, 0, false, false));
    }

    @SubscribeEvent
    public static void preventSignTargeting(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Parasite
                && event.getNewAboutToBeSetTarget() instanceof Player player
                && player.hasEffect(ModMobEffects.THE_SIGN)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    private static boolean hasSignCharm(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.THE_SIGN_CHARM)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void restoreGravity(MobEffectEvent.Remove event) {
        if (event.getCure() != null
                && Csrp.MODID.equals(BuiltInRegistries.MOB_EFFECT.getKey(event.getEffect().value()).getNamespace())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEffect().value() == ModMobEffects.DISTORTED_ENLIGHTENMENT.get()) {
            event.getEntity().setNoGravity(false);
        }
    }

    @SubscribeEvent
    public static void restoreGravityOnExpiry(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().is(ModMobEffects.DISTORTED_ENLIGHTENMENT)) {
            event.getEntity().setNoGravity(false);
        }
    }
}
