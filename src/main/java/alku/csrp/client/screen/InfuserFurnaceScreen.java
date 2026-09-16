package alku.csrp.client.screen;

import alku.csrp.inventory.InfuserFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class InfuserFurnaceScreen extends AbstractContainerScreen<InfuserFurnaceMenu> {
    private static final Identifier BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png");

    public InfuserFurnaceScreen(InfuserFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, 256, 256);
        int flame = menu.burnScaled();
        if (flame > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + 56, topPos + 36 + 14 - flame,
                    176.0F, (float) (14 - flame), 14, flame, 256, 256);
        }
        int arrow = menu.progressScaled();
        if (arrow > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + 79, topPos + 34,
                    176.0F, 14.0F, arrow, 17, 256, 256);
        }
    }
}
