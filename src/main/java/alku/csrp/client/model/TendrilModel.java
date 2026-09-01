package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.TendrilEntity;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class TendrilModel extends CitadelModelSet<TendrilEntity> {
    private static final String[] KEYS = {
            "shyco", "shyco", "nogla", "canra", "bano", "esor", "anged", "dragonelw", "dragonerw"
    };
    private static final String[] TEXTURES = {
            "tendrilshyco.png", "tendrilshyco.png", "tendrilnogla.png", "tendrilcanra.png",
            "tendrilbano.png", "tendrilesor.png", "tendrilanged.png",
            "tendrildragonelw.png", "tendrildragonerw.png"
    };

    public TendrilModel() {
        super(Map.of(
                "shyco", new ModelSpec("tendril_shyco"),
                "nogla", new ModelSpec("tendril_nogla"),
                "canra", new ModelSpec("tendril_canra", "tendril_static"),
                "bano", new ModelSpec("tendril_bano"),
                "esor", new ModelSpec("marauder_tendril"),
                "anged", new ModelSpec("tendril_anged"),
                "dragonelw", new ModelSpec("tendril_dragonelw", "tendril_static"),
                "dragonerw", new ModelSpec("tendril_dragonerw", "tendril_static")),
                entity -> KEYS[entity.getSkin()],
                entity -> ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                        "textures/entity/monster/" + TEXTURES[entity.getSkin()]));
    }
}
