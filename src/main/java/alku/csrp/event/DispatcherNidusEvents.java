package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.block.entity.DispatcherNidusBlockEntity;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.CarrierFlyingEntity;
import alku.csrp.entity.CarrierHeavyEntity;
import alku.csrp.entity.CarrierLightEntity;
import alku.csrp.entity.GnatEntity;
import alku.csrp.entity.IncompleteFormSmallEntity;
import alku.csrp.entity.LiceEntity;
import alku.csrp.entity.ManglerEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.RupterEntity;
import alku.csrp.world.SrpWorldData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Feeds parasite kills into an existing nearby Dispatcher Nidus.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DispatcherNidusEvents {
    private static final double COLLECT_RANGE = 10.0D;

    private DispatcherNidusEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        Entity killer = source.getEntity();
        if (!(killer instanceof LivingEntity killerEntity) || !(killer instanceof Parasite)
                || isExcluded(killerEntity)) {
            return;
        }
        ServerLevel level = (ServerLevel) victim.level();
        if (SrpWorldData.get(level).evolutionPhase() < 1) {
            return;
        }

        BlockPos center = killerEntity.blockPosition();
        DispatcherNidusBlockEntity nidus = findNearbyNidus(level, center);
        if (nidus != null) {
            nidus.addKill();
        }
    }

    private static boolean isExcluded(LivingEntity entity) {
        if (entity instanceof IncompleteFormSmallEntity) {
            return true;
        }
        return entity instanceof BuglinEntity
                || entity instanceof RupterEntity
                || entity instanceof ManglerEntity
                || entity instanceof GnatEntity
                || entity instanceof LiceEntity
                || entity instanceof CarrierLightEntity
                || entity instanceof CarrierHeavyEntity
                || entity instanceof CarrierFlyingEntity;
    }

    private static DispatcherNidusBlockEntity findNearbyNidus(ServerLevel level, BlockPos center) {
        List<DispatcherNidusBlockEntity> nidi = new ArrayList<>();
        int range = (int) Math.ceil(COLLECT_RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-range, -2, -range), center.offset(range, 6, range))) {
            if (level.getBlockEntity(pos) instanceof DispatcherNidusBlockEntity nidus) {
                nidi.add(nidus);
            }
        }
        return nidi.stream()
                .min(Comparator.comparingDouble(nidus -> nidus.getBlockPos().distSqr(center)))
                .orElse(null);
    }
}
