package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of SRParasites 1.10.9 {@code util.handlers.SnowGrassHandler}.
 *
 * <p>Two behaviours are carried over:</p>
 * <ol>
 *   <li>Placing a snow layer on top of short/tall grass converts it to the matching snow grass.</li>
 *   <li>While it is raining, every tenth tick a handful of random columns around each player are
 *       checked and any grass that can be snowed on is converted, which is what makes the cold star
 *       progressively turn white.</li>
 * </ol>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SnowGrassEvents {
    private static final int NATURAL_CHECK_RADIUS = 32;
    private static final int NATURAL_CHECKS_PER_PLAYER = 96;

    private SnowGrassEvents() {
    }

    /** Original {@code onSnowPlaced}: a new snow layer replaces grass with snow grass. */
    @SubscribeEvent
    public static void onSnowPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (!placed.is(Blocks.SNOW)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState replaced = event.getBlockSnapshot().getState();
        if (replaced.is(Blocks.SHORT_GRASS)) {
            level.setBlock(pos, ModBlocks.SNOW_SHORT_GRASS.get().defaultBlockState(), 3);
        } else if (isTallGrass(replaced, DoubleBlockHalf.LOWER)) {
            convertTallGrass(level, pos, replaced);
        }
    }

    /** Original {@code onWorldTick}: natural snowing around each player while it rains. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.isRaining()) {
            return;
        }
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            checkNaturalSnowAroundPlayer(level, player);
        }
    }

    private static void checkNaturalSnowAroundPlayer(ServerLevel level, ServerPlayer player) {
        int centerX = (int) Math.floor(player.getX());
        int centerZ = (int) Math.floor(player.getZ());
        RandomSource random = level.getRandom();

        for (int i = 0; i < NATURAL_CHECKS_PER_PLAYER; i++) {
            int x = centerX + random.nextInt(NATURAL_CHECK_RADIUS * 2 + 1) - NATURAL_CHECK_RADIUS;
            int z = centerZ + random.nextInt(NATURAL_CHECK_RADIUS * 2 + 1) - NATURAL_CHECK_RADIUS;
            BlockPos column = new BlockPos(x, 0, z);
            if (!level.hasChunkAt(column)) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.is(Blocks.SHORT_GRASS) || isTallGrass(state, DoubleBlockHalf.LOWER))) {
                continue;
            }
            if (!canSnowHere(level, pos)) {
                continue;
            }
            if (state.is(Blocks.SHORT_GRASS)) {
                level.setBlock(pos, ModBlocks.SNOW_SHORT_GRASS.get().defaultBlockState(), 3);
            } else {
                convertTallGrass(level, pos, state);
            }
        }
    }

    /**
     * Original {@code canSnowHere}: open sky, biome temperature at or below 0.15, low block light
     * and a biome that actually receives precipitation.  26.3 keeps the height-adjusted temperature
     * private, so {@code warmEnoughToRain} (its public {@code >= 0.15} test) is the modern spelling
     * of the same threshold.
     */
    private static boolean canSnowHere(ServerLevel level, BlockPos pos) {
        if (!level.canSeeSky(pos.above())) {
            return false;
        }
        if (level.getBrightness(LightLayer.BLOCK, pos) >= 10) {
            return false;
        }
        var biome = level.getBiome(pos).value();
        if (biome.warmEnoughToRain(pos, level.getSeaLevel())) {
            return false;
        }
        return biome.getPrecipitationAt(pos, level.getSeaLevel())
                != net.minecraft.world.level.biome.Biome.Precipitation.NONE;
    }

    private static boolean isTallGrass(BlockState state, DoubleBlockHalf half) {
        if (!state.is(Blocks.TALL_GRASS)) {
            return false;
        }
        return state.getValue(DoublePlantBlock.HALF) == half;
    }

    /** Original {@code convertTallGrass}: squash the double plant into the single snow tall grass. */
    private static void convertTallGrass(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = pos;
        if (state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
            lowerPos = pos.below();
        }
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!isTallGrass(lowerState, DoubleBlockHalf.LOWER)) {
            return;
        }
        level.setBlock(lowerPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(lowerPos, ModBlocks.SNOW_TALL_GRASS.get().defaultBlockState(), 3);
    }
}
