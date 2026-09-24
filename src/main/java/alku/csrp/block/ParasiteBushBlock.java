package alku.csrp.block;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteBush}
 * (out109 {@code block/BlockParasiteBush.java}) — registered as {@code srparasites:parasitebush}
 * ({@code init/SRPBlocks.java:545}, hardness 0.5F, {@code SoundType.GRASS}, harvest axe level 0,
 * destroy speed 0.2F, sword-holders break it four times faster).
 *
 * <p>Variant enum: the 1.10.9 class declares only {@code TENDRIL}, {@code BINE}, {@code POP},
 * {@code EYE}, {@code TOOH} ({@code BlockParasiteBush.java:555-560}) with {@code TENDRIL} as the
 * default ({@code BlockParasiteBush.java:18}).  This port keeps the wider superset that the shipped
 * {@code blockstates/parasitebush.json} asset already uses (the thorn and frost flavours come from
 * the newer resource pack the repository was built from) and implements the metadata behaviour for
 * the constants the 1.10.9 source actually defines; the extra constants carry no 1.10.9 behaviour by
 * definition and behave as plain decorative variants.</p>
 *
 * <p>Support rules ({@code BlockParasiteBush.java:49-113}): a {@code TENDRIL} or {@code BINE} block
 * may additionally hang from an SRP ceiling, and those two variants chain on top of each other.  The
 * derived {@code node} / {@code end} metadata comes from {@code getActualState}
 * ({@code BlockParasiteBush.java:300-320}): a {@code TOOH} sitting on special ground is a node, and
 * for {@code TENDRIL} / {@code BINE} the air above marks the end while the air below marks the
 * node.  Unsupported bushes are destroyed with drops
 * ({@code BlockParasiteBush.java:214-221}).</p>
 *
 * <p>{@code isLadder} returned true for {@code TENDRIL} / {@code BINE} when
 * {@code SRPConfigWorld.bushClimbingEnabled} was set ({@code BlockParasiteBush.java:262-276}); the
 * task's write scope excludes {@code config/**}, so the 1.12.2 default (enabled) is used and the
 * ladder hook is provided through {@link #isClimbable(BlockState)}.</p>
 */
public final class ParasiteBushBlock extends SrpBushBlock {
    /** {@code BlockParasiteBush.VARIANT}; the superset the shipped asset expects. */
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public ParasiteBushBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(VARIANT, Variant.TENDRIL)
                .setValue(NODE, false)
                .setValue(END, false));
    }

    /** Superset of the 1.10.9 enum, ordered so the metadata values of the original stay stable. */
    public enum Variant implements StringRepresentable {
        TENDRIL("tendril"),
        BINE("bine"),
        POP("pop"),
        EYE("eye"),
        TOOH("tooh"),
        DECANTER("decanter"),
        DECANTEREMPTY("decanterempty"),
        FROSTG("frostg"),
        FROSTGT("frostgt"),
        THORN("thorn"),
        THORNDEAD("thorndead"),
        THORNDORMAT("thorndormat"),
        THORNDORMATS("thorndormats"),
        THORNTWO("thorntwo"),
        THORNTWOS("thorntwos");

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
        return variant == Variant.TENDRIL || variant == Variant.BINE;
    }

    /** 1.12.2 {@code isLadder}: tendril and bine columns can be climbed. */
    public static boolean isClimbable(BlockState state) {
        return state.getBlock() instanceof ParasiteBushBlock bush && bush.isClimber(state);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // A tendril/bine may hang from an SRP ceiling (BlockParasiteBush.java:51-53,130-141).
        if (hasSrpCeilingSupport(level, pos)) {
            return true;
        }
        if (isSrpBush(state)) {
            if (!(state.getBlock() instanceof ParasiteBushBlock bush) || !bush.isClimber(state)) {
                return false;
            }
            return isSrpSupport(level.getBlockState(InfestedBushBlock.columnBase(level, pos)));
        }
        return isSrpSupport(state) && state.isFaceSturdy(level, pos, Direction.UP);
    }

    /** {@code isValidSRPCeilingSupport} + {@code hasSRPCeilingSupport}. */
    static boolean hasSrpCeilingSupport(BlockGetter level, BlockPos pos) {
        BlockPos cursor = pos.above();
        for (int guard = 0; guard < COLUMN_GUARD && isSrpBush(level.getBlockState(cursor)); guard++) {
            cursor = cursor.above();
        }
        BlockState support = level.getBlockState(cursor);
        return isSrpSupport(support) && support.isFaceSturdy(level, cursor, Direction.DOWN);
    }

    /** {@code BlockParasiteBush.func_175655_b(pos, true)} — unsupported bushes drop. */
    @Override
    protected void removeUnsupported(net.minecraft.world.level.Level level, BlockPos pos) {
        level.destroyBlock(pos, true);
    }

    @Override
    protected BlockState recomputeNodeEnd(BlockGetter level, BlockPos pos, BlockState state) {
        Variant variant = state.getValue(VARIANT);
        if (variant == Variant.TENDRIL) {
            boolean bottom = level.getBlockState(pos.below()).isAir();
            boolean top = !bottom && level.getBlockState(pos.above()).isAir();
            return state.setValue(END, top).setValue(NODE, bottom);
        }
        if (variant == Variant.BINE) {
            boolean top = level.getBlockState(pos.above()).isAir();
            boolean bottom = !top && level.getBlockState(pos.below()).isAir();
            return state.setValue(NODE, top).setValue(END, bottom);
        }
        return super.recomputeNodeEnd(level, pos, state);
    }
}
