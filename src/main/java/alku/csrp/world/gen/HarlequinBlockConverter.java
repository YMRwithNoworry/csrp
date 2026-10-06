package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Port of the 1.10.9 {@code util/convert/HarlequinBlockConverter} (out109
 * {@code util/convert/HarlequinBlockConverter.java}, 164 lines) — the block-replacement kernel
 * behind {@code /harlequin_convert}.
 *
 * <p>The original tested 1.12.2 {@code Material}s and block classes.  Their 26.3 equivalents:
 * {@code Material.AIR}/{@code WATER}/{@code LAVA} become {@code isAir} plus the water and lava
 * blocks, {@code BlockLeaves} becomes {@link LeavesBlock}, {@code BlockSand} becomes
 * {@link SandBlock} (it covered sand and red sand), and {@code BlockSandStone} plus
 * {@code Blocks.RED_SANDSTONE} become the sandstone family (1.12.2 kept every sandstone variant in
 * one {@code BlockSandStone} instance; 26.3 registers them separately).  The hard-coded ids are
 * {@code PLANKS} (the {@code minecraft:planks} tag in 26.3), {@code COBBLESTONE},
 * {@code MOSSY_COBBLESTONE}, {@code GRASS}, {@code DIRT} and {@code STONE}.</p>
 *
 * <p>Every write is {@code World#setBlockState(pos, state, 2)} in the original — clients only, no
 * neighbour updates — which is {@link ServerLevel#setBlock} with flag {@code 2} here.</p>
 */
public final class HarlequinBlockConverter {
    /** {@code World#setBlockState(pos, state, 2)}. */
    private static final int UPDATE_CLIENTS = 2;

    private HarlequinBlockConverter() {
    }

    /**
     * {@code HarlequinBlockConverter#convert} — replaces one block and returns the block it wrote, or
     * {@code null} when the original left the position alone.
     */
    public static Block convert(ServerLevel level, BlockPos pos, BlockState state, boolean blotchHere,
            RandomSource random, Config config) {
        if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
            return null;
        }

        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            level.setBlock(pos, config.alveoli().defaultBlockState(), UPDATE_CLIENTS);
            if (random.nextInt(100) < 30) {
                BlockPos below = pos.below();
                if (level.isEmptyBlock(below)) {
                    level.setBlock(below, withFacing(config.alveoliGrowth().defaultBlockState(),
                            Direction.DOWN), UPDATE_CLIENTS);
                }
            }
            return config.alveoli();
        }

        // 1.12.2 kept all six plank variants in the single Blocks.PLANKS instance; the modern
        // equivalent of "is a planks block" is the vanilla minecraft:planks tag.
        if (state.is(BlockTags.PLANKS) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE)) {
            level.setBlock(pos, config.harleskinn().defaultBlockState(), UPDATE_CLIENTS);
            return config.harleskinn();
        }

        if (state.is(Blocks.GRASS_BLOCK)) {
            if (!blotchHere) {
                level.setBlock(pos, config.harlequinnGrass().defaultBlockState(), UPDATE_CLIENTS);
                maybePlaceHair(level, pos, random, config);
                return config.harlequinnGrass();
            }
            level.setBlock(pos, config.harleskinn().defaultBlockState(), UPDATE_CLIENTS);
            BlockPos above = pos.above();
            Block aboveBlock = level.getBlockState(above).getBlock();
            if (aboveBlock == config.tresses() || aboveBlock == config.hirsute()) {
                level.removeBlock(above, false);
            }
            return config.harleskinn();
        }

        // The original's `!isTopSunlit || !sand && !sandstone && b != STONE`, then the same
        // three-way test again inside the branch (`&&` binds tighter than `||`).
        boolean convertibleGround = isAnySand(state) || isAnySandstone(state) || state.is(Blocks.STONE);
        if (!isTopSunlit(level, pos) || !convertibleGround) {
            if (!convertibleGround) {
                return null;
            }
            level.setBlock(pos, config.harleskinn().defaultBlockState(), UPDATE_CLIENTS);
            return config.harleskinn();
        }

        if (blotchHere) {
            level.setBlock(pos, config.harleskinn().defaultBlockState(), UPDATE_CLIENTS);
            return config.harleskinn();
        }

        level.setBlock(pos, config.harlequinnGrass().defaultBlockState(), UPDATE_CLIENTS);
        maybePlaceHair(level, pos, random, config);
        return config.harlequinnGrass();
    }

    /**
     * {@code HarlequinBlockConverter#maybePlaceHair} — 60 % {@code tresses_hair} (both halves, and
     * only when the block two above is free), then 20 % {@code hirsute_hair}.
     *
     * <p><strong>This never places anything in 1.10.9.</strong>  The gate is
     * {@link #canSustain}, i.e. the original's {@code IPlantable} check plus
     * {@code Block#canSustainPlant} on the ground block.  Forge's default
     * {@code Block#canSustainPlant} returns {@code false}, and the only implementation in the whole
     * 1.10.9 tree is the call site itself ({@code grep -rn canSustainPlant out109} → 1 hit), so
     * harlequin grass answers {@code false} for both plants and both branches stay shut.  The port
     * keeps the identical gate rather than "fixing" it into {@code canSurvive}: the original jar
     * converts grass without ever growing hair on it.</p>
     */
    private static void maybePlaceHair(ServerLevel level, BlockPos pos, RandomSource random, Config config) {
        BlockPos above = pos.above();
        if (!level.isEmptyBlock(above) || !isTopSunlit(level, pos)) {
            return;
        }
        int roll = random.nextInt(100);
        boolean canTresses = canSustain(level, pos, config.tresses());
        boolean canHirsute = canSustain(level, pos, config.hirsute());
        if (roll < 60 && canTresses && level.isEmptyBlock(above.above())) {
            placeDoublePlant(level, above, config.tresses());
        } else if (roll < 80 && canHirsute) {
            level.setBlock(above, config.hirsute().defaultBlockState(), UPDATE_CLIENTS);
        }
    }

    /**
     * {@code HarlequinBlockConverter#canSustain} — {@code ground.getBlock().canSustainPlant(...)}.
     *
     * <p>Returns {@code false} for exactly the reason the original did: Forge's
     * {@code Block#canSustainPlant} default is {@code false} and 1.10.9 overrides it nowhere.  The
     * {@code IPlantable} escape hatch in the original ({@code !(plant instanceof IPlantable)} →
     * {@code true}) is unreachable here because both plants are {@code BlockBush} subclasses and
     * therefore {@code IPlantable}.</p>
     */
    private static boolean canSustain(ServerLevel level, BlockPos pos, Block plant) {
        return false;
    }

    /** {@code HarlequinBlockConverter#isTopSunlit} — {@code world.canSeeSky(pos.up())}. */
    private static boolean isTopSunlit(ServerLevel level, BlockPos pos) {
        return level.canSeeSky(pos.above());
    }

    /** {@code state.getBlock() instanceof BlockSand}. */
    public static boolean isAnySand(BlockState state) {
        return state.getBlock() instanceof SandBlock;
    }

    /**
     * {@code state.getBlock() instanceof BlockSandStone || state.getBlock() == Blocks.RED_SANDSTONE}.
     *
     * <p>1.12.2's {@code BlockSandStone} carried the default, chiselled and smooth variants in one
     * block with a {@code TYPE} property, so the family has to be spelled out in 26.3.</p>
     */
    public static boolean isAnySandstone(BlockState state) {
        return state.is(Blocks.SANDSTONE) || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE) || state.is(Blocks.SMOOTH_SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.CHISELED_RED_SANDSTONE)
                || state.is(Blocks.CUT_RED_SANDSTONE) || state.is(Blocks.SMOOTH_RED_SANDSTONE);
    }

    /** {@code HarlequinBlockConverter#setFacingIfPresent} — sets a {@code facing} property if there is one. */
    private static BlockState withFacing(BlockState state, Direction facing) {
        for (Property<?> property : state.getProperties()) {
            if (!property.getName().equalsIgnoreCase("facing") || property.getValueClass() != Direction.class) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Property<Direction> facingProperty = (Property<Direction>) property;
            if (facingProperty.getPossibleValues().contains(facing)) {
                return state.setValue(facingProperty, facing);
            }
        }
        return state;
    }

    /** {@code HarlequinBlockConverter#placeDoublePlant} — lower half at {@code pos}, upper half above it. */
    private static void placeDoublePlant(ServerLevel level, BlockPos pos, Block plant) {
        if (plant instanceof DoublePlantBlock) {
            BlockState lower = plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
            level.setBlock(pos, lower, UPDATE_CLIENTS);
            level.setBlock(pos.above(), lower.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER),
                    UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, plant.defaultBlockState(), UPDATE_CLIENTS);
        }
    }

    /** {@code HarlequinBlockConverter.Config} — the seven palette blocks the commands pass in. */
    public record Config(Block harleskinn, Block harlequinnGrass, Block alveoli, Block alveoliGrowth,
            Block lipoma, Block tresses, Block hirsute) {
    }
}
