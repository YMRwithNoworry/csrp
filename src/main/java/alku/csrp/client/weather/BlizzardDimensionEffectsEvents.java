package alku.csrp.client.weather;

import alku.csrp.Csrp;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers {@link BlizzardDimensionEffects} for {@code minecraft:overworld} on the mod bus.
 *
 * <p>This replaces 1.10.9's {@code MixinEntityRendererBlizzard} and {@code MixinRenderGlobalBlizzardSky}
 * without a single mixin: the overworld dimension effect is the only supported place to shorten the fog,
 * hide the sunrise colours and take over precipitation in 1.20.1.
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BlizzardDimensionEffectsEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlizzardDimensionEffectsEvents.class);
    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");

    private BlizzardDimensionEffectsEvents() {
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(OVERWORLD, new BlizzardDimensionEffects());
        LOGGER.info("Registered cold star blizzard overworld dimension effects for {}", OVERWORLD);
    }
}
