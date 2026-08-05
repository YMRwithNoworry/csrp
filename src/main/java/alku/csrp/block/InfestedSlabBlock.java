package alku.csrp.block;

import alku.csrp.Csrp;
import alku.csrp.infection.BlockInfestation;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Slab variant that spreads infestation while touching infected material. */
public final class InfestedSlabBlock extends SlabBlock {
    public static final MapCodec<SlabBlock> CODEC = simpleCodec(InfestedSlabBlock::new);

    public InfestedSlabBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    public MapCodec<? extends SlabBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        scheduleCheck(level, pos, 10);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        scheduleCheck(level, pos, 10);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (touchesInfestation(level, pos)) {
            BlockInfestation.infestAround(level, pos, 1);
            level.scheduleTick(pos, this, 20);
        }
    }

    private void scheduleCheck(Level level, BlockPos pos, int delay) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, delay);
        }
    }

    private static boolean touchesInfestation(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            Block block = level.getBlockState(pos.relative(direction)).getBlock();
            if (block instanceof InfestedBlock) {
                return true;
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (Csrp.MODID.equals(id.getNamespace()) && id.getPath().contains("infest")) {
                return true;
            }
        }
        return false;
    }
}
