package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared skeleton for SRParasites 1.10.9 {@code BlockSnowGrass}, which registered one class twice
 * (short / tall) via a {@code tallGrass} constructor flag. The 1.20.1 port splits it into this base
 * plus {@link SnowShortGrassBlock} and {@link SnowTallGrassBlock}.
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockSnowGrass.java} —
 * hardness {@code 0.1F}, {@code SoundType.GRASS}, cutout render layer, shearable, passable
 * ({@code isPassable} = true), places a snow-covered grass block below itself, restores vanilla grass
 * when removed, and drops a seed with a 1/8 chance.
 *
 * <p>The 1.12.2 class extended {@code Block}; the 1.20.1 port extends {@link BushBlock} so that the
 * vanilla placement rules ({@code mayPlaceOn} / {@code canSurvive}) come for free. Properties are
 * supplied by {@code ModBlocks} as
 * {@code mapColor(MapColor.COLOR_BROWN).noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)}.
 *
 * <p>Slice 1: constructor wiring and the state plumbing only. Everything below marked TODO(C) is
 * feature logic.
 */
public abstract class SnowGrassBlock extends BushBlock {
    protected SnowGrassBlock(Properties properties) {
        super(properties);
    }

    /**
     * 1.10.9 {@code onBlockAdded}: turn the vanilla grass block below into a snow-covered grass block.
     * TODO(C): implement — this needs {@code ModBlocks.SNOW_COVERED_GRASS.get()}, so it is C-stage work.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // TODO(C): if (!level.isClientSide) { below = pos.below(); if below is Blocks.GRASS_BLOCK
        //   -> level.setBlock(below, ModBlocks.SNOW_COVERED_GRASS.get().defaultBlockState(), 3) }
    }

    /**
     * 1.10.9 {@code breakBlock}: restore vanilla grass below. TODO(C): implement.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        // TODO(C): if (!level.isClientSide) { below = pos.below(); if below is
        //   ModBlocks.SNOW_COVERED_GRASS.get() -> level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 3) }
        // Note: the 1.20.1 equivalent of 1.10.9 breakBlock's sound is played from
        // SnowCoveredGrassBlock's own path; do not double up.
    }

    // 1.10.9 canPlaceBlockAt (below must have a solid top face) is subsumed by BushBlock.canSurvive,
    // which is inherited unchanged in slice 1; C may tighten it via mayPlaceOn if needed.

    // TODO(C): the 1.10.9 class also registered a custom ItemBlock subclass that played a place sound;
    // in 1.20.1 that is best done in an overridden BlockItem (or skipped, being purely cosmetic).
    // TODO(C): implement net.minecraftforge.common.IForgeShearable (signatures verified on
    // net.minecraft.world.level.block.TallGrassBlock: isShearable(ItemStack, Level, BlockPos) and
    // onSheared(Player, ItemStack, Level, BlockPos, int)) and the 1/8-chance wheat-seed drop
    // (1.20.1 has no ForgeHooks.getGrassSeed; using Items.WHEAT_SEEDS is an approximation → UNVERIFIED).
}
