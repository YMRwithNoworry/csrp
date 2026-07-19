package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.client.particle.KirinWarningParticle;
import alku.csrp.client.renderer.AirscrewRenderer;
import alku.csrp.client.renderer.BuglinRenderer;
import alku.csrp.client.renderer.MarauderRenderer;
import alku.csrp.client.renderer.MarauderTendrilRenderer;
import alku.csrp.client.renderer.MovingFleshRenderer;
import alku.csrp.client.renderer.RupterRenderer;
import alku.csrp.client.renderer.SimAdventurerRenderer;
import alku.csrp.client.renderer.AssimilatedParasiteRenderer;
import alku.csrp.client.renderer.PrimitiveParasiteRenderer;
import alku.csrp.client.renderer.PullingBallRenderer;
import alku.csrp.client.renderer.ParasiteProjectileRenderer;
import alku.csrp.client.renderer.ScaryOrbRenderer;
import alku.csrp.client.renderer.TetheredMarauderizedRenderer;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModParticles;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

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
        event.registerEntityRenderer(ModEntities.PRI_ARACHNIDA.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_arachnida", 0.65F));
        event.registerEntityRenderer(ModEntities.PRI_BOLSTER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_bolster", 0.7F));
        event.registerEntityRenderer(ModEntities.PRI_BURROWER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_burrower", 0.5F));
        event.registerEntityRenderer(ModEntities.PRI_DEVOURER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_devourer", 0.7F));
        event.registerEntityRenderer(ModEntities.PRI_MANDUCATER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_manducater", 0.75F));
        event.registerEntityRenderer(ModEntities.PRI_REEKER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_reeker", 0.65F));
        event.registerEntityRenderer(ModEntities.PRI_TOZOON.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_tozoon", 0.65F));
        event.registerEntityRenderer(ModEntities.PRI_YELLOWEYE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "pri_yelloweye", 0.5F));
        event.registerEntityRenderer(ModEntities.ADA_ARACHNIDA.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_arachnida", 0.85F));
        event.registerEntityRenderer(ModEntities.ADA_BOLSTER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_bolster", 0.9F));
        event.registerEntityRenderer(ModEntities.ADA_BURROWER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_burrower", 0.7F));
        event.registerEntityRenderer(ModEntities.ADA_DEVOURER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_devourer", 0.8F));
        event.registerEntityRenderer(ModEntities.ADA_LONGARMS.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_longarms", 0.9F));
        event.registerEntityRenderer(ModEntities.ADA_MANDUCATER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_manducater", 0.85F));
        event.registerEntityRenderer(ModEntities.ADA_REEKER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_reeker", 0.85F));
        event.registerEntityRenderer(ModEntities.ADA_SUMMONER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_summoner", 0.8F));
        event.registerEntityRenderer(ModEntities.ADA_TOZOON.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_tozoon", 0.7F));
        event.registerEntityRenderer(ModEntities.ADA_VERMIN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_vermin", 0.85F));
        event.registerEntityRenderer(ModEntities.ADA_VISCERA.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_viscera", 0.75F));
        event.registerEntityRenderer(ModEntities.ADA_YELLOWEYE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "ada_yelloweye", 0.8F));
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
        event.registerEntityRenderer(ModEntities.AIRSCREW.get(), AirscrewRenderer::new);
        event.registerEntityRenderer(ModEntities.HEED.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "heed", 0.8F));
        event.registerEntityRenderer(ModEntities.DREDGE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "dredge", 0.8F));
        event.registerEntityRenderer(ModEntities.THRALL.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "thrall", 0.7F));
        event.registerEntityRenderer(ModEntities.PULLING_BALL.get(), PullingBallRenderer::new);
        event.registerEntityRenderer(ModEntities.LICE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "lice", 0.35F));
        event.registerEntityRenderer(ModEntities.MANGLER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "mangler", 0.5F));
        event.registerEntityRenderer(ModEntities.HOST.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "host", 0.7F));
        event.registerEntityRenderer(ModEntities.HOSTII.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "hostii", 1.0F));
        event.registerEntityRenderer(ModEntities.INCOMPLETEFORM_SMALL.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "incompleteform_small", 0.35F));
        event.registerEntityRenderer(ModEntities.INCOMPLETEFORM_MEDIUM.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "incompleteform_medium", 0.45F));
        event.registerEntityRenderer(ModEntities.DRACONITE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "draconite", 1.2F));
        event.registerEntityRenderer(ModEntities.KIRIN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "kirin", 1.1F));
        event.registerEntityRenderer(ModEntities.SIM_ADVENTURER.get(), SimAdventurerRenderer::new);
        event.registerEntityRenderer(ModEntities.SIM_ADVENTURER_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_adventurerhead", 0.6F));
        event.registerEntityRenderer(ModEntities.MOVINGFLESH.get(), MovingFleshRenderer::new);
        event.registerEntityRenderer(ModEntities.SIM_BEAR.get(), context ->
                new AssimilatedParasiteRenderer(context, 0.65F));
        event.registerEntityRenderer(ModEntities.SIM_COW.get(), context ->
                new AssimilatedParasiteRenderer(context, 0.55F));
        event.registerEntityRenderer(ModEntities.SIM_PIG.get(), context ->
                new AssimilatedParasiteRenderer(context, 0.45F));
        event.registerEntityRenderer(ModEntities.SIM_SHEEP.get(), context ->
                new AssimilatedParasiteRenderer(context, 0.50F));
        event.registerEntityRenderer(ModEntities.SIM_WOLF.get(), context ->
                new AssimilatedParasiteRenderer(context, 0.40F));
        event.registerEntityRenderer(ModEntities.SIM_SQUID.get(), context ->
                new AssimilatedParasiteRenderer(context, 0.45F));
        event.registerEntityRenderer(ModEntities.SIM_BIGSPIDER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_bigspider", 1.2F));
        event.registerEntityRenderer(ModEntities.SIM_DRAGONE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_dragone", 1.2F));
        event.registerEntityRenderer(ModEntities.SIM_DRAGON_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_dragonhead", 0.6F));
        event.registerEntityRenderer(ModEntities.SIM_ENDERMAN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_enderman", 0.5F));
        event.registerEntityRenderer(ModEntities.SIM_ENDERMAN_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_endermanhead", 0.6F));
        event.registerEntityRenderer(ModEntities.SIM_HORSE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_horse", 0.75F));
        event.registerEntityRenderer(ModEntities.SIM_HORSE_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_horsehead", 0.6F));
        event.registerEntityRenderer(ModEntities.SIM_HUMAN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_human", 0.5F));
        event.registerEntityRenderer(ModEntities.SIM_HUMAN_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_humanhead", 0.6F));
        event.registerEntityRenderer(ModEntities.SIM_COW_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_cowhead", 0.6F));
        event.registerEntityRenderer(ModEntities.SIM_PIG_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_pighead", 0.6F));
        event.registerEntityRenderer(ModEntities.SIM_SHEEP_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_sheephead", 0.5F));
        event.registerEntityRenderer(ModEntities.SIM_VILLAGER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_villager", 0.5F));
        event.registerEntityRenderer(ModEntities.SIM_VILLAGER_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_villagerhead", 0.6F));
        event.registerEntityRenderer(ModEntities.SIM_WOLF_HEAD.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sim_wolfhead", 0.4F));
        event.registerEntityRenderer(ModEntities.FER_BEAR.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_bear", 0.65F));
        event.registerEntityRenderer(ModEntities.FER_COW.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_cow", 0.55F));
        event.registerEntityRenderer(ModEntities.FER_HORSE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_horse", 0.5F));
        event.registerEntityRenderer(ModEntities.FER_HUMAN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_human", 0.5F));
        event.registerEntityRenderer(ModEntities.FER_PIG.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_pig", 0.45F));
        event.registerEntityRenderer(ModEntities.FER_SHEEP.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_sheep", 0.50F));
        event.registerEntityRenderer(ModEntities.FER_VILLAGER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_villager", 0.5F));
        event.registerEntityRenderer(ModEntities.FER_WOLF.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_wolf", 0.45F));
        event.registerEntityRenderer(ModEntities.FER_ENDERMAN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "fer_enderman", 0.5F));
        event.registerEntityRenderer(ModEntities.HI_BLAZE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "hi_blaze", 0.5F));
        event.registerEntityRenderer(ModEntities.HI_GOLEM.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "hi_golem", 0.8F));
        event.registerEntityRenderer(ModEntities.HI_SKELETON.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "hi_skeleton", 0.5F));
        event.registerEntityRenderer(ModEntities.MAR_BEAR.get(), context ->
                new TetheredMarauderizedRenderer<>(context, "mar_bear", 0.65F));
        event.registerEntityRenderer(ModEntities.MAR_COW.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "mar_cow", 0.55F));
        event.registerEntityRenderer(ModEntities.MAR_ENDERMAN.get(), context ->
                new TetheredMarauderizedRenderer<>(context, "mar_enderman", 0.5F));
        event.registerEntityRenderer(ModEntities.MAR_HUMAN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "mar_human", 0.5F));
        event.registerEntityRenderer(ModEntities.MAR_SHEEP.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "mar_sheep", 0.5F));
        event.registerEntityRenderer(ModEntities.MAR_VILLAGER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "mar_villager", 0.5F));
        event.registerEntityRenderer(ModEntities.MARAUDER.get(), MarauderRenderer::new);
        event.registerEntityRenderer(ModEntities.MARAUDER_TENDRIL.get(), MarauderTendrilRenderer::new);
        event.registerEntityRenderer(ModEntities.DISPATCHERTEN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "dispatcherten", 0.45F));
        event.registerEntityRenderer(ModEntities.KYPHOSIS.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "kyphosis", 1.0F));
        event.registerEntityRenderer(ModEntities.SEIZER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "seizer", 0.55F));
        event.registerEntityRenderer(ModEntities.SENTRY.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "sentry", 0.9F));
        event.registerEntityRenderer(ModEntities.WORM.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "worm", 1.1F));
        event.registerEntityRenderer(ModEntities.GRUNT.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "grunt", 0.5F));
        event.registerEntityRenderer(ModEntities.BOMBER_LIGHT.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "bomber_light", 0.85F));
        event.registerEntityRenderer(ModEntities.MONARCH.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "monarch", 1.1F));
        event.registerEntityRenderer(ModEntities.OVERSEER.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "overseer", 1.0F));
        event.registerEntityRenderer(ModEntities.VIGILANTE.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "vigilante", 0.9F));
        event.registerEntityRenderer(ModEntities.WARDEN.get(), context ->
                new PrimitiveParasiteRenderer<>(context, "warden", 1.05F));
        event.registerEntityRenderer(ModEntities.PARASITE_PROJECTILE.get(), ParasiteProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.KIRIN_WARNING.get(), KirinWarningParticle.Provider::new);
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
