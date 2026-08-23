package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.TetheredMarauderizedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Draws the animated guardian-style beam used by Marauderized bear and enderman tether attacks. */
public final class TetheredMarauderizedRenderer<T extends TetheredMarauderizedEntity>
        extends ParasiteGeoRenderer<T> {
    private static final int TETHER_SEGMENTS = 24;
    private static final float TETHER_RADIUS = 0.045F;

    public TetheredMarauderizedRenderer(EntityRendererProvider.Context context, String id, float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void render(T parasite, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(parasite, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        LivingEntity target = parasite.getPullTargetForRendering();
        if (target == null) {
            return;
        }

        Vec3 renderOrigin = parasite.getPosition(partialTick);
        Vec3 start = parasite.getEyePosition(partialTick).subtract(renderOrigin);
        Vec3 end = target.getPosition(partialTick).add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(renderOrigin);
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.01D) {
            return;
        }

        Vec3 forward = direction.scale(1.0D / length);
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.0001D) {
            side = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 up = forward.cross(side).normalize();

        float age = parasite.tickCount + partialTick;
        float pulse = (Mth.sin(age * 0.5F) + 1.0F) * 0.5F;
        int red = 64 + (int) (pulse * 191.0F);
        int green = 32 + (int) (pulse * 191.0F);
        int blue = 128 + (int) (pulse * 64.0F);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Vec3 previous = start;
        for (int segment = 1; segment <= TETHER_SEGMENTS; segment++) {
            float progress = segment / (float) TETHER_SEGMENTS;
            Vec3 current = tetherPoint(start, end, side, up, age, progress);
            float radius = TETHER_RADIUS * (0.85F + 0.15F * Mth.sin(age * 0.35F + progress * 14.0F));
            renderRibbonSegment(pose, consumer, previous, current, side, up, radius, red, green, blue);
            previous = current;
        }
    }

    private static Vec3 tetherPoint(Vec3 start, Vec3 end, Vec3 side, Vec3 up, float age, float progress) {
        float taper = Mth.sin((float) Math.PI * progress);
        double lateral = Mth.sin(age * 0.55F + progress * 24.0F) * 0.075D * taper;
        double vertical = Mth.cos(age * 0.42F + progress * 17.0F) * 0.045D * taper;
        return start.lerp(end, progress).add(side.scale(lateral)).add(up.scale(vertical));
    }

    private static void renderRibbonSegment(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end,
                                            Vec3 side, Vec3 up, float radius, int red, int green, int blue) {
        Vec3 sideOffset = side.scale(radius);
        Vec3 upOffset = up.scale(radius);
        emitQuad(pose, consumer, start.add(sideOffset), end.add(sideOffset), end.subtract(sideOffset),
                start.subtract(sideOffset), red, green, blue);
        emitQuad(pose, consumer, start.add(upOffset), end.add(upOffset), end.subtract(upOffset),
                start.subtract(upOffset), red, green, blue);
    }

    private static void emitQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 first, Vec3 second,
                                 Vec3 third, Vec3 fourth, int red, int green, int blue) {
        addVertex(pose, consumer, first, red, green, blue, 220);
        addVertex(pose, consumer, second, red, green, blue, 185);
        addVertex(pose, consumer, third, red, green, blue, 185);
        addVertex(pose, consumer, fourth, red, green, blue, 220);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 position,
                                  int red, int green, int blue, int alpha) {
        consumer.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha).endVertex();
    }
}
