package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class AlveoliBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty DEPLETED = BooleanProperty.create("depleted");
    private static final int BRONCHIAL_SEARCH_RADIUS = 6;
    private static final int RECOVERY_TICKS = 1_200;

    public AlveoliBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, true).setValue(DEPLETED, false));
    }

    @Override
public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, DEPLETED);
    }

    @Override
public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            updateActiveState(level, pos, state);
        }
    }

    @Override
public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            updateActiveState(level, pos, state);
        }
    }

    @Override
public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(DEPLETED)) {
            level.setBlock(pos, state.setValue(DEPLETED, false)
                    .setValue(ACTIVE, hasBronchialTube(level, pos)), Block.UPDATE_ALL);
            return;
        }
        updateActiveState(level, pos, state);
    }

    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack filled = ItemUtils.createFilledResult(stack, player,
                    new ItemStack(ModItems.ALVEOLAR_FLUID.get()));
            player.setItemInHand(hand, filled);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, state.setValue(DEPLETED, true).setValue(ACTIVE, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, RECOVERY_TICKS);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void updateActiveState(Level level, BlockPos pos, BlockState state) {
        boolean active = !state.getValue(DEPLETED) && hasBronchialTube(level, pos);
        if (state.getValue(ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    private static boolean hasBronchialTube(Level level, BlockPos origin) {
        return BlockPos.betweenClosedStream(
                origin.offset(-BRONCHIAL_SEARCH_RADIUS, -BRONCHIAL_SEARCH_RADIUS, -BRONCHIAL_SEARCH_RADIUS),
                origin.offset(BRONCHIAL_SEARCH_RADIUS, BRONCHIAL_SEARCH_RADIUS, BRONCHIAL_SEARCH_RADIUS))
                .map(level::getBlockState)
                .map(BlockState::getBlock)
                .anyMatch(block -> block == ModBlocks.HAIR_FOLLICLE_BLOCK.get()
                        || BuiltInRegistries.BLOCK.getKey(block).getPath().contains("hair_follicle"));
    }
}
