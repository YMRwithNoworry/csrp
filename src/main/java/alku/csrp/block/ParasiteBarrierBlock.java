package alku.csrp.block;

import alku.csrp.block.entity.ParasiteBarrierBlockEntity;
import alku.csrp.registry.ModPermissions;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 原件 {@code BlockParasiteBarrier} 与 {@code TileEntityParasiteBarrier} 的兼容实现。 */
public final class ParasiteBarrierBlock extends Block implements EntityBlock {
    public ParasiteBarrierBlock(Properties properties) {
        super(properties.strength(-1.0F, 6_000_000.0F).pushReaction(PushReaction.IMMOVEABLE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParasiteBarrierBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return type == alku.csrp.registry.ModBlockEntities.PARASITE_BARRIER.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> {
                    if (blockEntity instanceof ParasiteBarrierBlockEntity barrier) {
                        barrier.tick();
                    }
                }
                : null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && oldState.getBlock() != this
                && level.getBlockEntity(pos) instanceof ParasiteBarrierBlockEntity barrier) {
            barrier.initializeCenter(pos);
            if (level instanceof ServerLevel serverLevel) {
                barrier.scrubMobsAroundField(serverLevel);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.getAbilities().instabuild
                || player instanceof ServerPlayer serverPlayer
                && net.neoforged.neoforge.server.permission.PermissionAPI.getPermission(
                        serverPlayer, ModPermissions.PARASITE_BARRIER_CONFIGURE))) {
            player.sendSystemMessage(Component.translatable("msg.csrp.barrier.no_permission"));
            return InteractionResult.CONSUME;
        }
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ParasiteBarrierBlockEntity barrier) {
            barrier.cycleRadius(serverLevel);
            player.sendSystemMessage(Component.translatable("msg.csrp.barrier.radius_set", barrier.radiusChunks()));
        }
        return InteractionResult.CONSUME;
    }

    /** 原件 BlockParasiteBarrier 没有碰撞箱，但仍按完整方块遮挡与渲染。 */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
            java.util.function.BiConsumer<ItemStack, BlockPos> onHit) {
        // 原版屏障在爆炸中保留，不掉落且不执行爆炸破坏逻辑。
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }
}
