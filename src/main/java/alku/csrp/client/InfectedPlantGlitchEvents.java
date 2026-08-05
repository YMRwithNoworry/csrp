package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Applies the Godot "Glitch Double Vision" screen shader
 * (https://godotshaders.com/shader/glitch-double-vision/) while the local
 * player stands inside infected plant blocks.
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class InfectedPlantGlitchEvents {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "shaders/post/glitch_double_vision.json");

    private static PostChain loadedEffect;
    private static boolean loadAttempted;

    private InfectedPlantGlitchEvents() {
    }

    @SubscribeEvent
    public static void updateEffect(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldRender = isInsideInfectedPlant(minecraft);

        if (!shouldRender) {
            unloadEffect(minecraft);
            loadAttempted = false;
            return;
        }
        if (loadedEffect != null || loadAttempted
                || minecraft.gameRenderer.currentEffect() != null) {
            return;
        }

        loadAttempted = true;
        minecraft.gameRenderer.loadEffect(EFFECT);
        loadedEffect = minecraft.gameRenderer.currentEffect();
    }

    private static boolean isInsideInfectedPlant(Minecraft minecraft) {
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return false;
        }

        AABB playerBox = player.getBoundingBox();
        BlockPos min = BlockPos.containing(
                playerBox.minX - 1.0D, playerBox.minY - 1.0D, playerBox.minZ - 1.0D);
        BlockPos max = BlockPos.containing(
                playerBox.maxX + 1.0D, playerBox.maxY + 1.0D, playerBox.maxZ + 1.0D);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (!isInfectedPlant(state.getBlock())) {
                continue;
            }
            VoxelShape shape = state.getShape(level, pos);
            if (shape.isEmpty()) {
                if (playerBox.intersects(new AABB(pos))) {
                    return true;
                }
                continue;
            }
            if (shape.bounds().move(pos).intersects(playerBox)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInfectedPlant(Block block) {
        return block == ModBlocks.RESIDUE_PLANTS.get()
                || block == ModBlocks.THORNSHADE.get()
                || block == ModBlocks.ALVEOLI_GROWTH.get();
    }

    private static void unloadEffect(Minecraft minecraft) {
        if (loadedEffect == null) {
            return;
        }
        if (minecraft.gameRenderer.currentEffect() == loadedEffect) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loadedEffect = null;
    }
}
