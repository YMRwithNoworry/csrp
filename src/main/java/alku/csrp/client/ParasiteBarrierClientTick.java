package alku.csrp.client;

import alku.csrp.block.entity.ParasiteBarrierBlockEntity;
import alku.csrp.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ParasiteBarrierClientTick {
    private static final int VIEW_RANGE = 48;
    private static final int PARTICLE_STEP = 2;

    private ParasiteBarrierClientTick() {
    }

    public static void tick(ParasiteBarrierBlockEntity barrier, Level level) {
        Player player = Minecraft.getInstance().player;
        if (player == null || (!holdsBarrier(player.getMainHandItem()) && !holdsBarrier(player.getOffhandItem()))) {
            return;
        }

        int[] bounds = barrier.currentWorldBounds();
        int playerX = player.blockPosition().getX();
        int playerZ = player.blockPosition().getZ();
        int fromX = Math.max(bounds[0], playerX - VIEW_RANGE);
        int toX = Math.min(bounds[1], playerX + VIEW_RANGE);
        int fromZ = Math.max(bounds[2], playerZ - VIEW_RANGE);
        int toZ = Math.min(bounds[3], playerZ + VIEW_RANGE);
        double y = Math.max(2.0D, Math.min(level.getMaxY() - 2.0D, player.getY()));
        double minX = bounds[0] + 0.5D;
        double maxX = bounds[1] + 0.5D;
        double minZ = bounds[2] + 0.5D;
        double maxZ = bounds[3] + 0.5D;

        for (int z = fromZ; z <= toZ; z += PARTICLE_STEP) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()), minX, y, z + 0.5D, 0.0D, 0.0D, 0.0D);
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()), maxX, y, z + 0.5D, 0.0D, 0.0D, 0.0D);
        }
        for (int x = fromX; x <= toX; x += PARTICLE_STEP) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()), x + 0.5D, y, minZ, 0.0D, 0.0D, 0.0D);
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()), x + 0.5D, y, maxZ, 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean holdsBarrier(ItemStack stack) {
        return stack.is(ModBlocks.legacyBlock("parasite_barrier").get().asItem());
    }
}
