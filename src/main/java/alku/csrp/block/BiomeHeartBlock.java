package alku.csrp.block;

import alku.csrp.world.SrpWorldData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class BiomeHeartBlock extends SrpCoreBlock {

    public BiomeHeartBlock(Properties properties) {
        super(properties);
    }

    @Override
public void removeRecord(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SrpWorldData.get(serverLevel).removeNode(pos);
        }
    }
}
