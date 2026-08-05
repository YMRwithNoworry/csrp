package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.block.entity.ParasiticCystBlockEntity;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.CarrierFlyingEntity;
import alku.csrp.entity.CarrierHeavyEntity;
import alku.csrp.entity.CarrierLightEntity;
import alku.csrp.entity.GnatEntity;
import alku.csrp.entity.IncompleteFormSmallEntity;
import alku.csrp.entity.LiceEntity;
import alku.csrp.entity.ManglerEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.ParasiteBlockInventory;
import alku.csrp.entity.RupterEntity;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Parasites that die drop a Gluttonous Cyst (their stored block inventory
 * collector). The block-inventory hooks fill the cyst contents; this only
 * places the storage block.
 */
@EventBusSubscriber(modid = Csrp.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ParasiticCystEvents {
    private static final float DROP_CHANCE = 0.35F;

    private ParasiticCystEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(victim instanceof Parasite)
                || isExcluded(victim)) {
            return;
        }
        ServerLevel level = (ServerLevel) victim.level();
        if (level.random.nextFloat() >= DROP_CHANCE) {
            return;
        }
        BlockPos pos = victim.blockPosition();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.below();
        }
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlockAndUpdate(pos, ModBlocks.GLUTTONOUS_CYST.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof ParasiticCystBlockEntity cyst) {
                NonNullList<ItemStack> items = ParasiteBlockInventory.takeAll(victim);
                for (int slot = 0; slot < items.size(); slot++) {
                    cyst.setItem(slot, items.get(slot));
                }
            }
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
}
