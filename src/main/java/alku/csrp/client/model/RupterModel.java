package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.RupterEntity;
import net.minecraft.resources.Identifier;

public final class RupterModel extends CitadelParasiteModel<RupterEntity> {
    public RupterModel() {
        super("rupter");
    }

    @Override
    public Identifier texture(RupterEntity entity) {
        RupterEntity.BehaviorVariant behavior = entity.getBehaviorVariant();
        String suffix = behavior == RupterEntity.BehaviorVariant.NORMAL
                ? entity.getTextureVariant().suffix() : behavior.suffix();
        return Identifier.fromNamespaceAndPath(Csrp.MODID,
                "textures/entity/rupter" + suffix + ".png");
    }
}
