package alku.csrp.client.renderer;

import alku.csrp.entity.KirinSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 麒麟斩击渲染（原版 RenderKirinSlash 为程序化光刃，无贴图）：
 * 沿斩击方向绘制两片交叉渐变光刃，长度随生长值展开，淡出期透明。
 */
public class KirinSlashRenderer extends EntityRenderer<KirinSlashEntity, KirinSlashRenderer.KirinSlashRenderState> {
    private static final float BLADE_HALF_WIDTH = 1.1F;
    private static final float FADE_IN_TICKS = 5.0F;
    private static final float FADE_OUT_TICKS = 18.0F;

    public KirinSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public KirinSlashRenderState createRenderState() {
        return new KirinSlashRenderState();
    }

    @Override
    public void extractRenderState(KirinSlashEntity entity, KirinSlashRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tickCount = entity.tickCount;
        state.delayTicks = entity.getDelayTicks();
        state.growth = entity.getGrowth(partialTicks);
        state.life = entity.getLife();
        state.fading = entity.isFading();
        state.hitPopping = entity.isHitPopping();
        state.hitPopAge = entity.getHitPopAge();
        state.hitPopTicks = entity.getHitPopTicks();
        state.length = entity.getLength();
        state.yaw = entity.getYaw();
        state.pitch = entity.getPitch();
        state.roll = entity.getRoll();
    }

    @Override
    public void submit(KirinSlashRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        float visibleAge = state.tickCount + state.partialTick - state.delayTicks;
        if (visibleAge <= 0.0F) {
            return;
        }
        float growth = state.growth;
        if (growth <= 0.0F) {
            return;
        }
        float life = Math.max(1.0F, state.life);
        float fadeIn = Math.min(visibleAge / FADE_IN_TICKS, 1.0F);
        float fadeOut = 1.0F;
        if (visibleAge > life - FADE_OUT_TICKS) {
            fadeOut = 1.0F - Math.min((visibleAge - (life - FADE_OUT_TICKS)) / FADE_OUT_TICKS, 1.0F);
        }
        float alpha = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0F, 1.0F);
        float hitPopScale = 1.0F;
        float hitPopWidthScale = 1.0F;
        float hitPopAlphaBoost = 1.0F;
        if (state.hitPopping) {
            float progress = Mth.clamp((state.hitPopAge + state.partialTick)
                    / (float) Math.max(1, state.hitPopTicks), 0.0F, 1.0F);
            hitPopScale = 1.0F - progress * 0.82F;
            hitPopWidthScale = 1.0F - progress * 0.92F;
            float flash = 1.0F - Math.abs(progress - 0.18F) / 0.18F;
            hitPopAlphaBoost = (1.0F - progress) * (1.0F + Mth.clamp(flash, 0.0F, 1.0F) * 1.8F);
        }
        alpha *= hitPopAlphaBoost;
        if (state.fading) {
            alpha *= 0.35F;
        }
        if (alpha <= 0.01F || hitPopScale <= 0.02F || hitPopWidthScale <= 0.02F) {
            return;
        }
        float length = state.length * growth * hitPopScale;
        RenderType type = RenderTypes.lightning();
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
        poseStack.rotate(Axis.XP.rotationDegrees(state.pitch));
        poseStack.rotate(Axis.ZP.rotationDegrees(state.roll));

        submitCrossLayer(poseStack, submitNodeCollector, type, length, 0.52F * hitPopWidthScale,
                0.42F, 0.86F, 1.0F, 0.13F * alpha);
        submitCrossLayer(poseStack, submitNodeCollector, type, length, 0.20F * hitPopWidthScale,
                0.42F, 0.86F, 1.0F, 0.42F * alpha);
        submitCrossLayer(poseStack, submitNodeCollector, type, length, 0.055F * hitPopWidthScale,
                1.0F, 1.0F, 1.0F, 0.90F * alpha);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void submitCrossLayer(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            RenderType type, float length, float width, float red, float green, float blue, float alpha) {
        for (int blade = 0; blade < 2; blade++) {
            poseStack.pushPose();
            poseStack.rotate(Axis.ZP.rotationDegrees(90.0F * blade));
            submitNodeCollector.submitCustomGeometry(poseStack, type, (pose, consumer) ->
                    drawForwardPlane(pose, consumer, width, length, red, green, blue, alpha));
            poseStack.popPose();
        }
    }

    private static void drawForwardPlane(PoseStack.Pose pose, VertexConsumer consumer, float width, float length,
            float red, float green, float blue, float alpha) {
        float halfWidth = width * 0.5F;
        int segments = 18;
        for (int index = 0; index < segments; index++) {
            float t0 = (float) index / segments;
            float t1 = (float) (index + 1) / segments;
            float z0 = length * t0;
            float z1 = length * t1;
            float a0 = alpha * edgeFade(t0);
            float a1 = alpha * edgeFade(t1);
            vertex(consumer, pose, -halfWidth, 0.0F, z0, red, green, blue, a0);
            vertex(consumer, pose, halfWidth, 0.0F, z0, red, green, blue, a0);
            vertex(consumer, pose, halfWidth, 0.0F, z1, red, green, blue, a1);
            vertex(consumer, pose, -halfWidth, 0.0F, z1, red, green, blue, a1);
        }
    }

    private static float edgeFade(float position) {
        float fade = Mth.clamp(Math.min(position, 1.0F - position) * 2.0F, 0.0F, 1.0F);
        return fade * fade * (3.0F - 2.0F * fade);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float red, float green, float blue, float alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F));
    }

    public Identifier getTextureLocation(KirinSlashEntity entity) {
        return Identifier.withDefaultNamespace("textures/entity/kirin_slash.png");
    }

    @Override
    public boolean shouldRender(KirinSlashEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
            double camX, double camY, double camZ, float partialTicks) {
        Vec3 extent = entity.getSlashDirection().scale(entity.getLength());
        AABB slashBounds = entity.getBoundingBox().expandTowards(extent).inflate(BLADE_HALF_WIDTH);
        return super.shouldRender(entity, frustum, camX, camY, camZ, partialTicks)
                || frustum.isVisible(slashBounds);
    }

    /** Per-frame snapshot of the slash state needed to submit the procedural blades. */
    public static final class KirinSlashRenderState extends EntityRenderState {
        public float tickCount;
        public float delayTicks;
        public float growth;
        public float life;
        public boolean fading;
        public boolean hitPopping;
        public int hitPopAge;
        public int hitPopTicks;
        public float length;
        public float yaw;
        public float pitch;
        public float roll;
    }
}
