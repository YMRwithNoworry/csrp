package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.config.MobsConfig;
import alku.csrp.entity.KirinEntity;
import alku.csrp.entity.Parasite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.List;

/**
 * Direct port of SRParasites 1.10.9's {@code util/handlers/KirinDebugAttackHandler}.
 *
 * <p>Right-clicking with the kirin spawn item (originally {@code srparasites:itemmobspawner_kirin},
 * which this port ships as {@code csrp:kirin_spawn_egg}) forces the nearest kirin to fire a
 * judgement cut at its current target, at the nearest valid target, or - only when the config
 * allows it - at the player holding the item.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class KirinDebugAttackHandler {
    private static final ResourceLocation DEBUG_ITEM_ID = new ResourceLocation(Csrp.MODID,
            "kirin_spawn_egg");
    public static boolean DEBUG_FORCE_ENABLE_KIRIN_ITEM = false;
    private static final double SEARCH_HORIZONTAL = 48.0D;
    private static final double SEARCH_VERTICAL = 32.0D;

    private KirinDebugAttackHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        tryTrigger(event.getEntity(), event.getHand(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        tryTrigger(event.getEntity(), event.getHand(), event);
    }

    private static void tryTrigger(Player player, InteractionHand hand, PlayerInteractEvent event) {
        if (player == null || player.level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !DEBUG_ITEM_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return;
        }
        // Config is read last: this handler is on the global right-click path, so the cheap item
        // check must come first (an unreadable config value must not break every right-click).
        if (!MobsConfig.kirinDebugSpecialAttack() && !DEBUG_FORCE_ENABLE_KIRIN_ITEM) {
            return;
        }

        KirinEntity kirin = findClosestKirin(player);
        if (kirin == null) {
            return;
        }
        LivingEntity target = kirin.getTarget();
        if (!isValidTargetForDebug(kirin, target)) {
            target = findClosestValidTarget(kirin);
        }
        if (target == null && MobsConfig.kirinDebugTargetPlayerIfNoTarget()) {
            target = player;
        }
        if (target == null) {
            return;
        }

        kirin.setTarget(target);
        kirin.spawnJudgementCuts(target);
        // Original EntityParasiteBase#resetIdleTime().
        kirin.setNoActionTime(0);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean isValidTargetForDebug(KirinEntity kirin, LivingEntity living) {
        if (living == null || !living.isAlive() || living == kirin) {
            return false;
        }
        if (living instanceof Parasite) {
            return false;
        }
        if (living instanceof Player player) {
            if (player.isSpectator()) {
                return false;
            }
            if (player.getAbilities().instabuild && !MobsConfig.kirinDebugTargetCreativePlayers()) {
                return false;
            }
        }
        return kirin.getSensing().hasLineOfSight(living);
    }

    private static KirinEntity findClosestKirin(Player player) {
        AABB box = player.getBoundingBox().inflate(SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL);
        List<KirinEntity> nearby = player.level().getEntitiesOfClass(KirinEntity.class, box);
        KirinEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (KirinEntity kirin : nearby) {
            if (kirin == null || !kirin.isAlive()) {
                continue;
            }
            double distance = kirin.distanceToSqr(player);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = kirin;
            }
        }
        return closest;
    }

    private static LivingEntity findClosestValidTarget(KirinEntity kirin) {
        AABB box = kirin.getBoundingBox().inflate(SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL);
        List<LivingEntity> nearby = kirin.level().getEntitiesOfClass(LivingEntity.class, box);
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity living : nearby) {
            if (living == null || !living.isAlive() || living == kirin || living instanceof Parasite) {
                continue;
            }
            if (living instanceof Player player
                    && (player.isSpectator()
                            || player.getAbilities().instabuild
                                    && !MobsConfig.kirinDebugTargetCreativePlayers())) {
                continue;
            }
            if (!kirin.getSensing().hasLineOfSight(living)) {
                continue;
            }
            double distance = living.distanceToSqr(kirin);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = living;
            }
        }
        return closest;
    }
}
