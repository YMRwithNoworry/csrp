package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import alku.csrp.world.ColdStarTreeHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IForgeShearable;

/**
 * SRParasites 1.10.9 {@code BlockDeadheadGrassShort} (short deadhead vines) — a hanging plant.
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockDeadheadGrassShort.java} —
 * hardness {@code 0.0F} (1.20.1 {@code instabreak()}), {@code SoundType.GRASS}, cutout render layer,
 * shearable, no drops. 1.12.2 picked one of 5 textures per position in {@code getActualState};
 * 1.20.1 has no "actual state", so the texture index is baked in at placement time
 * ({@link #getStateForPlacement}) and also re-derived on rotation ({@link #rotate}).
 *
 * <p>Survival rule (1.10.9 {@code canPlaceBlockAt}/{@code canBlockStay}): the block <b>above</b> must
 * be a DEADHEAD-variant trunk, deadhead leaves, or any other block whose registry id contains
 * {@code deadhead}. Because {@code csrp:parasitetrunk} lost the legacy {@code variant=deadhead}
 * property during the port, {@link ColdStarTreeHandler#isDeadheadTrunk(BlockState)} matches the block
 * identity alone — see the class javadoc of that helper.
 */
public class DeadheadGrassShortBlock extends BushBlock implements IForgeShearable {
    /** 1.10.9: {@code PropertyInteger.create("texture", 0, 4)} — 5 texture variants. */
    public static final IntegerProperty TEXTURE = IntegerProperty.create("texture", 0, 4);

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    public DeadheadGrassShortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TEXTURE, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TEXTURE);
    }

    /**
     * 1.10.9 {@code canPlaceBlockAt}/{@code canBlockStay}: the plant hangs from the block above.
     * The default {@code BushBlock.canSurvive} checks the block <i>below</i>, which would be wrong
     * here, so it is fully replaced.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidDeadheadSupport(level.getBlockState(pos.above()));
    }

    /** 1.10.9's below-check existed only to mirror {@code canSurvive}; it must not re-add a floor rule. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /**
     * 1.20.1 replacement for 1.12.2 {@code getActualState}: bake the position-hashed texture index.
     * 1.10.9 formula (verbatim): {@code hash = x*3129871 ^ z*116129781 ^ y};
     * {@code hash = hash*hash*42317861 + hash*11}; {@code index = (hash >>> 16) % 5}.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TEXTURE, textureFor(context.getClickedPos()));
    }

    /**
     * 1.10.9 {@code getActualState} texture hash, kept verbatim: {@code hash = x*3129871 ^
     * z*116129781 ^ y}; {@code hash = hash*hash*42317861 + hash*11}; {@code index = (hash >>> 16) % 5}.
     */
    private static int textureFor(BlockPos pos) {
        long hash = pos.getX() * 3129871L ^ pos.getZ() * 116129781L ^ pos.getY();
        hash = hash * hash * 42317861L + hash * 11L;
        return (int) ((hash >>> 16) % 5L);
    }

    /** 1.10.9 {@code isValidDeadheadSupport}. */
    private static boolean isValidDeadheadSupport(BlockState supportState) {
        Block supportBlock = supportState.getBlock();
        if (ColdStarTreeHandler.isDeadheadTrunk(supportState)) {
            return true;
        }
        if (ColdStarTreeHandler.isDeadheadLeaves(supportState)) {
            return true;
        }
        return ColdStarTreeHandler.hasDeadheadId(supportBlock) && supportState.isSolid();
    }

    /** 1.10.9 {@code isShearable} — the vine is always shearable. */
    @Override
    public boolean isShearable(ItemStack item, Level level, BlockPos pos) {
        return true;
    }

    /** 1.10.9 {@code onSheared}: yields the block itself as an item. */
    @Override
    public List<ItemStack> onSheared(Player player, ItemStack item, Level level, BlockPos pos, int fortune) {
        return List.of(new ItemStack(ModBlocks.DEADHEAD_GRASS_SHORT.get()));
    }

    /** 1.10.9 {@code getItemDropped} returned {@code null} / {@code quantityDropped} = 0: no drops. */
    @Override
    public List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return List.of();
    }

    /** 1.10.9 {@code getPickBlock} → the block's own item. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.DEADHEAD_GRASS_SHORT.get());
    }
}
