package alku.csrp.block;

import alku.csrp.block.entity.FogNullifierBlockEntity;
import alku.csrp.registry.ModBlockEntities;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/** Clears one connected parasite-fog volume per stored use. */
public final class FogNullifierBlock extends Block implements EntityBlock {
    public static final int MAX_USES = 3;
    private static final int CLEAR_LIMIT = 500_000;

    public FogNullifierBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FogNullifierBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            readUsesFromItem(level, pos, stack);
            attemptClear((ServerLevel) level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            attemptClear((ServerLevel) level, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            attemptClear((ServerLevel) level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof FogNullifierBlockEntity nullifier) || nullifier.usesRemaining() <= 0) {
            return List.of();
        }
        return List.of(stackWithUses(nullifier.usesRemaining()));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof FogNullifierBlockEntity nullifier) {
            return stackWithUses(nullifier.usesRemaining());
        }
        return stackWithUses(MAX_USES);
    }

    private ItemStack stackWithUses(int uses) {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        tag.putInt(FogNullifierBlockEntity.USES_TAG, uses);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.FOG_NULLIFIER.get(), tag);
        return stack;
    }

    private static void readUsesFromItem(Level level, BlockPos pos, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof FogNullifierBlockEntity nullifier)) {
            return;
        }
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data != null && data.contains(FogNullifierBlockEntity.USES_TAG)) {
            nullifier.setUsesRemaining(data.getInt(FogNullifierBlockEntity.USES_TAG));
        }
    }

    private static boolean attemptClear(ServerLevel level, BlockPos origin) {
        if (!(level.getBlockEntity(origin) instanceof FogNullifierBlockEntity nullifier)
                || nullifier.usesRemaining() <= 0 || nullifier.isClearing()) {
            return false;
        }
        int cleared;
        nullifier.setClearing(true);
        try {
            cleared = clearConnectedFog(level, origin);
        } finally {
            nullifier.setClearing(false);
        }
        if (cleared == 0) {
            return false;
        }
        level.playSound(null, origin, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9F, 1.0F);
        nullifier.setUsesRemaining(nullifier.usesRemaining() - 1);
        if (nullifier.usesRemaining() <= 0) {
            level.playSound(null, origin, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
        }
        return true;
    }

    private static int clearConnectedFog(ServerLevel level, BlockPos origin) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = origin.relative(direction);
            if (isFog(level, adjacent) && seen.add(adjacent)) {
                queue.add(adjacent);
            }
        }
        int cleared = 0;
        while (!queue.isEmpty() && cleared < CLEAR_LIMIT) {
            BlockPos current = queue.removeFirst();
            if (!isFog(level, current)) {
                continue;
            }
            level.sendParticles(ParticleTypes.SMOKE,
                    current.getX() + 0.5D, current.getY() + 0.5D, current.getZ() + 0.5D,
                    4, 0.3D, 0.3D, 0.3D, 0.01D);
            level.setBlock(current, Blocks.AIR.defaultBlockState(), 3);
            cleared++;
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (isFog(level, adjacent) && seen.add(adjacent)) {
                    queue.addLast(adjacent);
                }
            }
        }
        return cleared;
    }

    private static boolean isFog(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FogBlock;
    }
}
