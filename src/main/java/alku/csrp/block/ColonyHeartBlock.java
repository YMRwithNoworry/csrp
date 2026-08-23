package alku.csrp.block;

import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class ColonyHeartBlock extends SrpCoreBlock {

    public ColonyHeartBlock(Properties properties) {
        super(properties);
    }

    @Override
public void removeRecord(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SrpWorldData.get(serverLevel).removeColony(pos);
        }
    }
}
