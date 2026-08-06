package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.SimHumanEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * SimHuman (特殊人形感染体) 的GeckoLib模型
 */
public final class SimHumanModel extends ParasiteGeoModel<SimHumanEntity> {
    @Override
    public ResourceLocation getModelResource(SimHumanEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "geo/sim_human.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SimHumanEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/sim_human.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SimHumanEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "animations/sim_human.animation.json");
    }
}
