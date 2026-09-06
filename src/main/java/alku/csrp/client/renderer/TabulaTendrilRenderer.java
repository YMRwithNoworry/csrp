package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.tabula.TabulaModelRegistry;
import alku.csrp.entity.TendrilEntity;
import alku.csrp.registry.ModMobEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Citadel renderer for the detached original Tabula tendril variants. */
public final class TabulaTendrilRenderer extends MobRenderer<TendrilEntity, EntityModel<TendrilEntity>> {
    private static final String[] MODEL_IDS = {
            "tendril_shyco", "tendril_shyco", "tendril_nogla", "tendril_canra",
            "tendril_bano", "marauder_tendril", "tendril_anged",
            "tendril_dragonelw", "tendril_dragonerw"
    };
    private static final ResourceLocation[] TEXTURES = {
            texture("tendrilshyco.png"), texture("tendrilshyco.png"), texture("tendrilnogla.png"),
            texture("tendrilcanra.png"), texture("tendrilbano.png"), texture("tendrilesor.png"),
            texture("tendrilanged.png"), texture("tendrildragonelw.png"), texture("tendrildragonerw.png")
    };

    private final EntityModel<TendrilEntity>[] models;

    @SuppressWarnings("unchecked")
    public TabulaTendrilRenderer(EntityRendererProvider.Context context) {
        super(context, model(MODEL_IDS[TendrilEntity.SHYCO]), 0.3F);
        models = new EntityModel[MODEL_IDS.length];
        for (int index = 0; index < MODEL_IDS.length; index++) {
            models[index] = model(MODEL_IDS[index]);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityModel<TendrilEntity> model(String id) {
        return (EntityModel<TendrilEntity>) (EntityModel<?>) TabulaModelRegistry.create(id);
    }

    private static ResourceLocation texture(String file) {
        return new ResourceLocation(Csrp.MODID, "textures/entity/monster/" + file);
    }

    private static int skin(TendrilEntity entity) {
        return Mth.clamp(entity.getSkin(), TendrilEntity.SHYCO, TendrilEntity.DRAGON_RIGHT_WING);
    }

    @Override
    public boolean shouldRender(TendrilEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        var player = Minecraft.getInstance().player;
        return (player == null || !player.hasEffect(ModMobEffects.BRAINING.get()))
                && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(TendrilEntity entity) {
        return TEXTURES[skin(entity)];
    }

    @Override
    public void render(TendrilEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        model = models[skin(entity)];
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
