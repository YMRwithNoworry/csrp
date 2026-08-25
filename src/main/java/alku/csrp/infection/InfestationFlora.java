package alku.csrp.infection;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 感染植被生长（原版 generateInfestationFeatures）：每次方块感染成功后掷骰，
 * 顶部约 1/12 长出寄染草（residue_plants），下方约 1/5 垂下寄染草脉（srpweb，最长 3 格）。
 */
public final class InfestationFlora {
    private InfestationFlora() {
    }

    public static void tryGrow(ServerLevel level, BlockPos converted, RandomSource random) {
        BlockPos above = converted.above();
        BlockState aboveState = level.getBlockState(above);
        if ((aboveState.isAir() || aboveState.canBeReplaced())
                && random.nextInt(12) == 0) {
            level.setBlock(above, ModBlocks.RESIDUE_PLANTS.get().defaultBlockState(), 3);
        }

        BlockPos below = converted.below();
        if (level.getBlockState(below).isAir() && random.nextInt(6) == 0) {
            int length = 1;
            if (random.nextInt(4) == 0) {
                length++;
            }
            if (random.nextInt(3) == 0) {
                length++;
            }
            for (int offset = 0; offset < length; offset++) {
                BlockPos target = below.below(offset);
                if (!level.getBlockState(target).isAir()) {
                    break;
                }
                level.setBlock(target, ModBlocks.SRP_WEB.get().defaultBlockState(), 3);
            }
        }
    }
}
