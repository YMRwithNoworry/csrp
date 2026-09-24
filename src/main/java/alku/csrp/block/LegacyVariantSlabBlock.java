package alku.csrp.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Variant-aware flavour of {@link LegacySlabBlock} for the four legacy slab ids whose shipped
 * blockstate carries both properties:
 * {@code parasiterubbleslabhalf}, {@code parasiterubbleslabdouble},
 * {@code parasitestainslabhalf}, {@code parasitestainslabdouble}.
 *
 * <p>The 1.12.2 originals — {@code BlockSlabRubble} and {@code BlockSlabStain}
 * (out109 {@code block/slabs/BlockSlabRubble.java:2-8}, {@code block/slabs/BlockSlabStain.java:2-8})
 * — declared their own {@code VARIANT} metadata on top of the vanilla slab {@code HALF} metadata,
 * with {@code BONE} / {@code DIRT} as the defaults.  26.3 models the half/double axis with
 * {@link SlabBlock#TYPE}, so the two original metadata axes become the two modern properties
 * {@code variant} + {@code type} that {@code blockstates/parasiterubbleslab*.json} already lists.</p>
 */
public class LegacyVariantSlabBlock extends LegacySlabBlock {
    private final EnumProperty<?> variant;

    public LegacyVariantSlabBlock(Properties properties, EnumProperty<?> variant, Object defaultValue) {
        super(properties);
        this.variant = variant;
        registerDefaultState(withVariant(
                stateDefinition.any().setValue(SlabBlock.TYPE, SlabType.BOTTOM), variant, defaultValue));
    }

    /** Raw-typed bridge so the shared variant enum of {@link ModBlocks} can be attached here. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState withVariant(BlockState state, EnumProperty<?> property, Object defaultValue) {
        return state.setValue((Property) property, (Comparable) defaultValue);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(variant);
    }
}
