package alku.csrp.client.screen;

import alku.csrp.inventory.ParasiticCystMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class ParasiticCystScreen extends AbstractContainerScreen<ParasiticCystMenu> {
    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    public ParasiticCystScreen(ParasiticCystMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, 256, 256);
    }
}
