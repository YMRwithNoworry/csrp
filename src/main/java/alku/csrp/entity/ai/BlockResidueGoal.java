package alku.csrp.entity.ai;

import alku.csrp.Config;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/** Reproduces the legacy adapted-parasite residue placement sequence. */
public final class BlockResidueGoal extends Goal {
    private final AdaptedVariantEntity parent;
    private final int range;
    private int blockBreakCounter = 160;

    public BlockResidueGoal(AdaptedVariantEntity parent, int range) {
        this.parent = parent;
        this.range = Math.max(1, range);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return eligible();
    }

    @Override
    public boolean canContinueToUse() {
        return eligible();
    }

    @Override
    public void stop() {
        parent.setResidueActivity(false);
        blockBreakCounter = 160;
    }

    @Override
    public void tick() {
        if (blockBreakCounter > 0) {
            if (parent.getRandom().nextInt(5) == 0) {
                blockBreakCounter--;
            }
            return;
        }

        blockBreakCounter--;
        // Legacy EntityAIBlockResidue emits the residue splash event on every
        // charge tick, including the sound and placement ticks below.
        if (!parent.level().isClientSide()) {
            parent.level().broadcastEntityEvent(parent, (byte) 13);
        }
        if (blockBreakCounter == -1) {
            parent.setResidueActivity(true);
            parent.getNavigation().stop();
            parent.playSound(ModSounds.get("adapted.v"), 2.0F, 1.0F);
        }
        if (blockBreakCounter == -40) {
            parent.playSound(ModSounds.get("adapted.v"), 2.0F, 1.0F);
        }
        if (blockBreakCounter == -60) {
            placeResidue();
        }
        if (blockBreakCounter == -100) {
            parent.setResidueActivity(false);
            blockBreakCounter = 200;
        }
    }

    private boolean eligible() {
        LivingEntity target = parent.getTarget();
        return Config.parasiteGenResidue() && parent.isAlive() && target == null
                && !parent.isInWaterOrBubble();
    }

    private void placeResidue() {
        if (!(parent.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos origin = BlockPos.containing(parent.getX(), parent.getY() + 0.1D, parent.getZ());
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                BlockPos candidate = origin.offset(x, 0, z);
                BlockState state = level.getBlockState(candidate);
                BlockState below = level.getBlockState(candidate.below());
                if (state.isAir() && !below.isAir() && below.isSolidRender(level, candidate.below())
                        && !below.is(ModBlocks.INFESTED_STAIN.get())
                        && parent.getRandom().nextBoolean()) {
                    level.setBlock(candidate, ModBlocks.INFESTED_REMAINS.get().defaultBlockState(), 3);
                }
            }
        }
    }
}
