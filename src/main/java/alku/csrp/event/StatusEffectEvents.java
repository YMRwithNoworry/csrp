package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.effect.DistortedEnlightenmentMobEffect;
import alku.csrp.entity.AbominationEntity;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import alku.csrp.world.SrpCoreSystems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** Runtime hooks for SRP effects whose behavior crosses entity boundaries. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class StatusEffectEvents {
    private static final int MAX_EFFECT_AMPLIFIER = 254;
    private static final ThreadLocal<Boolean> TRANSFERRING_PIVOT_DAMAGE =
            ThreadLocal.withInitial(() -> false);
    /** Guards the Needler merge because forceAddEffect may synchronously fire Added again. */
    private static final ThreadLocal<Boolean> STACKING_NEEDLER =
            ThreadLocal.withInitial(() -> false);
    private static final Map<LivingEntity, LivingEntity> ROOTER_OWNERS = new WeakHashMap<>();

    private StatusEffectEvents() {
    }

    @SubscribeEvent
    public static void stackNeedlerPotency(MobEffectEvent.Added event) {
        MobEffectInstance incoming = event.getEffectInstance();
        MobEffectInstance current = event.getOldEffectInstance();
        if (current == null || incoming.getEffect() != ModMobEffects.NEEDLER.get()
                || STACKING_NEEDLER.get()) {
            return;
        }
        int amplifier = Math.min(MAX_EFFECT_AMPLIFIER,
                current.getAmplifier() + incoming.getAmplifier() + 1);
        int duration = Math.max(current.getDuration(), incoming.getDuration());
        MobEffectInstance stacked = new MobEffectInstance(ModMobEffects.NEEDLER.get(), duration, amplifier,
                current.isAmbient() && incoming.isAmbient(),
                current.isVisible() || incoming.isVisible(),
                current.showIcon() || incoming.showIcon());
        // Added fires before vanilla merges the incoming instance; replacing the map entry here
        // leaves that later merge attached only to the displaced instance.
        STACKING_NEEDLER.set(true);
        try {
            event.getEntity().forceAddEffect(stacked, event.getEffectSource());
        } finally {
            STACKING_NEEDLER.set(false);
        }
    }

    @SubscribeEvent
    public static void modifyEffectDamage(LivingDamageEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        boolean victimDistorted = victim.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT.get());
        boolean attackerDistorted = attacker instanceof net.minecraft.world.entity.LivingEntity living
                && living.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT.get());
        if (victimDistorted && attacker instanceof Parasite) {
            event.setAmount(event.getAmount() * (attacker instanceof DraconiteEntity ? 5.0F : 0.8F));
        }
        if (attackerDistorted && victim instanceof Parasite) {
            event.setAmount(event.getAmount() * 0.8F);
        }
        var fear = victim.getEffect(ModMobEffects.FEAR.get());
        if (fear != null && (!victim.onGround() || event.getSource().is(DamageTypes.FALL))) {
            event.setAmount(event.getAmount() * 1.5F * (fear.getAmplifier() + 1));
        }
        var overheating = victim.getEffect(ModMobEffects.OVERHEATING.get());
        if (overheating != null && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * (overheating.getAmplifier() + 2));
        }
        if (attacker instanceof LivingEntity living) {
            var muscleOut = living.getEffect(ModMobEffects.MUSCLEOUT.get());
            if (muscleOut != null) {
                event.setAmount(event.getAmount() * 0.09F * (muscleOut.getAmplifier() + 1));
            }
        }
        transferPivotDamage(event);
    }

    private static void transferPivotDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        var pivot = victim.getEffect(ModMobEffects.PIVOT.get());
        if (!(victim instanceof Parasite) || pivot == null || TRANSFERRING_PIVOT_DAMAGE.get()) {
            return;
        }
        LivingEntity rooter = ROOTER_OWNERS.get(victim);
        if (rooter == null) {
            return;
        }
        if (!rooter.isAlive()) {
            ROOTER_OWNERS.remove(victim);
            victim.removeEffect(ModMobEffects.PIVOT.get());
            return;
        }
        float transferred = event.getAmount() * (pivot.getAmplifier() + 1) * 0.2375F;
        event.setAmount(event.getAmount() * 0.05F);
        TRANSFERRING_PIVOT_DAMAGE.set(true);
        try {
            rooter.hurt(event.getSource(), transferred);
        } finally {
            TRANSFERRING_PIVOT_DAMAGE.remove();
        }
    }

    public static void linkToRooter(LivingEntity parasite, LivingEntity rooter) {
        if (parasite instanceof Parasite && rooter != parasite) {
            ROOTER_OWNERS.put(parasite, rooter);
        }
    }

    @SubscribeEvent
    public static void preventRooterPivot(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.PIVOT.get() && isRooter(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DENY);
        }
    }

    private static boolean isRooter(LivingEntity entity) {
        if (entity instanceof AbominationEntity abomination) {
            return abomination.getKind() == AbominationEntity.Kind.BODIES;
        }
        return entity instanceof NexusParasiteEntity nexus && switch (nexus.getKind()) {
            case ROOTER_SI, ROOTER_SII, ROOTER_SIII, ROOTER_SIV, ROOTERBALL -> true;
            default -> false;
        };
    }

    @SubscribeEvent
    public static void suppressDebarDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Parasite && event.getEntity().hasEffect(ModMobEffects.DEBAR.get())) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void applyTheSign(PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        Player player = event.player;
        if (player.level().isClientSide || !hasSignCharm(player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(ModMobEffects.THE_SIGN.get(), 40, 0, false, false));
    }

    @SubscribeEvent
    public static void applyParasiteBiomeBlindness(PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0
                || !SrpCoreSystems.isInsideParasiteBiome(level, player.blockPosition())) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));
    }

    @SubscribeEvent
    public static void preventSignTargeting(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Parasite
                && event.getNewTarget() instanceof Player player
                && player.hasEffect(ModMobEffects.THE_SIGN.get())) {
            event.setNewTarget(null);
        }
    }

    private static boolean hasSignCharm(Player player) {
        return player.getMainHandItem().is(ModItems.THE_SIGN_CHARM.get())
                || player.getOffhandItem().is(ModItems.THE_SIGN_CHARM.get());
    }

    @SubscribeEvent
    public static void handleEffectRemoval(MobEffectEvent.Remove event) {
        if (Csrp.MODID.equals(BuiltInRegistries.MOB_EFFECT.getKey(event.getEffect()).getNamespace())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEffect() == ModMobEffects.DISTORTED_ENLIGHTENMENT.get()) {
            DistortedEnlightenmentMobEffect.clearOwnedGlow(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void clearGlowOnExpiry(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect() == ModMobEffects.DISTORTED_ENLIGHTENMENT.get()) {
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
            target.addEffect(new MobEffectInstance(ModMobEffects.CAMOUFLAGE.get(),
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
        var parate = attacker.getEffect(ModMobEffects.PARATE.get());
        if (parate == null) {
            return;
        }
        double multiplier = 0.5D * (parate.getAmplifier() + 1);
        stealBaseAttribute(attacker, event.getEntity(), Attributes.MAX_HEALTH, multiplier);
        stealBaseAttribute(attacker, event.getEntity(), Attributes.ARMOR, multiplier);
        stealBaseAttribute(attacker, event.getEntity(), Attributes.ATTACK_DAMAGE, multiplier);
    }

    private static void stealBaseAttribute(LivingEntity attacker, LivingEntity victim,
            Attribute attribute, double multiplier) {
        var own = attacker.getAttribute(attribute);
        var prey = victim.getAttribute(attribute);
        if (own != null && prey != null) {
            own.setBaseValue(own.getBaseValue() + prey.getBaseValue() * multiplier);
        }
    }
}
