package alku.csrp;

import alku.csrp.entity.BuglinEvolutionTarget;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModArmorMaterials;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
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
                        output.accept(ModItems.GNAT_SPAWN_EGG.get());
                        output.accept(ModItems.CARRIER_HEAVY_SPAWN_EGG.get());
                        output.accept(ModItems.CARRIER_LIGHT_SPAWN_EGG.get());
                        output.accept(ModItems.CARRIER_FLYING_SPAWN_EGG.get());
                        ModItems.ITEMS.getEntries().stream()
                                .filter(item -> item != ModItems.BUGLIN_SPAWN_EGG && item != ModItems.RUPTER_SPAWN_EGG
                                        && item != ModItems.PRI_LONGARMS_SPAWN_EGG && item != ModItems.PRI_SUMMONER_SPAWN_EGG
                                        && item != ModItems.PRI_VERMIN_SPAWN_EGG && item != ModItems.PRI_VISCERA_SPAWN_EGG
                                        && item != ModItems.GNAT_SPAWN_EGG && item != ModItems.CARRIER_HEAVY_SPAWN_EGG
                                        && item != ModItems.CARRIER_LIGHT_SPAWN_EGG && item != ModItems.CARRIER_FLYING_SPAWN_EGG)
                                .forEach(item -> output.accept(item.get()));
                    })
                    .build());

    public Csrp(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModArmorMaterials.MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMobEffects.EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        BuglinEvolutionTarget.registerRupter(ModEntities.RUPTER);
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
            event.accept(ModItems.GNAT_SPAWN_EGG.get());
            event.accept(ModItems.CARRIER_HEAVY_SPAWN_EGG.get());
            event.accept(ModItems.CARRIER_LIGHT_SPAWN_EGG.get());
            event.accept(ModItems.CARRIER_FLYING_SPAWN_EGG.get());
            event.accept(ModItems.AIRSCREW_SPAWN_EGG.get());
            event.accept(ModItems.HEED_SPAWN_EGG.get());
            event.accept(ModItems.DREDGE_SPAWN_EGG.get());
            event.accept(ModItems.THRALL_SPAWN_EGG.get());
        }
    }
}
