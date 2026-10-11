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

/** Adds the CSRP world-options entry to vanilla's More tab. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$MoreTab")
public abstract class CreateWorldMoreTabMixin extends GridLayoutTab {
    protected CreateWorldMoreTabMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void csrp$addWorldOptions(CreateWorldScreen parent, CallbackInfo callback) {
        // GridLayout coordinates are row, column. Vanilla MoreTab occupies rows 0-2;
        // add a complete fourth row so the entry stays below the vanilla controls.
        layout.addChild(Button.builder(Component.translatable("options.csrp.world_options"),
                        button -> Minecraft.getInstance().setScreen(new SrpWorldOptionsScreen(parent)))
                .width(210)
                .build(), 3, 0, 1, 1);
    }
}
