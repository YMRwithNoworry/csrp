package alku.csrp.item;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * 希腊火：对寄生植被使用后引燃并烧毁整片连片灌木（对应原版 ItemGreekFire）。
 * 原版目标为 BlockParasiteBush/BlockInfestedBush/BlockInfestedRemain/BlockGore；
 * 当前对齐本项目已有的 infestremain 与 residue_plants，后续移植灌木/血肉块时并入 {@link #isTargetBush}。
 */
public class GreekFireItem extends Item {
    private static final int MAX_SPREAD = 12000;
    private static final int LINK_RADIUS = 8;
    private static final int PARTICLE_EVERY = 8;
    private static final int MAX_PARTICLE_SPAWNS = 24;

    public GreekFireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        if (!isTargetBush(level.getBlockState(pos).getBlock())) {
            return InteractionResult.PASS;
        }

        int burned = burnBushCluster(level, pos);
        if (burned <= 0) {
            return InteractionResult.PASS;
        }

        level.playSound(null, pos, SoundEvents.FIRE_IGNITE, SoundSource.BLOCKS, 1.0F,
                0.9F + level.random.nextFloat() * 0.2F);
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().hurtAndBreak(1, player,
                    context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.CONSUME;
    }

    private int burnBushCluster(Level level, BlockPos origin) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin.asLong());
        int burned = 0;
        int particleBursts = 0;

        while (!queue.isEmpty() && burned < MAX_SPREAD) {
            BlockPos current = queue.poll();
            if (!isTargetBush(level.getBlockState(current).getBlock())) {
                continue;
            }

            level.removeBlock(current, false);
            burned++;
            if (burned % PARTICLE_EVERY == 0 && particleBursts < MAX_PARTICLE_SPAWNS) {
                spawnSmoke(level, current);
                particleBursts++;
            }

            for (BlockPos next : BlockPos.betweenClosed(
                    current.offset(-LINK_RADIUS, -LINK_RADIUS, -LINK_RADIUS),
                    current.offset(LINK_RADIUS, LINK_RADIUS, LINK_RADIUS))) {
                if (next.equals(current)) {
                    continue;
                }
                if (visited.add(next.asLong()) && isTargetBush(level.getBlockState(next).getBlock())) {
                    queue.add(next.immutable());
                }
            }
        }

        if (burned > 0 && particleBursts == 0) {
            spawnSmoke(level, origin);
        }
        return burned;
    }

    private static void spawnSmoke(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 4, 0.25, 0.25, 0.25, 0.01);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private static boolean isTargetBush(Block block) {
        return block == ModBlocks.INFESTED_REMAINS.get() || block == ModBlocks.RESIDUE_PLANTS.get();
    }
}
