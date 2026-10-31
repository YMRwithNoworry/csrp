package alku.csrp.block;

import alku.csrp.world.DeadheadTreeGen;
import alku.csrp.world.gen.WorldGenParasiteTallFlower;
import alku.csrp.world.gen.WorldGenParasiteTree;
import alku.csrp.world.gen.WorldGenParasiteTreeThin;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteSapling}
 * (out109 {@code block/BlockParasiteSapling.java}) — registered as
 * {@code srparasites:parasitesapling} ({@code init/SRPBlocks.java:554}).
 *
 * <p>Reproduced behaviour:</p>
 * <ul>
 *   <li>the {@code stage} 0..1 and 6-value {@code variant} metadata with defaults
 *       {@code TREE}/0 ({@code BlockParasiteSapling.java:9-10,17}).</li>
 *   <li>{@code SAPLING_AABB} shape ({@code BlockParasiteSapling.java:11,20}).</li>
 *   <li>{@code updateTick} ({@code BlockParasiteSapling.java:22-30}): while the light above is at
 *       least 9 the sapling grows with probability {@code 1/7}.</li>
 *   <li>{@code grow} ({@code BlockParasiteSapling.java:32-39}): stage 0 only advances the stage; a
 *       stage-1 sapling removes itself and places the variant's tree, restoring the exact sapling
 *       state when the generator rejects the position.  Variant routing follows the original:
 *       TREE/CONSUMED → {@code WorldGenParasiteTree}, TREETHIN → {@code WorldGenParasiteTreeThin},
 *       FLOWERTALL/INFESTED → {@code WorldGenParasiteTallFlower}, DEADHEAD → the ported
 *       deadhead tree generator.</li>
 *   <li>{@code tryGrowWithInfestedBonemeal} ({@code BlockParasiteSapling.java:97-121}): bone-meal
 *       growth skips the vanilla sapling event; stage 0 advances any variant while stage 1 only
 *       permits CONSUMED, DEADHEAD, and INFESTED (the vanilla sapling fallback was removed there).</li>
 * </ul>
 */
public final class ParasiteSaplingBlock extends BushBlock implements BonemealableBlock {
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);

    /** {@code world.getLightFromNeighbors(pos.up()) >= 9}. */
    private static final int MIN_GROWTH_LIGHT = 9;
    /** {@code rand.nextInt(7) == 0}. */
    private static final int GROWTH_DIVISOR = 7;

    public ParasiteSaplingBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any()
                .setValue(VARIANT, Variant.TREE)
                .setValue(STAGE, 0));
    }

    /** The 6 original metadata constants, in the original declaration order. */
    public enum Variant implements StringRepresentable {
        TREE("tree"),
        TREETHIN("treethin"),
        FLOWERTALL("flowertall"),
        CONSUMED("consumed"),
        DEADHEAD("deadhead"),
        INFESTED("infested");

        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name.toLowerCase(Locale.ROOT);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, STAGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return HirsuteHairBlock.isSrpBlock(state)
                || state.is(net.minecraft.tags.BlockTags.DIRT)
                || state.is(net.minecraft.tags.BlockTags.SAND);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) >= MIN_GROWTH_LIGHT
                && random.nextInt(GROWTH_DIVISOR) == 0) {
            grow(state, level, pos, random);
        }
    }

    /** {@code BlockParasiteSapling.grow}; stage 1 runs its variant-specific tree generator. */
    private void grow(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_CLIENTS);
        } else {
            generateTree(state, level, pos, random);
        }
    }

    /** 1.10.9 {@code tryGrowWithInfestedBonemeal}: stage 0 advances any variant; stage 1 only permits
     * consumed, deadhead, and infested saplings. */
    public boolean tryGrowWithInfestedBonemeal(ServerLevel level, BlockPos pos, BlockState state,
            RandomSource random) {
        if (!state.is(this)) {
            return false;
        }
        if (state.getValue(STAGE) == 0) {
            return level.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_CLIENTS);
        }
        Variant variant = state.getValue(VARIANT);
        if (variant != Variant.CONSUMED && variant != Variant.DEADHEAD && variant != Variant.INFESTED) {
            return false;
        }
        return generateTree(state, level, pos, random);
    }

    /** Remove the sapling before generation and restore its exact state when space/resources reject it. */
    private boolean generateTree(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        boolean generated = switch (state.getValue(VARIANT)) {
            case TREE, CONSUMED -> WorldGenParasiteTree.generate(level, random, pos);
            case TREETHIN -> WorldGenParasiteTreeThin.generate(level, random, pos);
            case FLOWERTALL, INFESTED -> WorldGenParasiteTallFlower.generate(level, random, pos);
            case DEADHEAD -> DeadheadTreeGen.grow(level, random, pos);
        };
        if (!generated) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        return generated;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state,
            BonemealSource source) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state,
            BonemealSource source) {
        return level.getRandom().nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state,
            BonemealSource source) {
        grow(state, level, pos, random);
    }
}
