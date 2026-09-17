package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.IForgeShearable;

/**
 * Shared implementation for SRParasites 1.10.9 {@code BlockSnowGrass}, which registered one class
 * twice (short / tall) via a {@code tallGrass} constructor flag. The 1.20.1 port splits it into this
 * base plus {@link SnowShortGrassBlock} and {@link SnowTallGrassBlock}.
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockSnowGrass.java} —
 * hardness {@code 0.1F}, {@code SoundType.GRASS}, cutout render layer, shearable, passable
 * ({@code isPassable} = true), places a snow-covered grass block below itself, restores vanilla grass
 * when removed, and drops a seed with a 1/8 chance.
 *
 * <p>The 1.12.2 class extended {@code Block}; the 1.20.1 port extends {@link BushBlock}, whose
 * {@code canSurvive} ("the block below must support the plant") matches 1.10.9's
 * {@code canPlaceBlockAt} ({@code belowState.isSideSolid(world, below, EnumFacing.UP)}).
 */
public abstract class SnowGrassBlock extends BushBlock implements IForgeShearable {
    protected SnowGrassBlock(Properties properties) {
        super(properties);
    }

    /** 1.10.9 {@code onBlockAdded}: turn the vanilla grass block below into a snow-covered grass block. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        BlockPos below = pos.below();
        if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(below, ModBlocks.SNOW_COVERED_GRASS.get().defaultBlockState(), 3);
        }
    }

    /** 1.10.9 {@code breakBlock}: restore vanilla grass below. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(ModBlocks.SNOW_COVERED_GRASS.get())) {
                level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 1.10.9 {@code getDrops}: {@code if (RANDOM.nextInt(8) == 0) drops.add(grassSeed)}.
     *
     * <p><b>UNVERIFIED approximation:</b> 1.20.1 has no {@code ForgeHooks.getGrassSeed}, and the
     * vanilla {@code TallGrassBlock} drop table mixes wheat and beetroot seeds, so this port uses
     * {@link Items#WHEAT_SEEDS} only. Behaviour differs slightly from 1.10.9.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getLevel().getRandom().nextInt(8) == 0) {
            return List.of(new ItemStack(Items.WHEAT_SEEDS));
        }
        return List.of();
    }

    /** 1.10.9 {@code isShearable} — snow grass is always shearable. */
    @Override
    public boolean isShearable(ItemStack item, Level level, BlockPos pos) {
        return true;
    }

    /** 1.10.9 {@code onSheared}: {@code NonNullList.withSize(1, new ItemStack(Item.getItemFromBlock(this)))}. */
    @Override
    public List<ItemStack> onSheared(Player player, ItemStack item, Level level, BlockPos pos, int fortune) {
        return List.of(new ItemStack(asItem()));
    }

    /** 1.10.9 {@code getPickBlock} → the block's own item. */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter level, BlockPos pos,
            BlockState state) {
        return new ItemStack(asItem());
    }

    /** 1.10.9 {@code isPassable} = true: the plant never blocks skylight. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos) {
        return true;
    }
}
