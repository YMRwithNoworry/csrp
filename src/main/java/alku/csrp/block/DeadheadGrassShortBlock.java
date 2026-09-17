package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Skeleton for SRParasites 1.10.9 {@code BlockDeadheadGrassShort} (short deadhead vines).
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockDeadheadGrassShort.java} —
 * hardness {@code 0.0F} (1.20.1 {@code instabreak()}), {@code SoundType.GRASS}, cutout render layer,
 * shearable, no drops. 1.12.2 picked one of 5 textures per position in {@code getActualState};
 * 1.20.1 has no "actual state", so the texture index must be baked in at placement time
 * ({@link #TEXTURE}).
 *
 * <p>Slice 1 only establishes the class shape, the parent type and the state property; survival
 * rules, texture hashing, shearing and drop suppression are C-stage work.
 */
public class DeadheadGrassShortBlock extends BushBlock {
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

    // TODO(C): override canSurvive / mayPlaceOn so the plant only survives on DEADHEAD-variant trunks
    // and on DeadheadLeavesBlock, using the same "deadhead"/"dead_head" registry-id fallback as 1.10.9.
    // TODO(C): override getStateForPlacement to bake the position-hashed texture (1.10.9 formula:
    // hash = x*3129871 ^ z*116129781 ^ y; hash = hash*hash*42317861 + hash*11; index = (hash >>> 16) % 5).
    // TODO(C): implement net.minecraftforge.common.IForgeShearable (copy the exact signatures from
    // net.minecraft.world.level.block.TallGrassBlock: isShearable(ItemStack, Level, BlockPos) and
    // onSheared(Player, ItemStack, Level, BlockPos, int)) and suppress all drops.
}
