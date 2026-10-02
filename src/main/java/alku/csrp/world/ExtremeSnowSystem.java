package alku.csrp.world;

import alku.csrp.Csrp;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class ExtremeSnowSystem {
    private ExtremeSnowSystem() {
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % 200 != 0) {
            return;
        }
        SrpWorldData data = SrpWorldData.get(level);
        if (data.starType() != SrpStarType.COLD || !data.extremeSnow()) {
            return;
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ level.getGameTime());
        for (var player : level.players()) {
            BlockPos center = player.blockPosition();
            for (int i = 0; i < 4; i++) {
                int x = center.getX() + random.nextInt(17) - 8;
                int z = center.getZ() + random.nextInt(17) - 8;
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                BlockPos pos = new BlockPos(x, y, z);
                BlockState current = level.getBlockState(pos);
                if (current.is(Blocks.SNOW)) {
                    int layers = current.getValue(SnowLayerBlock.LAYERS);
                    if (layers < 8) {
                        level.setBlock(pos, current.setValue(SnowLayerBlock.LAYERS, layers + 1), 2);
                        continue;
                    }
                    pos = pos.above();
                } else {
                    pos = pos.above();
                }
                if (level.getBlockState(pos).isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
                    level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 2);
                }
            }
        }
    }
}
