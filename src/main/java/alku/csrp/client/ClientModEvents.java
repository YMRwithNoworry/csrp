package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.client.renderer.BuglinRenderer;
import alku.csrp.client.renderer.RupterRenderer;
import alku.csrp.client.renderer.PrimitiveParasiteRenderer;
import alku.csrp.client.renderer.ScaryOrbRenderer;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Csrp.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BUGLIN.get(), BuglinRenderer::new);
        event.registerEntityRenderer(ModEntities.RUPTER.get(), RupterRenderer::new);
        event.registerEntityRenderer(ModEntities.PRI_LONGARMS.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_longarms", 0.65F));
        event.registerEntityRenderer(ModEntities.PRI_SUMMONER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_summoner", 0.7F));
        event.registerEntityRenderer(ModEntities.PRI_VERMIN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_vermin", 0.65F));
        event.registerEntityRenderer(ModEntities.PRI_VISCERA.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_viscera", 0.7F));
        event.registerEntityRenderer(ModEntities.GNAT.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "gnat", 0.25F));
        event.registerEntityRenderer(ModEntities.CARRIER_HEAVY.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "carrier_heavy", 0.8F));
        event.registerEntityRenderer(ModEntities.CARRIER_LIGHT.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "carrier_light", 0.55F));
        event.registerEntityRenderer(ModEntities.CARRIER_FLYING.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "carrier_flying", 0.7F));
        event.registerEntityRenderer(ModEntities.CRUX.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "crux", 0.7F));
        event.registerEntityRenderer(ModEntities.CRUX_INCOMPLETE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "crux_incomplete", 0.45F));
        event.registerEntityRenderer(ModEntities.SCARY_ORB.get(), ScaryOrbRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowProperties(ModItems.WEAPON_BOW.get());
            registerBowProperties(ModItems.WEAPON_BOW_SENTIENT.get());
        });
    }

    private static void registerBowProperties(net.minecraft.world.item.Item bow) {
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1.0F : 0.0F);
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
    }
}
