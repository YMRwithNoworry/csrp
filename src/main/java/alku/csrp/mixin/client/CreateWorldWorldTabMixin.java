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

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class CreateWorldWorldTabMixin extends GridLayoutTab {
    protected CreateWorldWorldTabMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void csrp$addWorldOptions(CreateWorldScreen parent, CallbackInfo callback) {
        // Vanilla uses rows 0-2: world type, seed and generation switches.
        // Joining its grid lets tab switching, resizing and narration handle this button too.
        layout.addChild(Button.builder(Component.translatable("options.csrp.world_options"),
                        button -> Minecraft.getInstance().setScreen(new SrpWorldOptionsScreen(parent)))
                .width(310).build(), 3, 0, 1, 2);
    }
}
