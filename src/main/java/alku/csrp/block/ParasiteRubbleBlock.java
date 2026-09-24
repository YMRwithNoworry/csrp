package alku.csrp.block;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteRubble}
 * (out109 {@code block/BlockParasiteRubble.java}) — registered as {@code srparasites:parasiterubble}
 * ({@code init/SRPBlocks.java:551}, hardness 2.3F, resistance 10.0F, infested = {@code false}).
 *
 * <p>Two original behaviours are reproduced:</p>
 * <ul>
 *   <li>the 15-value {@code variant} metadata
 *       ({@code BlockParasiteRubble.EnumType}, default {@code BONE},
 *       {@code BlockParasiteRubble.java:23}).</li>
 *   <li>{@code getActualState} ({@code BlockParasiteRubble.java:74-117}): the four "weathered"
 *       variants swap to their snow-covered twin while {@code snow_layer} or {@code snow} sits
 *       above the block, and back again once the snow is gone.  In 26.3 that derived state is
 *       resolved from the neighbour update instead of {@code getActualState}.</li>
 * </ul>
 *
 * <p>The spreading random tick is inherited from {@link ParasiteSpreadingBlock} with
 * {@code infested = false}, matching the original's constructor call.</p>
 */
public final class ParasiteRubbleBlock extends ParasiteSpreadingBlock {
    /** {@code BlockParasiteRubble.VARIANT}; ordering matches the original enum. */
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public ParasiteRubbleBlock(Properties properties) {
        super(properties, false);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.BONE));
    }

    /** The 15 original metadata constants, in the original declaration order. */
    public enum Variant implements StringRepresentable {
        FLESH("flesh"),
        BONE("bone"),
        STONE("stone"),
        STONEDEBRIS("stonedebris"),
        WOOD("wood"),
        BRICKS("bricks"),
        METAL("metal"),
        OBSIDIAN("obsidian"),
        FUNGUS("fungus"),
        WEATHB("weathb"),
        WEATHBS("weathbs"),
        WEATHBC("weathbc"),
        WEATHBCS("weathbcs"),
        WEATHFS("weathfs"),
        WEATHFSS("weathfss");

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
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    /** {@code BlockParasiteRubble.getActualState} — snow above swaps the weathered twins. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.UP) {
            BlockState snowy = withSnow(state, isSnow(neighbourState));
            if (snowy != state) {
                return snowy;
            }
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    private static boolean isSnow(BlockState above) {
        return above.is(Blocks.SNOW) || above.is(Blocks.SNOW_BLOCK);
    }

    private static BlockState withSnow(BlockState state, boolean snowy) {
        return switch (state.getValue(VARIANT)) {
            case WEATHB -> state.setValue(VARIANT, snowy ? Variant.WEATHBS : Variant.WEATHB);
            case WEATHBS -> state.setValue(VARIANT, snowy ? Variant.WEATHBS : Variant.WEATHB);
            case WEATHBC -> state.setValue(VARIANT, snowy ? Variant.WEATHBCS : Variant.WEATHBC);
            case WEATHBCS -> state.setValue(VARIANT, snowy ? Variant.WEATHBCS : Variant.WEATHBC);
            case WEATHFS -> state.setValue(VARIANT, snowy ? Variant.WEATHFSS : Variant.WEATHFS);
            case WEATHFSS -> state.setValue(VARIANT, snowy ? Variant.WEATHFSS : Variant.WEATHFS);
            default -> state;
        };
    }
}
