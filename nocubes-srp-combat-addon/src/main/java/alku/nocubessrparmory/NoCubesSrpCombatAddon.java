package alku.nocubessrparmory;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(NoCubesSrpCombatAddon.MOD_ID)
public final class NoCubesSrpCombatAddon {
    public static final String MOD_ID = "nocubessrparmory";
    public static final String VERSION = "3.0.0-port.1";
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARMORY_TAB = TABS.register("armory",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tabnocubesrparmorytab"))
                    .icon(() -> AddonItems.ICON_ARMORY_TAB.get().getDefaultInstance())
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .displayItems((parameters, output) -> AddonItems.all(output))
                    .build());

    public NoCubesSrpCombatAddon(IEventBus modBus) {
        AddonArmorMaterials.MATERIALS.register(modBus);
        AddonItems.ITEMS.register(modBus);
        ArmoryEntities.register(modBus);
        TABS.register(modBus);
    }
}
