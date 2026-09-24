package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared port of the SRP "bush" family — the 1.12.2 {@code BlockInfestedBush}
 * (out109 {@code block/BlockInfestedBush.java}) and {@code BlockParasiteBush}
 * (out109 {@code block/BlockParasiteBush.java}).  Both classes are the same machine with a different
 * metadata enum and a slightly different support rule, so the port factors the shared half here and
 * leaves the variant enum plus support specifics to {@link InfestedBushBlock} and
 * {@link ParasiteBushBlock}.
 *
 * <p>Shared behaviour, with original line references:</p>
 * <ul>
 *   <li>{@code END} / {@code NODE} metadata plus the {@code variant} enum
 *       ({@code BlockInfestedBush.java:9-11}, {@code BlockParasiteBush.java:8-11}).</li>
 *   <li>{@code TALL_GRASS_AABB} for the short variants and {@code REED_AABB} for the climber
 *       variants ({@code BlockInfestedBush.java:5-6,30-33}).</li>
 *   <li>Placement only on top of an {@code srparasites}-namespace block that is solid on its upper
 *       face, excluding {@code bloodyice} and {@code ashen_glass}
 *       ({@code BlockInfestedBush.java:53-69}).</li>
 *   <li>No item drops ({@code getItemDropped} returns {@code null},
 *       {@code BlockInfestedBush.java:257}); shears still yield the block itself through the
 *       {@code IShearable} contract ({@code BlockInfestedBush.java:379-388}).</li>
 *   <li>Right-clicking with raw meat harvests the bush, plays the 1.12.2 grass-break event 2005 and
 *       puts the held item on a 10-tick cooldown ({@code BlockInfestedBush.java:137-159}).</li>
 * </ul>
 */
public abstract class SrpBushBlock extends BushBlock {
    /** {@code NODE} — the bush sits directly on special SRP ground. */
    public static final BooleanProperty NODE = BooleanProperty.create("node");
    /** {@code END} — the last block of a climber column. */
    public static final BooleanProperty END = BooleanProperty.create("end");

    /** {@code TALL_GRASS_AABB} = AABB(0.1, 0.0, 0.1, 0.9, 0.8, 0.9). */
    protected static final VoxelShape TALL_GRASS_SHAPE = Block.box(1.6D, 0.0D, 1.6D, 14.4D, 12.8D, 14.4D);
    /** {@code REED_AABB} = AABB(0.125, 0.0, 0.125, 0.875, 1.0, 0.875). */
    protected static final VoxelShape REED_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    /** Guard on the original's downward column walk ({@code for (int guard = 0; guard < 64; ...)}). */
    protected static final int COLUMN_GUARD = 64;
    /** 1.12.2 item-use cooldown applied by the raw-meat harvest. */
    private static final int HARVEST_COOLDOWN_TICKS = 10;

    protected SrpBushBlock(Properties properties) {
        super(properties);
    }

    /** The subclass' {@code variant} property. */
    protected abstract EnumProperty<?> variantProperty();

    /** {@code true} when the current variant uses {@link #REED_SHAPE} and may climb. */
    protected abstract boolean isClimber(BlockState state);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(END, NODE, variantProperty());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return isClimber(state) ? REED_SHAPE : TALL_GRASS_SHAPE;
    }

    /** Serialized name of the current variant, used for the original's string comparisons. */
    protected final String variantName(BlockState state) {
        Object value = state.getValue(variantProperty());
        return value instanceof net.minecraft.util.StringRepresentable named
                ? named.getSerializedName()
                : String.valueOf(value).toLowerCase(Locale.ROOT);
    }

    /** {@code BlockInfestedBush.checkBush} — is this an SRP block that can carry vegetation? */
    protected static boolean isSrpSupport(BlockState support) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(support.getBlock());
        if (id == null || !"srparasites".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return !"bloodyice".equals(path) && !"ashen_glass".equals(path);
    }

    /** Original {@code isVariant(state, name)}. */
    protected final boolean isVariant(BlockState state, String name) {
        return variantName(state).equals(name);
    }

    /** {@code isSpecialGround} — {@code parasitestain} anywhere, or {@code parasiterubble} stone. */
    protected static boolean isSpecialGround(BlockGetter level, BlockPos groundPos) {
        BlockState below = level.getBlockState(groundPos);
        Block block = below.getBlock();
        if (block == ModBlocks.legacyBlock("parasitestain").get()) {
            return true;
        }
        if (block == ModBlocks.legacyBlock("parasiterubble").get()
                && below.hasProperty(ParasiteRubbleBlock.VARIANT)) {
            return below.getValue(ParasiteRubbleBlock.VARIANT) == ParasiteRubbleBlock.Variant.STONE;
        }
        return false;
    }

    protected static boolean isSrpBush(BlockState state) {
        return state.getBlock() instanceof SrpBushBlock;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (directionToNeighbour == Direction.DOWN || directionToNeighbour == Direction.UP
                || isClimber(state)) {
            return recomputeNodeEnd(level, pos, state);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos,
                neighbourState, random);
    }

    /**
     * 26.3 replacement for the 1.12.2 {@code getActualState} override
     * ({@code BlockInfestedBush.java:299-318}, {@code BlockParasiteBush.java:300-320}): the
     * {@code node} / {@code end} metadata is derived from the neighbours.
     */
    protected BlockState recomputeNodeEnd(BlockGetter level, BlockPos pos, BlockState state) {
        boolean node = false;
        boolean end = false;
        if (isVariant(state, "grass1") || isVariant(state, "tooh")) {
            node = isSpecialGround(level, pos.below());
        } else if (isClimber(state)) {
            boolean aboveEmpty = level.getBlockState(pos.above()).isAir();
            boolean belowEmpty = level.getBlockState(pos.below()).isAir();
            end = aboveEmpty;
            node = belowEmpty;
        }
        return state.setValue(NODE, node).setValue(END, end);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
            net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbourBlock, orientation, movedByPiston);
        if (!level.isClientSide() && !canSurvive(state, level, pos)) {
            removeUnsupported(level, pos);
        }
    }

    /** {@code BlockInfestedBush.java:241-246} drops nothing; the parasite bush used destroyBlock. */
    protected void removeUnsupported(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** {@code getItemDropped} returned {@code null}: the bush itself never drops. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack tool
                && tool.is(Items.SHEARS)) {
            return List.of(new ItemStack(asItem()));
        }
        return List.of();
    }

    /** {@code BlockInfestedBush.func_180639_a} — raw meat harvests the bush. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!isRawMeat(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            Block.popResource(level, pos.above(), new ItemStack(asItem()));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.levelEvent(2005, pos, 0);
            player.getCooldowns().addCooldown(stack, HARVEST_COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The 1.12.2 check consulted the ore dictionary for the {@code listAllmeatraw} /
     * {@code foodMeatRaw} keys plus a hard-coded vanilla list.  26.3 has no raw-meat item tag, so the
     * vanilla {@code c:meat}/{@code minecraft:meat} tag is the closest available equivalent.
     */
    protected static boolean isRawMeat(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.MEAT);
    }

    /** Re-exported for the registry so both bush blocks share one empty-cube constant. */
    static VoxelShape emptyShape() {
        return Shapes.empty();
    }
}
