package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.client.model.tabula.ModelSRP;
import alku.csrp.client.model.tabula.generated.ModelTabula_biomass_pod;
import alku.csrp.client.model.tabula.generated.ModelTabula_biomass_venkrol;
import alku.csrp.entity.BiomassEntity;
import alku.csrp.registry.ModMobEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Citadel renderer for the two original SRP biomass Tabula models. */
public final class TabulaBiomassRenderer extends MobRenderer<BiomassEntity, EntityModel<BiomassEntity>> {
    private static final ResourceLocation VENKROL_TEXTURE = new ResourceLocation(
            Csrp.MODID, "textures/entity/biomass_venkrol.png");
    private static final ResourceLocation POD_TEXTURE = new ResourceLocation(
            Csrp.MODID, "textures/entity/biomass_pod.png");

    private final ModelSRP<BiomassEntity> venkrolModel = new ModelTabula_biomass_venkrol();
    private final ModelSRP<BiomassEntity> podModel = new ModelTabula_biomass_pod();

    public TabulaBiomassRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelTabula_biomass_venkrol(), 0.5F);
    }

    @Override
    public boolean shouldRender(BiomassEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        var player = Minecraft.getInstance().player;
        return (player == null || !player.hasEffect(ModMobEffects.BRAINING.get()))
                && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(BiomassEntity entity) {
        return entity.getSkin() <= 3 ? VENKROL_TEXTURE : POD_TEXTURE;
    }

    @Override
    public void render(BiomassEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        model = entity.getSkin() <= 3 ? venkrolModel : podModel;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
