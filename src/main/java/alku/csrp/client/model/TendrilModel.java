package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.TendrilEntity;
import net.minecraft.resources.ResourceLocation;

public final class TendrilModel extends ParasiteGeoModel<TendrilEntity> {
    private static final String[] MODEL_IDS = {
            "tendril_shyco",
            "tendril_shyco",
            "tendril_nogla",
            "tendril_canra",
            "tendril_bano",
            "marauder_tendril",
            "tendril_anged",
            "tendril_dragonelw",
            "tendril_dragonerw"
    };
    private static final String[] TEXTURE_IDS = {
            "tendrilshyco.png",
            "tendrilshyco.png",
            "tendrilnogla.png",
            "tendrilcanra.png",
            "tendrilbano.png",
            "tendrilesor.png",
            "tendrilanged.png",
            "tendrildragonelw.png",
            "tendrildragonerw.png"
    };

    @Override
    public ResourceLocation getModelResource(TendrilEntity animatable) {
        return resource("geo/" + MODEL_IDS[animatable.getSkin()] + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TendrilEntity animatable) {
        return resource("textures/entity/monster/" + TEXTURE_IDS[animatable.getSkin()]);
    }

    @Override
    public ResourceLocation getAnimationResource(TendrilEntity animatable) {
        String animation = switch (animatable.getSkin()) {
            case TendrilEntity.SHYCO -> "tendril_shyco";
            case TendrilEntity.NOGLA -> "tendril_nogla";
            case TendrilEntity.BANO -> "tendril_bano";
            case TendrilEntity.ESOR -> "marauder_tendril";
            case TendrilEntity.ANGED -> "tendril_anged";
            default -> "tendril_static";
        };
        return resource("animations/" + animation + ".animation.json");
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path);
    }
}
