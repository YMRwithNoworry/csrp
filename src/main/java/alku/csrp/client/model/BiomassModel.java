package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.BiomassEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public final class BiomassModel extends GeoModel<BiomassEntity> {
    private static final ResourceLocation POD_MODEL = new ResourceLocation(
            Csrp.MODID, "geo/biomass_pod.geo.json");
    private static final ResourceLocation VENKROL_MODEL = new ResourceLocation(
            Csrp.MODID, "geo/biomass_venkrol.geo.json");
    private static final ResourceLocation POD_TEXTURE = new ResourceLocation(
            Csrp.MODID, "textures/entity/biomass_pod.png");
    private static final ResourceLocation VENKROL_TEXTURE = new ResourceLocation(
            Csrp.MODID, "textures/entity/biomass_venkrol.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            Csrp.MODID, "animations/biomass.animation.json");

    @Override
    public ResourceLocation getModelResource(BiomassEntity animatable) {
        return animatable.getSkin() <= 3 ? VENKROL_MODEL : POD_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BiomassEntity animatable) {
        return animatable.getSkin() <= 3 ? VENKROL_TEXTURE : POD_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BiomassEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(BiomassEntity animatable, long instanceId,
                                    AnimationState<BiomassEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        int skin = animatable.getSkin();
        setVisible("mainbodysi", skin == 1);
        setVisible("mainbodysii", skin == 2);
        setVisible("mainbodysiii", skin == 3);
        setVisible("alafha", skin == 4);
        setVisible("pri_sum", skin == 5);
        setVisible("ada_sum", skin == 6);
        String rootName = skin <= 3
                ? switch (skin) {
                    case 1 -> "mainbodysi";
                    case 2 -> "mainbodysii";
                    default -> "mainbodysiii";
                }
                : switch (skin) {
                    case 4 -> "alafha";
                    case 5 -> "pri_sum";
                    default -> "ada_sum";
                };
        float partialTick = animationState.getPartialTick();
        float pulse = 1.4F + Mth.sin((animatable.tickCount + partialTick) * 0.8F) * 0.05F;
        float width = pulse + animatable.getGrowthWidth(partialTick);
        float height = pulse + animatable.getGrowthHeight(partialTick);
        getBone(rootName).ifPresent(root -> applyGrowthScale(root, width, height));
    }

    private static void applyGrowthScale(GeoBone root, float width, float height) {
        root.setScaleX(width);
        root.setScaleY(height);
        root.setScaleZ(width);
    }

    private void setVisible(String boneName, boolean visible) {
        getBone(boneName).ifPresent(bone -> bone.setHidden(!visible));
    }
}
