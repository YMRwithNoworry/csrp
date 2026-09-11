package alku.nocubessrparmory;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only renderers. {@link ThrownItemRenderer} uses the entity's
 * {@code ItemSupplier#getItem()} stack, which reproduces the original
 * RenderSnowball icon choice (invisible projectile, bomb icon, core icon).
 */
@EventBusSubscriber(modid = NoCubesSrpCombatAddon.MOD_ID, value = Dist.CLIENT)
public final class AddonClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (ArmoryLauncherItem.Kind kind : ArmoryLauncherItem.Kind.values()) {
            event.registerEntityRenderer(ArmoryEntities.get(kind),
                    context -> new ThrownItemRenderer<>(context, 1.0F, true));
        }
    }

    private AddonClientEvents() {}
}
