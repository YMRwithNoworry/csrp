package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Replaces Kirin's opaque no-vision overlay with a readable VHS post effect. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class KirinVhsEffectEvents {
    private static final Identifier EFFECT = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "shaders/post/kirin_vhs.json");

    private KirinVhsEffectEvents() {
    }

    @SubscribeEvent
    public static void updateEffect(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        boolean shouldRender = minecraft.player.hasEffect(ModMobEffects.NOVISION);
        List<Identifier> active = minecraft.player.getActivePostEffects();
        if (shouldRender && !active.contains(EFFECT)) {
            minecraft.player.setActivePostEffects(List.of(EFFECT));
        } else if (!shouldRender && active.contains(EFFECT)) {
            minecraft.player.setActivePostEffects(List.of());
        }
    }
}
