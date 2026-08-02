package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.DerivedParasiteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/** Restores the translucent cosmical shadow pass used by legacy derived parasites. */
public final class DerivedParasiteRenderer<T extends DerivedParasiteEntity> extends ParasiteGeoRenderer<T> {
    private static final ResourceLocation COSMIC_HACKING_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/layer/cosmichasking.png");
    private static final ResourceLocation GUARDIAN_BEAM_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/guardian_beam.png");
    private static final RenderType GUARDIAN_BEAM_RENDER_TYPE = RenderType.entityTranslucentEmissive(
            GUARDIAN_BEAM_TEXTURE);
    private static final int BEAM_SIDES = 8;
    private static final float BEAM_RADIUS = 0.282F;
    private static final int BEAM_RED = 78;
    private static final int BEAM_GREEN = 156;
    private static final int BEAM_BLUE = 250;

    private final ResourceLocation shadowTexture;

    public DerivedParasiteRenderer(EntityRendererProvider.Context context, String id, String shadowTexture,
            float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowTexture = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                "textures/entity/" + shadowTexture + ".png");
        this.shadowRadius = shadowRadius;
        addRenderLayer(new ShadowLayer<>(this, this.shadowTexture));
        addRenderLayer(new CosmicHackingLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.isShadowClone() ? shadowTexture : super.getTextureLocation(entity);
    }

    @Override
    public RenderType getRenderType(T entity, ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        // Iris cannot reliably map NeoForge's unlit translucent shader used by the shared model.
        // The normal derived textures are binary-alpha, so keep the body on the vanilla entity
        // cutout path while reserving translucency for the actual shadow clone/effect passes.
        return entity.isShadowClone() ? RenderType.entityTranslucent(shadowTexture)
                : RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public Color getRenderColor(T entity, float partialTick, int packedLight) {
        return entity.isShadowClone() ? Color.ofRGBA(255, 255, 255, 128)
                : super.getRenderColor(entity, partialTick, packedLight);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (entity.isShadowClone()) {
            poseStack.scale(1.2F, 1.2F, 1.2F);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        if (entity.isShadowed() && !entity.isShadowClone()) {
            for (int targetId : entity.getNeuralTargetIds()) {
                Entity target = entity.level().getEntity(targetId);
                if (target instanceof LivingEntity living && living.isAlive()) {
                    renderNeuralBeam(entity, living, partialTick, poseStack, bufferSource);
                }
            }
        }
    }

    private static void renderNeuralBeam(DerivedParasiteEntity parasite, LivingEntity target, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource) {
        Vec3 renderOrigin = parasite.getPosition(partialTick);
        Vec3 start = parasite.getEyePosition(partialTick).subtract(renderOrigin);
        Vec3 end = target.getPosition(partialTick).add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(renderOrigin);
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        if (distance < 0.01D) {
            return;
        }

        Vec3 normalized = direction.scale(1.0D / distance);
        float beamLength = (float) distance + 1.0F;
        float pitch = (float) Math.acos(normalized.y);
        float yaw = (float) Math.atan2(normalized.z, normalized.x);
        float age = parasite.tickCount + partialTick;
        float textureOffset = age * 0.5F % 1.0F;
        float startV = -1.0F + textureOffset;
        float endV = beamLength * 2.5F + startV;

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        poseStack.mulPose(Axis.YP.rotationDegrees((Mth.HALF_PI - yaw) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * Mth.RAD_TO_DEG));

        VertexConsumer consumer = bufferSource.getBuffer(GUARDIAN_BEAM_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        float spin = age * -0.075F;
        for (int side = 0; side < BEAM_SIDES; side++) {
            float progress = side / (float) BEAM_SIDES;
            float nextProgress = (side + 1) / (float) BEAM_SIDES;
            float angle = spin + progress * Mth.TWO_PI;
            float nextAngle = spin + nextProgress * Mth.TWO_PI;
            float x = Mth.cos(angle) * BEAM_RADIUS;
            float z = Mth.sin(angle) * BEAM_RADIUS;
            float nextX = Mth.cos(nextAngle) * BEAM_RADIUS;
            float nextZ = Mth.sin(nextAngle) * BEAM_RADIUS;

            beamVertex(consumer, pose, x, beamLength, z, progress, endV);
            beamVertex(consumer, pose, x, 0.0F, z, progress, startV);
            beamVertex(consumer, pose, nextX, 0.0F, nextZ, nextProgress, startV);
            beamVertex(consumer, pose, nextX, beamLength, nextZ, nextProgress, endV);
        }
        poseStack.popPose();
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(BEAM_RED, BEAM_GREEN, BEAM_BLUE, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static final class ShadowLayer<T extends DerivedParasiteEntity> extends GeoRenderLayer<T> {
        private final ResourceLocation texture;

        private ShadowLayer(DerivedParasiteRenderer<T> renderer, ResourceLocation texture) {
            super(renderer);
            this.texture = texture;
        }

        @Override
        public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType,
                MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                int packedOverlay) {
            float alpha = entity.getShadowRenderAlpha(partialTick);
            if (entity.isShadowClone() || !entity.isShadowed() || alpha <= 0.0F) {
                return;
            }

            RenderType shadowRenderType = RenderType.entityTranslucent(texture);
            int alphaByte = Math.min(255, Math.max(0, Math.round(alpha * 255.0F)));
            int colour = alphaByte << 24 | 0xFFFFFF;
            poseStack.pushPose();
            poseStack.scale(1.2F, 1.2F, 1.2F);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, shadowRenderType,
                    bufferSource.getBuffer(shadowRenderType), partialTick, packedLight, packedOverlay, colour);
            poseStack.popPose();
        }
    }

    private static final class CosmicHackingLayer<T extends DerivedParasiteEntity> extends GeoRenderLayer<T> {
        private CosmicHackingLayer(DerivedParasiteRenderer<T> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, RenderType renderType,
                MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                int packedOverlay) {
            if (!entity.isShadowed() || entity.isShadowClone() || !entity.isNeuralLinkActive()) {
                return;
            }

            float age = entity.tickCount + partialTick;
            RenderType hackingRenderType = RenderType.energySwirl(COSMIC_HACKING_TEXTURE,
                    age * 0.01F, age * 0.01F);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, entity, hackingRenderType,
                    bufferSource.getBuffer(hackingRenderType), partialTick, LightTexture.FULL_BRIGHT,
                    packedOverlay, 0xFFFF80FF);
        }
    }
}
