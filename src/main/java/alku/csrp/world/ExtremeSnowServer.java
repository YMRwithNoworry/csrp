package alku.csrp.world;

import alku.csrp.Csrp;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of SRP 1.10.9 {@code world/ExtremeSnowServer}: while the forced extreme snow is on, it sprinkles
 * snow around every player in the dimension.
 *
 * <p>Each player gets {@code (int)(12 + 48 * intensity)} attempts per tick inside an 18 block square;
 * every attempt that lands on open ground (and, unless {@code forceAnywhere} is set, on ground the biome
 * would snow on) gets one layer of snow.</p>
 *
 * <p>The original wrapped that placement in {@code isAirBlock(hit)} before checking for an existing
 * {@code SNOW_LAYER}, so its "grow the layer up to 8" branch was unreachable — the reachable behaviour,
 * reproduced here, is a single layer on open ground.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ExtremeSnowServer {
    /** {@code ExtremeSnowServer.onWorldTick}: {@code 12 + 48 * intensity} attempts per player. */
    private static final float TRIES_BASE = 12.0F;
    private static final float TRIES_PER_INTENSITY = 48.0F;
    /** {@code ExtremeSnowServer.radius}. */
    private static final int RADIUS = 18;

    private ExtremeSnowServer() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ExtremeSnowData data = ExtremeSnowData.get(level);
        if (!data.isEnabled()) {
            return;
        }
        int triesPerPlayer = (int) (TRIES_BASE + TRIES_PER_INTENSITY * data.getIntensity());
        boolean forceAnywhere = data.isForceAnywhere();
        int span = RADIUS * 2 + 1;

        for (ServerPlayer player : level.players()) {
            for (int attempt = 0; attempt < triesPerPlayer; attempt++) {
                int x = (int) (player.getX() + (level.getRandom().nextInt(span) - RADIUS));
                int z = (int) (player.getZ() + (level.getRandom().nextInt(span) - RADIUS));
                BlockPos base = new BlockPos(x, (int) player.getY(), z);
                BlockPos hit = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, base);
                if (!level.isEmptyBlock(hit)) {
                    continue;
                }
                if (!forceAnywhere && !level.getBiome(hit).value().shouldSnow(level, hit)) {
                    continue;
                }
                level.setBlock(hit, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1),
                        Block.UPDATE_CLIENTS);
            }
        }
    }
}
