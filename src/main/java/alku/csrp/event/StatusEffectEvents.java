package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.effect.DistortedEnlightenmentMobEffect;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Comparator;

/** Runtime hooks for SRP effects whose behavior crosses entity boundaries. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class StatusEffectEvents {
    private static final ThreadLocal<Boolean> TRANSFERRING_PIVOT_DAMAGE =
            ThreadLocal.withInitial(() -> false);

    private StatusEffectEvents() {
    }

    @SubscribeEvent
    public static void modifyEffectDamage(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        boolean victimDistorted = victim.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT);
        boolean attackerDistorted = attacker instanceof net.minecraft.world.entity.LivingEntity living
                && living.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT);
        if (victimDistorted && attacker instanceof Parasite) {
            event.setAmount(event.getAmount() * (attacker instanceof DraconiteEntity ? 5.0F : 0.8F));
        }
        if (attackerDistorted && victim instanceof Parasite) {
            event.setAmount(event.getAmount() * 0.8F);
        }
        var fear = victim.getEffect(ModMobEffects.FEAR);
        if (fear != null && (!victim.onGround() || event.getSource().is(DamageTypes.FALL))) {
            event.setAmount(event.getAmount() * 1.5F * (fear.getAmplifier() + 1));
        }
        var overheating = victim.getEffect(ModMobEffects.OVERHEATING);
        if (overheating != null && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * (overheating.getAmplifier() + 2));
        }
        if (attacker instanceof LivingEntity living) {
            var muscleOut = living.getEffect(ModMobEffects.MUSCLEOUT);
            if (muscleOut != null) {
                event.setAmount(event.getAmount() * 0.09F * (muscleOut.getAmplifier() + 1));
            }
        }
        transferPivotDamage(event);
    }

    private static void transferPivotDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        var pivot = victim.getEffect(ModMobEffects.PIVOT);
        if (!(victim instanceof Parasite) || pivot == null || TRANSFERRING_PIVOT_DAMAGE.get()) {
            return;
        }
        NexusParasiteEntity rooter = victim.level().getEntitiesOfClass(NexusParasiteEntity.class,
                        victim.getBoundingBox().inflate(32.0D), StatusEffectEvents::isRooter)
                .stream().min(Comparator.comparingDouble(root -> root.distanceToSqr(victim))).orElse(null);
        if (rooter == null) {
            return;
        }
        float ratio = Math.min(0.95F, 0.2375F + 0.05F * pivot.getAmplifier());
        float transferred = event.getAmount() * ratio;
        event.setAmount(Math.max(0.0F, event.getAmount() - transferred));
        TRANSFERRING_PIVOT_DAMAGE.set(true);
        try {
            rooter.hurt(event.getSource(), transferred);
        } finally {
            TRANSFERRING_PIVOT_DAMAGE.remove();
        }
    }

    private static boolean isRooter(NexusParasiteEntity entity) {
        return entity.isAlive() && switch (entity.getKind()) {
            case ROOTER_SI, ROOTER_SII, ROOTER_SIII, ROOTER_SIV, ROOTERBALL -> true;
            default -> false;
        };
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
        return player.getMainHandItem().is(ModItems.THE_SIGN_CHARM)
                || player.getOffhandItem().is(ModItems.THE_SIGN_CHARM);
    }

    @SubscribeEvent
    public static void handleEffectRemoval(MobEffectEvent.Remove event) {
        if (event.getCure() != null
                && Csrp.MODID.equals(BuiltInRegistries.MOB_EFFECT.getKey(event.getEffect().value()).getNamespace())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEffect().value() == ModMobEffects.DISTORTED_ENLIGHTENMENT.get()) {
            DistortedEnlightenmentMobEffect.clearOwnedGlow(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void clearGlowOnExpiry(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().is(ModMobEffects.DISTORTED_ENLIGHTENMENT)) {
            DistortedEnlightenmentMobEffect.clearOwnedGlow(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void feedCamouflage(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        var stack = event.getEntity().getItemInHand(event.getHand());
        int duration;
        if (stack.is(Items.GOLDEN_APPLE)) {
            duration = 6_000;
        } else if (stack.is(Items.GOLDEN_CARROT)) {
            duration = 3_000;
        } else {
            return;
        }
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            target.addEffect(new MobEffectInstance(ModMobEffects.CAMOUFLAGE,
                    duration, 0, false, true), player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void absorbParateAttributes(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !(attacker instanceof Parasite)) {
            return;
        }
        var parate = attacker.getEffect(ModMobEffects.PARATE);
        if (parate == null) {
            return;
        }
        double multiplier = 0.5D * (parate.getAmplifier() + 1);
        stealBaseAttribute(attacker, event.getEntity(), Attributes.MAX_HEALTH, multiplier);
        stealBaseAttribute(attacker, event.getEntity(), Attributes.ARMOR, multiplier);
        stealBaseAttribute(attacker, event.getEntity(), Attributes.ATTACK_DAMAGE, multiplier);
    }

    private static void stealBaseAttribute(LivingEntity attacker, LivingEntity victim,
            Holder<Attribute> attribute, double multiplier) {
        var own = attacker.getAttribute(attribute);
        var prey = victim.getAttribute(attribute);
        if (own != null && prey != null) {
            own.setBaseValue(own.getBaseValue() + prey.getBaseValue() * multiplier);
        }
    }
}
