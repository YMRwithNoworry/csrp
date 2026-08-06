package alku.csrp.client.renderer;

import alku.csrp.client.model.SimHumanModel;
import alku.csrp.entity.SimHumanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * SimHuman (特殊人形感染体) 的渲染器
 */
public final class SimHumanRenderer extends ParasiteGeoRenderer<SimHumanEntity> {
    public SimHumanRenderer(EntityRendererProvider.Context context) {
        super(context, new SimHumanModel());
        this.shadowRadius = 0.6F;
    }
}
