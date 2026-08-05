package alku.csrp.block;

import alku.csrp.block.entity.DispatcherNidusBlockEntity;
import alku.csrp.registry.ModMobEffects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Dispatcher Nidus: collects parasite kills, spawns Stage I Dispatchers, and
 * knocks back players that touch its sides while applying the concussive
 * smoke trail effect.
 */
public final class DispatcherNidusBlock extends Block implements EntityBlock {
    public DispatcherNidusBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DispatcherNidusBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null
                : (level1, pos, state1, blockEntity) ->
                        DispatcherNidusBlockEntity.serverTick(level1, pos, state1, blockEntity);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof Player player)
                || level.getGameTime() % 20L != 0L) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 away = entity.position().subtract(center);
        double length = away.length();
        if (length < 0.001D) {
            away = new Vec3(entity.getRandom().nextDouble() - 0.5D, 0.0D,
                    entity.getRandom().nextDouble() - 0.5D).normalize();
        } else {
            away = away.normalize();
        }
        player.push(away.x * 1.6D, 0.45D, away.z * 1.6D);
        player.addEffect(new MobEffectInstance(ModMobEffects.DOD_SMOKE_TRAIL, 20, 0));
    }

    public static boolean tryPlace(ServerLevel level, BlockPos pos) {
        BlockPos candidate = pos;
        if (!level.getBlockState(candidate).canBeReplaced()) {
            candidate = pos.below();
        }
        if (!level.getBlockState(candidate).canBeReplaced()) {
            return false;
        }
        level.setBlockAndUpdate(candidate, alku.csrp.registry.ModBlocks.DISPATCHER_NIDUS.get()
                .defaultBlockState());
        return true;
    }
}
