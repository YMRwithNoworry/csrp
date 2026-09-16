package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.MeteorModelParts;
import alku.csrp.client.model.MeteorModelParts.Part;
import alku.csrp.entity.MeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * Port of the 1.10.8 {@code RenderMeteor} / {@code ModelMeteor}. The original model is a 580x580
 * texture box model with 92 parts; {@link MeteorModelParts} carries the exact box data and this
 * renderer reproduces the vanilla {@code ModelRenderer} transform chain plus the original idle
 * tentacle/body animation.
 */
public final class HiveSatelliteRenderer
        extends EntityRenderer<MeteorEntity, HiveSatelliteRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/meteor.png");
    private static final float TEXTURE_SIZE = 580.0F;
    private static final float SCALE = 0.0625F;
    private static final Map<String, Part> PARTS = new HashMap<>();
    private static final Map<String, List<Part>> CHILDREN = new HashMap<>();

    static {
        for (Part part : MeteorModelParts.PARTS) {
            PARTS.put(part.name(), part);
        }
        for (Part part : MeteorModelParts.PARTS) {
            if (part.parent() != null) {
                CHILDREN.computeIfAbsent(part.parent(), key -> new ArrayList<>()).add(part);
            }
        }
    }

    public HiveSatelliteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MeteorEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.root = entity.isRoot();
    }

    @Override
    public boolean shouldRender(MeteorEntity entity, Frustum camera, double camX, double camY, double camZ,
            float partialTicks) {
        return true;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        Part root = PARTS.get("mainbody");
        if (root == null) {
            return;
        }
        float age = state.ageInTicks;

        poseStack.pushPose();
        // RenderMeteor.applyRotations: the original renderYawOffset is always 0.
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        // RenderMeteor.prepareScaleCosmical.
        float f = 1.0F;
        float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
        f = Mth.clamp(f, 0.0F, 1.1F);
        f *= f;
        f *= f;
        float f2 = (1.0F + f * 0.4F) * f1;
        float f3 = (1.0F + f * 0.1F) / f1;
        float plus = state.root ? 0.5F : -0.8F;
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(plus + f2, plus + f3, plus + f2);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        poseStack.scale(SCALE, SCALE, SCALE);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, vertices) -> {
            PoseStack local = new PoseStack();
            local.last().set(pose);
            renderPart(local, vertices, root, age);
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private void renderPart(PoseStack poseStack, VertexConsumer vertices, Part part, float age) {
        poseStack.pushPose();
        // ModelRenderer.render: offset, then rotation point, then Z/Y/X rotations.
        poseStack.translate(0.0F, animatedOffsetY(part.name(), age), 0.0F);
        poseStack.translate(part.rpx(), part.rpy(), part.rpz());
        float rx = animatedRotateX(part, age);
        if (part.rz() != 0.0F) {
            poseStack.rotate(Axis.ZP, part.rz());
        }
        if (part.ry() != 0.0F) {
            poseStack.rotate(Axis.YP, part.ry());
        }
        if (rx != 0.0F) {
            poseStack.rotate(Axis.XP, rx);
        }
        renderBox(poseStack, vertices, part);
        List<Part> children = CHILDREN.get(part.name());
        if (children != null) {
            for (Part child : children) {
                renderPart(poseStack, vertices, child, age);
            }
        }
        poseStack.popPose();
    }

    /** Exact port of the ModelBox UV layout used by the 1.10.8 model. */
    private static void renderBox(PoseStack poseStack, VertexConsumer vertices, Part part) {
        float x0 = part.ox();
        float y0 = part.oy();
        float z0 = part.oz();
        float x1 = x0 + part.w();
        float y1 = y0 + part.h();
        float z1 = z0 + part.d();
        float u = part.u();
        float v = part.v();
        float w = part.w();
        float h = part.h();
        float d = part.d();
        PoseStack.Pose pose = poseStack.last();

        // +X
        quad(pose, vertices, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1,
                u + d + w, v + d, u + d + w + d, v + d + h, 1.0F, 0.0F, 0.0F);
        // -X
        quad(pose, vertices, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0,
                u, v + d, u + d, v + d + h, -1.0F, 0.0F, 0.0F);
        // -Y
        quad(pose, vertices, x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0,
                u + d, v, u + d + w, v + d, 0.0F, -1.0F, 0.0F);
        // +Y (V is reversed in the original ModelBox)
        quad(pose, vertices, x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1,
                u + d + w, v + d, u + d + w + w, v, 0.0F, 1.0F, 0.0F);
        // -Z
        quad(pose, vertices, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
                u + d, v + d, u + d + w, v + d + h, 0.0F, 0.0F, -1.0F);
        // +Z
        quad(pose, vertices, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
                u + d + w + d, v + d, u + d + w + d + w, v + d + h, 0.0F, 0.0F, 1.0F);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vertices,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float u1, float v1, float u2, float v2, float nx, float ny, float nz) {
        vertex(pose, vertices, x0, y0, z0, u2, v1, nx, ny, nz);
        vertex(pose, vertices, x1, y1, z1, u1, v1, nx, ny, nz);
        vertex(pose, vertices, x2, y2, z2, u1, v2, nx, ny, nz);
        vertex(pose, vertices, x3, y3, z3, u2, v2, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices,
            float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vertices.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u / TEXTURE_SIZE, v / TEXTURE_SIZE)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }

    /** ModelMeteor.setRotationAngles: the tentacle joints sway with three cosine waves. */
    private static float animatedRotateX(Part part, float age) {
        float f1 = Mth.cos(age * 0.21109599F) * 0.5164299F;
        float f2 = Mth.cos(age * 0.10975879F) * 0.722022F;
        float f3 = Mth.cos(age * 0.15110986F) * 0.61975884F;
        return switch (part.name()) {
            case "taclejointJ" -> -f2;
            case "taclejointJ_1" -> f2;
            case "taclejointJ_2" -> -f1;
            case "taclejointJ_3" -> f3;
            case "taclejointJ_4" -> f2;
            case "taclejointJ_5" -> -f3;
            case "taclejointC" -> f3;
            case "taclejointC_1" -> -f1;
            case "taclejointC_2" -> f3;
            case "taclejointC_3" -> -f2;
            case "taclejointC_4" -> -f1;
            case "taclejointC_5" -> f2;
            case "taclejointH" -> f1;
            case "taclejointH_1" -> -f2;
            case "taclejointH_2" -> -f3;
            case "taclejointH_3" -> f2;
            case "taclejointH_4" -> -f1;
            case "taclejointH_5" -> f2;
            case "taclejointE" -> f1;
            case "taclejointE_1" -> -f2;
            case "taclejointE_2" -> f1;
            case "taclejointE_3" -> -f3;
            case "taclejointE_4" -> f1;
            case "taclejointE_5" -> -f2;
            case "taclejointF" -> -f3;
            case "taclejointF_1" -> -f2;
            case "taclejointF_2" -> f1;
            case "taclejointF_3" -> -f3;
            case "taclejointF_4" -> f2;
            case "taclejointF_5" -> f2;
            default -> part.rx();
        };
    }

    /** ModelMeteor.setRotationAngles: body plates bob vertically. */
    private static float animatedOffsetY(String name, float age) {
        float f1 = Mth.cos(age * 0.15986F) * 0.18429872F;
        float f2 = Mth.cos(age * 0.186F) * 0.249872F;
        float f3 = Mth.cos(age * 0.13986F) * 0.218872F;
        float f4 = Mth.cos(age * 0.143096F) * 0.1429872F;
        float f5 = Mth.cos(age * 0.119758785F) * 0.20872F;
        float f6 = Mth.cos(age * 0.13986F) * 0.21975887F;
        return switch (name) {
            case "bm" -> f1 * 0.5F;
            case "b1" -> f2;
            case "b2" -> f3;
            case "b3" -> f4;
            case "b4" -> f6;
            case "b5" -> f5;
            case "b6" -> f5;
            case "b8" -> f6;
            case "b9" -> f2;
            case "b10" -> f3;
            default -> 0.0F;
        };
    }

    public static final class State extends EntityRenderState {
        public boolean root;
    }
}
