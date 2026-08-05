package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.inventory.ParasiteLootMenu;
import alku.csrp.inventory.ParasiticCystMenu;
import alku.csrp.inventory.RelayTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Csrp.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ParasiteLootMenu>> PARASITE_LOOT =
            MENUS.register("parasite_loot", () -> new MenuType<>(ParasiteLootMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RelayTerminalMenu>> RELAY_TERMINAL =
            MENUS.register("relay_terminal", () -> new MenuType<>(RelayTerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ParasiticCystMenu>> PARASITIC_CYST =
            MENUS.register("parasitic_cyst", () -> new MenuType<>(ParasiticCystMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }
}
