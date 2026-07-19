package alku.csrp;

import alku.csrp.entity.BuglinEvolutionTarget;
import alku.csrp.entity.ManglerEvolutionTarget;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModArmorMaterials;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Csrp.MODID)
public final class Csrp {
    public static final String MODID = "csrp";
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CSRP_TAB =
            CREATIVE_MODE_TABS.register("csrp_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.csrp"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.BUGLIN_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BUGLIN_SPAWN_EGG.get());
                        output.accept(ModItems.RUPTER_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_LONGARMS_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_SUMMONER_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_VERMIN_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_VISCERA_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_ARACHNIDA_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_BOLSTER_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_BURROWER_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_DEVOURER_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_MANDUCATER_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_REEKER_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_TOZOON_SPAWN_EGG.get());
                        output.accept(ModItems.PRI_YELLOWEYE_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_ARACHNIDA_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_BOLSTER_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_BURROWER_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_DEVOURER_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_LONGARMS_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_MANDUCATER_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_REEKER_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_SUMMONER_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_TOZOON_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_VERMIN_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_VISCERA_SPAWN_EGG.get());
                        output.accept(ModItems.ADA_YELLOWEYE_SPAWN_EGG.get());
                        output.accept(ModItems.GNAT_SPAWN_EGG.get());
                        output.accept(ModItems.CARRIER_HEAVY_SPAWN_EGG.get());
                        output.accept(ModItems.CARRIER_LIGHT_SPAWN_EGG.get());
                        output.accept(ModItems.CARRIER_FLYING_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_BIGSPIDER_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_DRAGONE_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_DRAGONHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_ENDERMAN_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_ENDERMANHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_HORSE_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_HORSEHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_HUMAN_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_HUMANHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_COWHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_PIGHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_SHEEPHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_VILLAGER_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_VILLAGERHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.SIM_WOLFHEAD_SPAWN_EGG.get());
                        output.accept(ModItems.DISPATCHERTEN_SPAWN_EGG.get());
                        output.accept(ModItems.KYPHOSIS_SPAWN_EGG.get());
                        output.accept(ModItems.SEIZER_SPAWN_EGG.get());
                        output.accept(ModItems.SENTRY_SPAWN_EGG.get());
                        output.accept(ModItems.WORM_SPAWN_EGG.get());
                        output.accept(ModItems.GRUNT_SPAWN_EGG.get());
                        output.accept(ModItems.BOMBER_LIGHT_SPAWN_EGG.get());
                        output.accept(ModItems.MONARCH_SPAWN_EGG.get());
                        output.accept(ModItems.OVERSEER_SPAWN_EGG.get());
                        output.accept(ModItems.VIGILANTE_SPAWN_EGG.get());
                        output.accept(ModItems.WARDEN_SPAWN_EGG.get());
                        ModItems.ITEMS.getEntries().stream()
                                .filter(item -> item != ModItems.BUGLIN_SPAWN_EGG && item != ModItems.RUPTER_SPAWN_EGG
                                        && item != ModItems.PRI_LONGARMS_SPAWN_EGG && item != ModItems.PRI_SUMMONER_SPAWN_EGG
                                        && item != ModItems.PRI_VERMIN_SPAWN_EGG && item != ModItems.PRI_VISCERA_SPAWN_EGG
                                        && item != ModItems.PRI_ARACHNIDA_SPAWN_EGG && item != ModItems.PRI_BOLSTER_SPAWN_EGG
                                        && item != ModItems.PRI_BURROWER_SPAWN_EGG && item != ModItems.PRI_DEVOURER_SPAWN_EGG
                                        && item != ModItems.PRI_MANDUCATER_SPAWN_EGG && item != ModItems.PRI_REEKER_SPAWN_EGG
                                        && item != ModItems.PRI_TOZOON_SPAWN_EGG && item != ModItems.PRI_YELLOWEYE_SPAWN_EGG
                                        && item != ModItems.ADA_ARACHNIDA_SPAWN_EGG && item != ModItems.ADA_BOLSTER_SPAWN_EGG
                                        && item != ModItems.ADA_BURROWER_SPAWN_EGG && item != ModItems.ADA_DEVOURER_SPAWN_EGG
                                        && item != ModItems.ADA_LONGARMS_SPAWN_EGG && item != ModItems.ADA_MANDUCATER_SPAWN_EGG
                                        && item != ModItems.ADA_REEKER_SPAWN_EGG && item != ModItems.ADA_SUMMONER_SPAWN_EGG
                                        && item != ModItems.ADA_TOZOON_SPAWN_EGG && item != ModItems.ADA_VERMIN_SPAWN_EGG
                                        && item != ModItems.ADA_VISCERA_SPAWN_EGG && item != ModItems.ADA_YELLOWEYE_SPAWN_EGG
                                        && item != ModItems.GNAT_SPAWN_EGG && item != ModItems.CARRIER_HEAVY_SPAWN_EGG
                                        && item != ModItems.CARRIER_LIGHT_SPAWN_EGG && item != ModItems.CARRIER_FLYING_SPAWN_EGG
                                        && item != ModItems.SIM_BIGSPIDER_SPAWN_EGG && item != ModItems.SIM_DRAGONE_SPAWN_EGG
                                        && item != ModItems.SIM_DRAGONHEAD_SPAWN_EGG && item != ModItems.SIM_ENDERMAN_SPAWN_EGG
                                        && item != ModItems.SIM_ENDERMANHEAD_SPAWN_EGG && item != ModItems.SIM_HORSE_SPAWN_EGG
                                        && item != ModItems.SIM_HORSEHEAD_SPAWN_EGG && item != ModItems.SIM_HUMAN_SPAWN_EGG
                                        && item != ModItems.SIM_HUMANHEAD_SPAWN_EGG && item != ModItems.SIM_COWHEAD_SPAWN_EGG
                                        && item != ModItems.SIM_PIGHEAD_SPAWN_EGG && item != ModItems.SIM_SHEEPHEAD_SPAWN_EGG
                                        && item != ModItems.SIM_VILLAGER_SPAWN_EGG && item != ModItems.SIM_VILLAGERHEAD_SPAWN_EGG
                                        && item != ModItems.SIM_WOLFHEAD_SPAWN_EGG
                                        && item != ModItems.DISPATCHERTEN_SPAWN_EGG && item != ModItems.KYPHOSIS_SPAWN_EGG
                                        && item != ModItems.SEIZER_SPAWN_EGG && item != ModItems.SENTRY_SPAWN_EGG
                                        && item != ModItems.WORM_SPAWN_EGG && item != ModItems.GRUNT_SPAWN_EGG
                                        && item != ModItems.BOMBER_LIGHT_SPAWN_EGG && item != ModItems.MONARCH_SPAWN_EGG
                                        && item != ModItems.OVERSEER_SPAWN_EGG && item != ModItems.VIGILANTE_SPAWN_EGG
                                        && item != ModItems.WARDEN_SPAWN_EGG)
                                .forEach(item -> output.accept(item.get()));
                    })
                    .build());

    public Csrp(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModArmorMaterials.MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMobEffects.EFFECTS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        BuglinEvolutionTarget.registerRupter(ModEntities.RUPTER);
        ManglerEvolutionTarget.registerMangler(ModEntities.MANGLER);
        modEventBus.addListener(this::addCreativeItems);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.BUGLIN_SPAWN_EGG.get());
            event.accept(ModItems.RUPTER_SPAWN_EGG.get());
            event.accept(ModItems.PRI_LONGARMS_SPAWN_EGG.get());
            event.accept(ModItems.PRI_SUMMONER_SPAWN_EGG.get());
            event.accept(ModItems.PRI_VERMIN_SPAWN_EGG.get());
            event.accept(ModItems.PRI_VISCERA_SPAWN_EGG.get());
            event.accept(ModItems.PRI_ARACHNIDA_SPAWN_EGG.get());
            event.accept(ModItems.PRI_BOLSTER_SPAWN_EGG.get());
            event.accept(ModItems.PRI_BURROWER_SPAWN_EGG.get());
            event.accept(ModItems.PRI_DEVOURER_SPAWN_EGG.get());
            event.accept(ModItems.PRI_MANDUCATER_SPAWN_EGG.get());
            event.accept(ModItems.PRI_REEKER_SPAWN_EGG.get());
            event.accept(ModItems.PRI_TOZOON_SPAWN_EGG.get());
            event.accept(ModItems.PRI_YELLOWEYE_SPAWN_EGG.get());
            event.accept(ModItems.ADA_ARACHNIDA_SPAWN_EGG.get());
            event.accept(ModItems.ADA_BOLSTER_SPAWN_EGG.get());
            event.accept(ModItems.ADA_BURROWER_SPAWN_EGG.get());
            event.accept(ModItems.ADA_DEVOURER_SPAWN_EGG.get());
            event.accept(ModItems.ADA_LONGARMS_SPAWN_EGG.get());
            event.accept(ModItems.ADA_MANDUCATER_SPAWN_EGG.get());
            event.accept(ModItems.ADA_REEKER_SPAWN_EGG.get());
            event.accept(ModItems.ADA_SUMMONER_SPAWN_EGG.get());
            event.accept(ModItems.ADA_TOZOON_SPAWN_EGG.get());
            event.accept(ModItems.ADA_VERMIN_SPAWN_EGG.get());
            event.accept(ModItems.ADA_VISCERA_SPAWN_EGG.get());
            event.accept(ModItems.ADA_YELLOWEYE_SPAWN_EGG.get());
            event.accept(ModItems.GNAT_SPAWN_EGG.get());
            event.accept(ModItems.CARRIER_HEAVY_SPAWN_EGG.get());
            event.accept(ModItems.CARRIER_LIGHT_SPAWN_EGG.get());
            event.accept(ModItems.CARRIER_FLYING_SPAWN_EGG.get());
            event.accept(ModItems.AIRSCREW_SPAWN_EGG.get());
            event.accept(ModItems.HEED_SPAWN_EGG.get());
            event.accept(ModItems.DREDGE_SPAWN_EGG.get());
            event.accept(ModItems.THRALL_SPAWN_EGG.get());
            event.accept(ModItems.LICE_SPAWN_EGG.get());
            event.accept(ModItems.MANGLER_SPAWN_EGG.get());
            event.accept(ModItems.HOST_SPAWN_EGG.get());
            event.accept(ModItems.HOSTII_SPAWN_EGG.get());
            event.accept(ModItems.INCOMPLETEFORM_SMALL_SPAWN_EGG.get());
            event.accept(ModItems.INCOMPLETEFORM_MEDIUM_SPAWN_EGG.get());
            event.accept(ModItems.DRACONITE_SPAWN_EGG.get());
            event.accept(ModItems.KIRIN_SPAWN_EGG.get());
            event.accept(ModItems.SIM_ADVENTURER_SPAWN_EGG.get());
            event.accept(ModItems.SIM_ADVENTURER_HEAD_SPAWN_EGG.get());
            event.accept(ModItems.MOVING_FLESH_SPAWN_EGG.get());
            event.accept(ModItems.SIM_BEAR_SPAWN_EGG.get());
            event.accept(ModItems.SIM_COW_SPAWN_EGG.get());
            event.accept(ModItems.SIM_PIG_SPAWN_EGG.get());
            event.accept(ModItems.SIM_SHEEP_SPAWN_EGG.get());
            event.accept(ModItems.SIM_WOLF_SPAWN_EGG.get());
            event.accept(ModItems.SIM_SQUID_SPAWN_EGG.get());
            event.accept(ModItems.SIM_BIGSPIDER_SPAWN_EGG.get());
            event.accept(ModItems.SIM_DRAGONE_SPAWN_EGG.get());
            event.accept(ModItems.SIM_DRAGONHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_ENDERMAN_SPAWN_EGG.get());
            event.accept(ModItems.SIM_ENDERMANHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_HORSE_SPAWN_EGG.get());
            event.accept(ModItems.SIM_HORSEHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_HUMAN_SPAWN_EGG.get());
            event.accept(ModItems.SIM_HUMANHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_COWHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_PIGHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_SHEEPHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_VILLAGER_SPAWN_EGG.get());
            event.accept(ModItems.SIM_VILLAGERHEAD_SPAWN_EGG.get());
            event.accept(ModItems.SIM_WOLFHEAD_SPAWN_EGG.get());
            event.accept(ModItems.FER_BEAR_SPAWN_EGG.get());
            event.accept(ModItems.FER_COW_SPAWN_EGG.get());
            event.accept(ModItems.FER_HORSE_SPAWN_EGG.get());
            event.accept(ModItems.FER_HUMAN_SPAWN_EGG.get());
            event.accept(ModItems.FER_PIG_SPAWN_EGG.get());
            event.accept(ModItems.FER_SHEEP_SPAWN_EGG.get());
            event.accept(ModItems.FER_VILLAGER_SPAWN_EGG.get());
            event.accept(ModItems.FER_WOLF_SPAWN_EGG.get());
            event.accept(ModItems.FER_ENDERMAN_SPAWN_EGG.get());
            event.accept(ModItems.HI_BLAZE_SPAWN_EGG.get());
            event.accept(ModItems.HI_GOLEM_SPAWN_EGG.get());
            event.accept(ModItems.HI_SKELETON_SPAWN_EGG.get());
            event.accept(ModItems.MAR_BEAR_SPAWN_EGG.get());
            event.accept(ModItems.MAR_COW_SPAWN_EGG.get());
            event.accept(ModItems.MAR_ENDERMAN_SPAWN_EGG.get());
            event.accept(ModItems.MAR_HUMAN_SPAWN_EGG.get());
            event.accept(ModItems.MAR_SHEEP_SPAWN_EGG.get());
            event.accept(ModItems.MAR_VILLAGER_SPAWN_EGG.get());
            event.accept(ModItems.MARAUDER_SPAWN_EGG.get());
            event.accept(ModItems.DISPATCHERTEN_SPAWN_EGG.get());
            event.accept(ModItems.KYPHOSIS_SPAWN_EGG.get());
            event.accept(ModItems.SEIZER_SPAWN_EGG.get());
            event.accept(ModItems.SENTRY_SPAWN_EGG.get());
            event.accept(ModItems.WORM_SPAWN_EGG.get());
            event.accept(ModItems.GRUNT_SPAWN_EGG.get());
            event.accept(ModItems.BOMBER_LIGHT_SPAWN_EGG.get());
            event.accept(ModItems.MONARCH_SPAWN_EGG.get());
            event.accept(ModItems.OVERSEER_SPAWN_EGG.get());
            event.accept(ModItems.VIGILANTE_SPAWN_EGG.get());
            event.accept(ModItems.WARDEN_SPAWN_EGG.get());
        }
    }
}
