package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.BiomassEntity;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class BiomassModel extends CitadelModelSet<BiomassEntity> {
    private static final ResourceLocation POD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/biomass_pod.png");
    private static final ResourceLocation VENKROL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/biomass_venkrol.png");

    public BiomassModel() {
        super(Map.of(
                "venkrol", new ModelSpec("biomass_venkrol", "biomass"),
                "pod", new ModelSpec("biomass_pod", "biomass")),
                entity -> entity.getSkin() <= 3 ? "venkrol" : "pod",
                entity -> entity.getSkin() <= 3 ? VENKROL_TEXTURE : POD_TEXTURE);
    }

    @Override
    protected void customize(BiomassEntity entity, CitadelParasiteModel<BiomassEntity> model,
            float ageInTicks) {
        int skin = entity.getSkin();
        setVisible(model, "mainbodysi", skin == 1);
        setVisible(model, "mainbodysii", skin == 2);
        setVisible(model, "mainbodysiii", skin == 3);
        setVisible(model, "alafha", skin == 4);
        setVisible(model, "pri_sum", skin == 5);
        setVisible(model, "ada_sum", skin == 6);
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
        float partialTick = ageInTicks - entity.tickCount;
        float pulse = 1.4F + Mth.sin(ageInTicks * 0.8F) * 0.05F;
        float width = pulse + entity.getGrowthWidth(partialTick);
        float height = pulse + entity.getGrowthHeight(partialTick);
        var root = model.findPart(rootName);
        if (root != null) {
            root.setScale(width, height, width);
        }
    }

    private static void setVisible(CitadelParasiteModel<BiomassEntity> model,
            String name, boolean visible) {
        var part = model.findPart(name);
        if (part != null) {
            part.showModel = visible;
        }
    }
}
