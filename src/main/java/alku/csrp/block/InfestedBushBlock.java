package alku.csrp.block;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockInfestedBush}
 * (out109 {@code block/BlockInfestedBush.java}) — registered as {@code srparasites:infestedbush}
 * ({@code init/SRPBlocks.java:132}, hardness 0.4F, {@code SoundType.GRASS}).
 *
 * <p>The variant enum follows the original's
 * ({@code BlockInfestedBush.EnumType}: {@code INFECTED}, {@code GRASS1}, {@code FLOWER1},
 * {@code SPINE}, {@code VINE}, {@code ARC}) with {@code INFECTED} as the default
 * ({@code BlockInfestedBush.java:29-33}).  {@code SPINE} and {@code VINE} use {@code REED_AABB} and
 * can be chained on top of each other; both are also the only variants that climb
 * ({@code BlockInfestedBush.java:31,160-172,315-317}).</p>
 *
 * <p>Support rule ({@code BlockInfestedBush.java:53-69}): the block below — or, for a {@code SPINE} /
 * {@code VINE} column, the first non-bush block under the column — must belong to the
 * {@code srparasites} namespace, be solid on its upper face, and not be {@code bloodyice} or
 * {@code ashen_glass}.  Unsupported bushes are removed without drops
 * ({@code BlockInfestedBush.java:241-246}).</p>
 */
public final class InfestedBushBlock extends SrpBushBlock {
    /** {@code BlockInfestedBush.VARIANT}; ordering matches the original enum. */
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public InfestedBushBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(VARIANT, Variant.INFECTED)
                .setValue(NODE, false)
                .setValue(END, false));
    }

    /** The 6 original metadata constants, in the original declaration order. */
    public enum Variant implements StringRepresentable {
        INFECTED("infected"),
        GRASS1("grass1"),
        FLOWER1("flower1"),
        SPINE("spine"),
        VINE("vine"),
        ARC("arc");

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
    protected EnumProperty<?> variantProperty() {
        return VARIANT;
    }

    @Override
    protected boolean isClimber(BlockState state) {
        Variant variant = state.getValue(VARIANT);
        return variant == Variant.SPINE || variant == Variant.VINE;
    }

    /** {@code BlockInfestedBush.func_180671_f} / {@code checkBushV}. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        if (isSrpBush(state)) {
            if (!(state.getBlock() instanceof InfestedBushBlock bush) || !bush.isClimber(state)) {
                return false;
            }
            return isSrpSupport(level.getBlockState(columnBase(level, pos)));
        }
        return isSrpSupport(state) && state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    /** Walk down through the bush column, exactly like the original's {@code guard < 64} loop. */
    static BlockPos columnBase(BlockGetter level, BlockPos pos) {
        BlockPos base = pos;
        for (int guard = 0; guard < COLUMN_GUARD && isSrpBush(level.getBlockState(base)); guard++) {
            base = base.below();
        }
        return base;
    }

    /** {@code BlockInfestedBush.func_176196_c} — placement also allows a supported column below. */
    @Override
    protected BlockState recomputeNodeEnd(BlockGetter level, BlockPos pos, BlockState state) {
        if (isClimber(state)) {
            boolean end = !isSrpBush(level.getBlockState(pos.above()));
            boolean node = isSpecialGround(level, columnBase(level, pos));
            return state.setValue(NODE, node).setValue(END, end);
        }
        return super.recomputeNodeEnd(level, pos, state);
    }
}
