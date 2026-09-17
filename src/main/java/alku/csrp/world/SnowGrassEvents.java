package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Server-side snow grass conversion, ported from SRParasites 1.10.9
 * {@code util/handlers/SnowGrassHandler}.
 *
 * <p>Two triggers, both preserved:
 * <ol>
 *   <li>{@code BlockEvent.PlaceEvent} (1.12.2) → {@link BlockEvent.EntityPlaceEvent}: placing a snow
 *       layer on top of grass / tall grass converts it into snow grass. The pre-placement state is
 *       read from {@code event.getBlockSnapshot().getReplacedBlock()} (verified: the
 *       {@code BlockSnapshot} getters, and the absence of {@code BlockSnapshot.getState()} in Forge
 *       47.4.23, are recorded in PLAN.md §4.2/§10.1).</li>
 *   <li>{@code WorldTickEvent} (1.12.2) → {@link TickEvent.LevelTickEvent}: while it rains, every 10
 *       ticks, 96 random columns inside a 32-block radius around each player are converted when the
 *       column can hold snow.</li>
 * </ol>
 *
 * <p>1.12.2 {@code WorldProvider.canDoRainSnowIce} becomes
 * {@code level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.NONE}
 * (PLAN.md T6), and {@code world.getLightFor(EnumSkyBlock.BLOCK, pos)} becomes
 * {@code level.getBrightness(LightLayer.BLOCK, pos)}.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SnowGrassEvents {
    private static final int NATURAL_CHECK_RADIUS = 32;
    private static final int NATURAL_CHECKS_PER_PLAYER = 96;
    /** 1.10.9 used {@code world.getTotalWorldTime() % 10 == 0}. */
    private static final long NATURAL_CHECK_INTERVAL = 10L;
    /** 1.10.9 {@code canSnowHere}: {@code getTemperature > 0.15 → false}. */
    private static final float MAX_SNOW_TEMPERATURE = 0.15F;
    /** 1.10.9 {@code canSnowHere}: {@code blockLight >= 10 → false}. */
    private static final int MAX_SNOW_LIGHT = 10;

    private SnowGrassEvents() {
    }

    /** 1.10.9 {@code onSnowPlaced}. */
    @SubscribeEvent
    public static void onSnowPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        if (!event.getPlacedBlock().is(Blocks.SNOW)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState replaced = event.getBlockSnapshot().getReplacedBlock();
        if (isShortGrass(replaced)) {
            level.setBlock(pos, ModBlocks.SNOW_SHORT_GRASS.get().defaultBlockState(), 3);
        } else if (isTallGrass(replaced)) {
            convertTallGrass(level, pos, replaced);
        }
    }

    /** 1.10.9 {@code onWorldTick}. */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }
        Level level = event.level;
        if (!level.isRaining() || level.getGameTime() % NATURAL_CHECK_INTERVAL != 0L) {
            return;
        }
        for (Player player : level.players()) {
            checkNaturalSnowAroundPlayer(level, player);
        }
    }

    /** 1.10.9 {@code checkNaturalSnowAroundPlayer}. */
    private static void checkNaturalSnowAroundPlayer(Level level, Player player) {
        int centerX = (int) Math.floor(player.getX());
        int centerZ = (int) Math.floor(player.getZ());
        for (int i = 0; i < NATURAL_CHECKS_PER_PLAYER; i++) {
            int x = centerX + level.getRandom().nextInt(NATURAL_CHECK_RADIUS * 2 + 1) - NATURAL_CHECK_RADIUS;
            int z = centerZ + level.getRandom().nextInt(NATURAL_CHECK_RADIUS * 2 + 1) - NATURAL_CHECK_RADIUS;
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isShortGrass(state) && !isTallGrass(state)) {
                continue;
            }
            if (!canSnowHere(level, pos)) {
                continue;
            }
            if (isShortGrass(state)) {
                level.setBlock(pos, ModBlocks.SNOW_SHORT_GRASS.get().defaultBlockState(), 3);
            } else {
                convertTallGrass(level, pos, state);
            }
        }
    }

    /** 1.10.9 {@code canSnowHere}. */
    private static boolean canSnowHere(Level level, BlockPos pos) {
        if (!level.canSeeSky(pos.above())) {
            return false;
        }
        BlockPos pos2 = pos.immutable();
        if (level.getBiome(pos2).value().getBaseTemperature() > MAX_SNOW_TEMPERATURE) {
            return false;
        }
        if (level.getBrightness(LightLayer.BLOCK, pos) >= MAX_SNOW_LIGHT) {
            return false;
        }
        return level.getBiome(pos2).value().getPrecipitationAt(pos2) != Biome.Precipitation.NONE;
    }

    /** 1.10.9 {@code isShortGrass}: {@code TALLGRASS} with {@code type == GRASS}. */
    private static boolean isShortGrass(BlockState state) {
        return state.is(Blocks.GRASS);
    }

    /** 1.10.9 {@code isTallGrass}: {@code DOUBLE_PLANT}, upper half always counts. */
    private static boolean isTallGrass(BlockState state) {
        if (!state.is(Blocks.TALL_GRASS) && !state.is(Blocks.LARGE_FERN)) {
            return false;
        }
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
    }

    /** 1.10.9 {@code convertTallGrass}. */
    private static void convertTallGrass(Level level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = pos;
        BlockState lowerState = state;
        if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            lowerPos = pos.below();
            lowerState = level.getBlockState(lowerPos);
        }
        Block block = lowerState.getBlock();
        if ((block == Blocks.TALL_GRASS || block == Blocks.LARGE_FERN)
                && lowerState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(lowerPos.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(lowerPos, ModBlocks.SNOW_TALL_GRASS.get().defaultBlockState(), 3);
        }
    }
}
