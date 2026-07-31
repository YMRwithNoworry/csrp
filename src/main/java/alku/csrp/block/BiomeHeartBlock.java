package alku.csrp.block;

import alku.csrp.world.SrpWorldData;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class BiomeHeartBlock extends SrpCoreBlock {
    public static final MapCodec<BiomeHeartBlock> CODEC = simpleCodec(BiomeHeartBlock::new);

    public BiomeHeartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BiomeHeartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void removeRecord(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SrpWorldData.get(serverLevel).removeNode(pos);
        }
    }
}
