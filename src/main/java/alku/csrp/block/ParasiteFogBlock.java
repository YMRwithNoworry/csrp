package alku.csrp.block;

import alku.csrp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteFog}
 * (out109 {@code block/BlockParasiteFog.java}) — registered as {@code srparasites:parasitefog}
 * ({@code init/SRPBlocks.java:967}).
 *
 * <p>Reproduced behaviour:</p>
 * <ul>
 *   <li>the {@code air} metadata 0..2 ({@code BlockParasiteFog.STAGE},
 *       {@code BlockParasiteFog.java:9,19}); the shipped {@code blockstates/parasitefog.json}
 *       expects exactly {@code air=0/1/2}.</li>
 *   <li>{@code updateTick} ({@code BlockParasiteFog.java:78-124}): {@code air=0} advances to
 *       {@code air=1}, and {@code air=2} expands the "air=2" state to every fog block in the
 *       5&times;5&times;5 neighbourhood and then removes itself.</li>
 *   <li>Right-clicking with a glass bottle fills a fog bottle and clears the block
 *       ({@code BlockParasiteFog.java:125-146}).</li>
 *   <li>No collision, no drops, random-ticking
 *       ({@code BlockParasiteFog.java:60-71}).</li>
 * </ul>
 */
public class ParasiteFogBlock extends Block {
    /** {@code STAGE} — the original named the property {@code air} with range 0..2. */
    public static final IntegerProperty AIR = IntegerProperty.create("air", 0, 2);
    /** {@code BGrange = 2} cube walk of the {@code air=2} expansion. */
    private static final int EXPAND_RADIUS = 2;

    public ParasiteFogBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(AIR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AIR);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int meta = state.getValue(AIR);
        if (meta == 0) {
            level.setBlock(pos, state.setValue(AIR, 1), Block.UPDATE_ALL);
            return;
        }
        if (meta != 2) {
            return;
        }
        for (int dx = -EXPAND_RADIUS; dx <= EXPAND_RADIUS; dx++) {
            for (int dy = -EXPAND_RADIUS; dy <= EXPAND_RADIUS; dy++) {
                for (int dz = -EXPAND_RADIUS; dz <= EXPAND_RADIUS; dz++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    BlockState targetState = level.getBlockState(target);
                    if (targetState.getBlock() instanceof ParasiteFogBlock
                            && targetState.getValue(AIR) != 2) {
                        level.setBlock(target, targetState.setValue(AIR, 2), Block.UPDATE_ALL);
                    }
                }
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** {@code BlockParasiteFog.func_180639_a} — a glass bottle turns the fog into a fog bottle. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            ItemStack fogBottle = new ItemStack(ModItems.FOG_BOTTLE.get());
            if (!player.getInventory().add(fogBottle)) {
                player.drop(fogBottle, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
