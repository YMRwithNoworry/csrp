package alku.csrp.block;

import alku.csrp.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteCanister}
 * (out109 {@code block/BlockParasiteCanister.java}) — registered as
 * {@code srparasites:parasitecanister} ({@code init/SRPBlocks.java:546}, hardness 0.7F,
 * {@code tickRandom = true}).
 *
 * <p>Reproduced behaviour:</p>
 * <ul>
 *   <li>the 4-value {@code variant} metadata, default {@code SAC}
 *       ({@code BlockParasiteCanister.java:18,25}).</li>
 *   <li>the smaller {@code TALL_GRASS_AABB} shape for {@code CYST} only
 *       ({@code BlockParasiteCanister.java:10,19}); every other variant keeps the full cube.</li>
 *   <li>{@code updateTick} ({@code BlockParasiteCanister.java:36-43}): a {@code CYST} with air
 *       underneath vanishes.  The original also required {@code world.isAreaLoaded(pos, 3)}.</li>
 *   <li>{@code getItemDropped}/{@code quantityDropped} ({@code BlockParasiteCanister.java:44-58}):
 *       only {@code SAC} drops, and then the {@code itemlurecomponent6} item
 *       ({@code SRPItems.itemlurecomponent6} → {@link ModItems#LURECOMPONENT6}) with a 5&nbsp;%
 *       chance.  All other variants drop the block itself through the normal path.</li>
 * </ul>
 */
public final class ParasiteCanisterBlock extends Block {
    /** {@code BlockParasiteCanister.VARIANT}; ordering matches the original enum. */
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    /** {@code TALL_GRASS_AABB} = AABB(0.1, 0.0, 0.1, 0.9, 0.55, 0.9). */
    private static final VoxelShape CYST_SHAPE = Block.box(1.6D, 0.0D, 1.6D, 14.4D, 8.8D, 14.4D);
    /** {@code world.isAreaLoaded(pos, 3)} guard of the original tick. */
    private static final int AREA_LOAD_RADIUS = 3;
    /** {@code random.nextDouble() < 0.05} SAC drop chance. */
    private static final double SAC_DROP_CHANCE = 0.05D;

    public ParasiteCanisterBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.SAC));
    }

    /** The 4 original metadata constants, in the original declaration order. */
    public enum Variant implements StringRepresentable {
        SAC("sac"),
        CYST("cyst"),
        LUMP("lump"),
        BAG("bag");

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
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VARIANT) == Variant.CYST ? CYST_SHAPE : super.getShape(state, level, pos, context);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, AREA_LOAD_RADIUS)) {
            return;
        }
        if (state.getValue(VARIANT) == Variant.CYST && level.getBlockState(pos.below()).is(Blocks.AIR)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** {@code BlockParasiteCanister.java:44-58} — only SAC drops, and rarely. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(VARIANT) != Variant.SAC) {
            return super.getDrops(state, params);
        }
        List<ItemStack> drops = new ArrayList<>();
        if (params.getLevel().getRandom().nextDouble() < SAC_DROP_CHANCE) {
            drops.add(new ItemStack(ModItems.LURECOMPONENT6.get()));
        }
        return drops;
    }

    /** Kept so the loot-table context requirement of the SAC branch stays explicit. */
    static boolean usesToolContext(LootParams.Builder params) {
        return params.getOptionalParameter(LootContextParams.TOOL) != null;
    }
}
