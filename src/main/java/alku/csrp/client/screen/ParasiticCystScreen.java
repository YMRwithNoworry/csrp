package alku.csrp.client.screen;

import alku.csrp.inventory.ParasiticCystMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ParasiticCystScreen extends AbstractContainerScreen<ParasiticCystMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int CONTAINER_ROWS = 4;
    private static final int CONTAINER_BACKGROUND_HEIGHT = CONTAINER_ROWS * 18 + 17;
    private static final int PLAYER_BACKGROUND_Y = 126;
    private static final int PLAYER_BACKGROUND_HEIGHT = 96;

    public ParasiticCystScreen(ParasiticCystMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 114 + CONTAINER_ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0,
                imageWidth, CONTAINER_BACKGROUND_HEIGHT, 256, 256);
        graphics.blit(BACKGROUND, leftPos, topPos + CONTAINER_BACKGROUND_HEIGHT,
                0, PLAYER_BACKGROUND_Y, imageWidth, PLAYER_BACKGROUND_HEIGHT, 256, 256);
    }
}
