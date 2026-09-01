package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.MarauderEntity;
import net.minecraft.resources.ResourceLocation;

/** Original Esor model with broken tendrils hidden. */
public final class MarauderModel extends CitadelParasiteModel<MarauderEntity> {
    private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/marauder.png");
    private static final ResourceLocation HARDENED = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/marauder_hardened.png");

    public MarauderModel() {
        super("marauder");
    }

    @Override
    public ResourceLocation texture(MarauderEntity entity) {
        return entity.isHardenedVariant() ? HARDENED : NORMAL;
    }

    @Override
    protected void customize(MarauderEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        getBone("taclejointLA0").ifPresent(bone -> bone.showModel = entity.isLeftTendrilAttached());
        getBone("taclejointRA0").ifPresent(bone -> bone.showModel = entity.isRightTendrilAttached());
    }
}
