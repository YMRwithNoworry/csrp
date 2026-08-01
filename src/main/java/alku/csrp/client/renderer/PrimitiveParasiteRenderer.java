package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Set;

public final class PrimitiveParasiteRenderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T> {
    private static final Set<String> REVERSED_MODELS = Set.of(
            "bomber_light", "haunter", "warden", "wraith", "anc_dreadnaut_ten"
    );

    private final boolean reverseFacing;

    public PrimitiveParasiteRenderer(EntityRendererProvider.Context context, String id, float shadowRadius) {
        super(context, new PrimitiveParasiteModel<>(id));
        this.shadowRadius = shadowRadius;
        this.reverseFacing = REVERSED_MODELS.contains(id);
    }

    @Override
    public void preRender(PoseStack poseStack, T entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        if (reverseFacing) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
    }
}
