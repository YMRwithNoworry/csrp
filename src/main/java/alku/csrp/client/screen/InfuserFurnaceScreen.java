package alku.csrp.client.screen;

import alku.csrp.inventory.InfuserFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class InfuserFurnaceScreen extends AbstractContainerScreen<InfuserFurnaceMenu> {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("textures/gui/container/furnace.png");

    public InfuserFurnaceScreen(InfuserFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int flame = menu.burnScaled();
        if (flame > 0) {
            graphics.blit(BACKGROUND, leftPos + 56, topPos + 36 + 14 - flame,
                    176, 14 - flame, 14, flame, 256, 256);
        }
        int arrow = menu.progressScaled();
        if (arrow > 0) {
            graphics.blit(BACKGROUND, leftPos + 79, topPos + 34,
                    176, 14, arrow, 17, 256, 256);
        }
    }
}
