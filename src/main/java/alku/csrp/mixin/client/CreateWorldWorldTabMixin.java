package alku.csrp.mixin.client;

import alku.csrp.client.SrpWorldOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the CSRP entry to the vanilla World options tab. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class CreateWorldWorldTabMixin extends GridLayoutTab {
    private static final int CSRP_ENTRY_ROW = 4;
    private static final int WORLD_TAB_ROW_SPACING = 1;

    protected CreateWorldWorldTabMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void csrp$addWorldOptions(CreateWorldScreen parent, CallbackInfo callback) {
        // The extra row needs a compact outer grid; the nested vanilla controls retain
        // their own spacing, while the entry stays above the create/cancel footer.
        layout.rowSpacing(WORLD_TAB_ROW_SPACING);
        layout.addChild(Button.builder(Component.translatable("options.csrp.world_options"),
                        button -> Minecraft.getInstance().setScreen(new SrpWorldOptionsScreen(parent)))
                .width(310)
                .build(), CSRP_ENTRY_ROW, 0, 1, 2);
    }
}
