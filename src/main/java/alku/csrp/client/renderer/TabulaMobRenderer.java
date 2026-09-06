package alku.csrp.client.renderer;

import alku.csrp.client.model.tabula.ModelSRP;
import alku.csrp.client.model.tabula.TabulaModelRegistry;
import alku.csrp.client.model.tabula.TabulaTextureResolver;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.CarrierEntity;
import alku.csrp.entity.MeltableAssimilated;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.registry.ModMobEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Vanilla entity renderer for a Citadel-backed Tabula model.
 *
 * <p>Unlike a GeckoLib renderer this class never resolves a geo or animation
 * JSON file. {@link ModelSRP#setupAnim} invokes the Java animation code ported
 * from SRParasites 1.10.8 directly each frame.</p>
 */
public class TabulaMobRenderer<T extends Mob> extends MobRenderer<T, EntityModel<T>> {
    private final String modelId;

    public TabulaMobRenderer(EntityRendererProvider.Context context, String modelId, float shadowRadius) {
        super(context, model(modelId), shadowRadius);
        this.modelId = modelId;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mob> EntityModel<T> model(String id) {
        return (EntityModel<T>) (EntityModel<?>) TabulaModelRegistry.create(id);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.hasEffect(ModMobEffects.BRAINING.get())) {
            return false;
        }
        return super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (entity instanceof AssimilatedParasiteEntity assimilated) {
            return assimilated.getTextureResource();
        }
        return TabulaTextureResolver.resolve(entity, modelId);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight) {
        applyModelState(entity);
        poseStack.pushPose();
        applyScale(entity, poseStack, partialTick);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    /** Common SRP visual scale effects formerly applied in GeckoLib preRender. */
    protected void applyScale(T entity, PoseStack poseStack, float partialTick) {
        if (entity instanceof MeltableAssimilated meltable && meltable.isMelting()) {
            poseStack.scale(1.0F, meltable.getMeltRenderScale(partialTick), 1.0F);
        }
        if (entity instanceof CarrierEntity carrier) {
            float swell = Mth.clamp(carrier.getSwellProgress(partialTick), 0.0F, 1.0F);
            float pulse = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
            float eased = swell * swell;
            eased *= eased;
            float horizontalScale = (1.0F + eased * 0.4F) * pulse;
            float verticalScale = (1.0F + eased * 0.1F) / pulse;
            poseStack.scale(horizontalScale, verticalScale, horizontalScale);
        }
    }

    /** Apply optional part visibility used by multipart Tabula exports. */
    protected void applyModelState(T entity) {
        if (!(model instanceof ModelSRP<?> tabula)) {
            return;
        }
        if (entity instanceof alku.csrp.entity.AdaptedVariantEntity adapted) {
            String left;
            String right;
            switch (adapted.getKind()) {
                case LONGARMS, MANDUCATER, SUMMONER -> { left = "taclejointL1"; right = "taclejointR1"; }
                case REEKER -> { left = "taclejointL"; right = "taclejointR"; }
                case BOLSTER -> { left = "jointMLT0"; right = "jointMRT0"; }
                default -> { left = null; right = null; }
            }
            if (left != null) {
                tabula.setHidden(left, !adapted.isLeftTendrilAttached());
                tabula.setHidden(right, !adapted.isRightTendrilAttached());
            }
        }
        if (entity instanceof PrimitiveVariantEntity primitive && primitive.isPrimitiveYelloweye()) {
            // The glow is a separate render concern; keep the model's body visible
            // and leave the synchronized skin choice to the texture resolver.
            tabula.setHidden("mainbody", false);
        }
    }
}
