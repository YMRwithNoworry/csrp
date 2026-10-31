package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.effect.DistortedEnlightenmentMobEffect;
import alku.csrp.entity.AbominationEntity;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/** Runtime hooks for SRP effects whose behavior crosses entity boundaries. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class StatusEffectEvents {
    private static final int MAX_EFFECT_AMPLIFIER = 254;
    private static final ThreadLocal<Boolean> TRANSFERRING_PIVOT_DAMAGE =
            ThreadLocal.withInitial(() -> false);
    private static final String ROOTER_OWNER_UUID = "csrp_rooter_owner_uuid";
    private static final String ROOTER_OWNER_DIMENSION = "csrp_rooter_owner_dimension";

    private StatusEffectEvents() {
    }

    @SubscribeEvent
    public static void stackNeedlerPotency(MobEffectEvent.Added event) {
        MobEffectInstance incoming = event.getEffectInstance();
        MobEffectInstance current = event.getOldEffectInstance();
        if (current == null || !incoming.is(ModMobEffects.NEEDLER)) {
            return;
        }
        int amplifier = Math.min(MAX_EFFECT_AMPLIFIER,
                current.getAmplifier() + incoming.getAmplifier() + 1);
        int duration = Math.max(current.getDuration(), incoming.getDuration());
        MobEffectInstance stacked = new MobEffectInstance(ModMobEffects.NEEDLER, duration, amplifier,
                current.isAmbient() && incoming.isAmbient(),
                current.isVisible() || incoming.isVisible(),
                current.showIcon() || incoming.showIcon());
        // Added fires before vanilla merges the incoming instance; replacing the map entry here
        // leaves that later merge attached only to the displaced instance.
        event.getEntity().forceAddEffect(stacked, event.getEffectSource());
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
        // Original SRPEventHandlerBus#mobFear (out109 1050-1088).
        var fear = victim.getEffect(ModMobEffects.FEAR);
        if (Config.fearActive() && fear != null) {
            int fearAmp = fear.getAmplifier() + 1;
            if (event.getSource().is(DamageTypes.FALL) && Config.fearFallDamage() != 0.0F) {
                event.setAmount(event.getAmount() * (Config.fearFallDamage() * fearAmp));
            }
            if (Config.fearAirDamage() != 0.0F) {
                if (Config.fearUnfair()) {
                    if (!victim.onGround()) {
                        event.setAmount(event.getAmount() * (Config.fearAirDamage() * fearAmp));
                    }
                } else {
                    boolean inFluidOrSupport = victim.isInWater() || victim.isInLava()
                            || victim.onClimbable() || victim.isPassenger();
                    if (!inFluidOrSupport) {
                        boolean creativeFlying = victim instanceof Player player && player.getAbilities().flying;
                        boolean elytraFlying = victim.isFallFlying();
                        boolean descending = victim.getDeltaMovement().y < 0.0D;
                        boolean trueAirborne = creativeFlying || elytraFlying
                                || (!victim.onGround() && descending && victim.fallDistance >= 2.0F);
                        if (trueAirborne) {
                            event.setAmount(event.getAmount() * (Config.fearAirDamage() * fearAmp));
                        }
                    }
                }
            }
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
        if (!(victim instanceof Parasite) || pivot == null || TRANSFERRING_PIVOT_DAMAGE.get()
                || event.getSource().is(DamageTypes.IN_FIRE) || event.getSource().is(DamageTypes.ON_FIRE)) {
            return;
        }
        LivingEntity rooter = resolveRooter(victim);
        if (rooter == null) {
            return;
        }
        if (!rooter.isAlive()) {
            clearRooterLink(victim);
            return;
        }
        float ratio = Math.min(0.95F, (pivot.getAmplifier() + 1) * Config.pivotDamageSent());
        float transferred = event.getAmount() * ratio;
        event.setAmount(event.getAmount() * (1.0F - ratio));
        TRANSFERRING_PIVOT_DAMAGE.set(true);
        try {
            rooter.hurt(event.getSource(), transferred);
        } finally {
            TRANSFERRING_PIVOT_DAMAGE.remove();
        }
    }

    public static void linkToRooter(LivingEntity parasite, LivingEntity rooter) {
        if (parasite instanceof Parasite && rooter != parasite && parasite.level() instanceof ServerLevel
                && parasite.level() == rooter.level()) {
            CompoundTag data = parasite.getPersistentData();
            data.putString(ROOTER_OWNER_UUID, rooter.getUUID().toString());
            data.putString(ROOTER_OWNER_DIMENSION, rooter.level().dimension().identifier().toString());
        }
    }

    private static LivingEntity resolveRooter(LivingEntity parasite) {
        if (!(parasite.level() instanceof ServerLevel level)) {
            return null;
        }
        CompoundTag data = parasite.getPersistentData();
        String ownerId = data.getStringOr(ROOTER_OWNER_UUID, "");
        String dimension = data.getStringOr(ROOTER_OWNER_DIMENSION, "");
        if (ownerId.isEmpty() || !dimension.equals(level.dimension().identifier().toString())) {
            clearRooterLink(parasite);
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(ownerId);
        } catch (IllegalArgumentException invalidId) {
            clearRooterLink(parasite);
            return null;
        }
        Entity entity = level.getEntity(uuid);
        if (entity == null) {
            // A missing entity may simply be in an unloaded chunk.
            return null;
        }
        if (entity instanceof LivingEntity rooter && isRooter(rooter) && rooter != parasite) {
            return rooter;
        }
        clearRooterLink(parasite);
        return null;
    }

    private static void clearRooterLink(LivingEntity parasite) {
        parasite.getPersistentData().remove(ROOTER_OWNER_UUID);
        parasite.getPersistentData().remove(ROOTER_OWNER_DIMENSION);
    }

    @SubscribeEvent
    public static void clearExpiredRooterLink(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity parasite && parasite instanceof Parasite
                && parasite.level() instanceof ServerLevel && parasite.tickCount % 20 == 0
                && !parasite.hasEffect(ModMobEffects.PIVOT)) {
            clearRooterLink(parasite);
        }
    }

    @SubscribeEvent
    public static void clearDeadRooterLinks(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        clearRooterLink(dead);
        if (!isRooter(dead)) {
            return;
        }
        String ownerId = dead.getUUID().toString();
        String dimension = level.dimension().identifier().toString();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity parasite
                    && ownerId.equals(parasite.getPersistentData().getStringOr(ROOTER_OWNER_UUID, ""))
                    && dimension.equals(parasite.getPersistentData().getStringOr(ROOTER_OWNER_DIMENSION, ""))) {
                clearRooterLink(parasite);
            }
        }
    }

    @SubscribeEvent
    public static void preventRooterPivot(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(ModMobEffects.PIVOT) && isRooter(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static boolean isRooter(LivingEntity entity) {
        if (entity instanceof AbominationEntity abomination) {
            return abomination.getKind() == AbominationEntity.Kind.BODIES;
        }
        return entity instanceof NexusParasiteEntity nexus && switch (nexus.getKind()) {
            case ROOTER_SI, ROOTER_SII, ROOTER_SIII, ROOTER_SIV -> true;
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
        if (player.level().isClientSide() || !hasSignCharm(player)) {
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
        if (!player.level().isClientSide()) {
            target.addEffect(new MobEffectInstance(ModMobEffects.CAMOUFLAGE,
                    duration, 0, false, true), player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        event.setCancellationResult(player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
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
