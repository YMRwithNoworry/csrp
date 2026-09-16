package alku.csrp.client.renderer;

import alku.csrp.entity.DragonEggAssimilationEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;

public final class DragonEggAssimilationRenderer extends EntityRenderer<DragonEggAssimilationEntity, DragonEggAssimilationRenderer.State> {
    public DragonEggAssimilationRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float ticks = state.ticks;
        float progress = Mth.clamp(ticks / DragonEggAssimilationEntity.ANIMATION_DURATION, 0.0F, 1.0F);
        float shake = progress * progress * 0.12F;
        float lift = Mth.sin(progress * Mth.PI) * 0.8F;
        float scale = 1.0F + Mth.sin(progress * Mth.PI) * 0.35F;

        poseStack.pushPose();
        poseStack.translate(Mth.sin(ticks * 2.7F) * shake, lift, Mth.cos(ticks * 3.1F) * shake);
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, ticks * progress * 4.0F);
        poseStack.rotateDegrees(Axis.ZP, Mth.sin(ticks * 1.7F) * progress * 8.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        if (state.movingBlock.blockState.getRenderShape() == RenderShape.MODEL) {
            submitNodeCollector.submitMovingBlock(poseStack, state.movingBlock, state.outlineColor);
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public void extractRenderState(DragonEggAssimilationEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ticks = entity.getAnimationTicks() + partialTick;
        BlockPos pos = entity.blockPosition();
        state.movingBlock.blockPos = pos;
        state.movingBlock.blockState = Blocks.DRAGON_EGG.defaultBlockState();
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.movingBlock.biome = clientLevel.getBiome(pos);
            state.movingBlock.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlock.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    public static final class State extends EntityRenderState {
        public float ticks;
        public final MovingBlockRenderState movingBlock = new MovingBlockRenderState();
    }
}
