package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.ParasiteDespawnHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;

/**
 * 原版 {@code Mob#checkDespawn} 有两条移除寄生体的路径：
 * <ul>
 *   <li>{@code PEACEFUL && shouldDespawnInPeaceful()} 直接 {@code discard()}，完全绕过
 *       {@code MobDespawnMixin} 的 cyst/recall 收尾流程，会让寄生体无痕消失；</li>
 *   <li>超距/闲置判定走 {@code removeWhenFarAway}，由 {@code MobDespawnMixin} 负责收尾。</li>
 * </ul>
 * NeoForge 的 {@link MobDespawnEvent} 在这两条分支之前触发，且能覆盖 PEACEFUL 分支，
 * 因此在这里补齐原版语义：
 * <ol>
 *   <li>Nexus 家族（原版 canD = SRPConfig.rsDespawn = false）永不自然消失；</li>
 *   <li>PEACEFUL 下先执行 spawnCyst/storeBefDes，落不下痕迹就拒绝移除。</li>
 * </ol>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteDespawnEvents {
    private ParasiteDespawnEvents() {
    }

    @SubscribeEvent
    public static void onMobDespawn(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob.level() instanceof ServerLevel level) || level.isClientSide
                || !(mob instanceof Parasite) || !ParasiteDespawnHandler.isCsrpParasite(mob)) {
            return;
        }
        if (!ParasiteDespawnHandler.canDespawnNaturally(mob)) {
            event.setResult(MobDespawnEvent.Result.DENY);
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            event.setResult(ParasiteDespawnHandler.leaveDespawnTrace(mob)
                    ? MobDespawnEvent.Result.ALLOW
                    : MobDespawnEvent.Result.DENY);
        }
    }
}
