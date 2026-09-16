package alku.csrp.client.model;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

/** Carries the live entity so the legacy Tabula animation code can keep reading entity state. */
public class LegacyMobRenderState extends LivingEntityRenderState {
    public LivingEntity legacyEntity;
}
