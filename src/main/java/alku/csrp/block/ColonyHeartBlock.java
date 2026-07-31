package alku.csrp.block;

import alku.csrp.world.SrpWorldData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class ColonyHeartBlock extends SrpCoreBlock {
    public static final MapCodec<ColonyHeartBlock> CODEC = simpleCodec(ColonyHeartBlock::new);

    public ColonyHeartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ColonyHeartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void removeRecord(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SrpWorldData.get(serverLevel).removeColony(pos);
        }
    }
}
