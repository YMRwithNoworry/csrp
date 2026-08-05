package alku.csrp.client.screen;

import alku.csrp.block.entity.RelayTerminalBlockEntity;
import alku.csrp.inventory.RelayTerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class RelayTerminalScreen extends AbstractContainerScreen<RelayTerminalMenu> {
    private Button scanButton;

    public RelayTerminalScreen(RelayTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        scanButton = addRenderableWidget(Button.builder(Component.translatable("screen.csrp.relay.scan"),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                RelayTerminalMenu.SCAN_BUTTON);
                    }
                }).bounds(leftPos + 105, topPos + 33, 58, 20).build());
        updateButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF17191B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1,
                0xFF303438);
        graphics.fill(leftPos + 7, topPos + 27, leftPos + 99, topPos + 59, 0xFF202326);
        graphics.fill(leftPos + 79, topPos + 35, leftPos + 97, topPos + 53, 0xFF0E1011);
        graphics.fill(leftPos + 80, topPos + 36, leftPos + 96, topPos + 52, 0xFF24282B);

        int progressX = leftPos + 8;
        int progressY = topPos + 62;
        graphics.fill(progressX, progressY, progressX + 160, progressY + 6, 0xFF101112);
        if (menu.isScanning()) {
            int completed = RelayTerminalBlockEntity.SCAN_TICKS - menu.scanTicks();
            int width = Math.max(0, Math.min(160,
                    completed * 160 / RelayTerminalBlockEntity.SCAN_TICKS));
            graphics.fill(progressX, progressY, progressX + width, progressY + 6, 0xFFC94545);
        } else if (menu.cooldownTicks() > 0) {
            int remaining = menu.cooldownTicks();
            int width = Math.max(0, Math.min(160,
                    remaining * 160 / RelayTerminalBlockEntity.COOLDOWN_TICKS));
            graphics.fill(progressX, progressY, progressX + width, progressY + 6, 0xFF4D7C8B);
        } else {
            graphics.fill(progressX, progressY, progressX + 160, progressY + 6, 0xFF4C8A5A);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, leftPos + 7 + column * 18, topPos + 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, leftPos + 7 + column * 18, topPos + 141);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE4E7E9, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                0xFFB9BEC2, false);
        Component status;
        if (!menu.isFormed()) {
            status = Component.translatable("screen.csrp.relay.status.incomplete");
        } else if (menu.isScanning()) {
            status = Component.translatable("screen.csrp.relay.status.scanning",
                    (menu.scanTicks() + 19) / 20);
        } else if (menu.cooldownTicks() > 0) {
            status = Component.translatable("screen.csrp.relay.status.cooldown",
                    (menu.cooldownTicks() + 19) / 20);
        } else {
            status = Component.translatable("screen.csrp.relay.status.ready");
        }
        graphics.drawString(font, status, 9, 16, 0xFFB9BEC2, false);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButton();
    }

    private void updateButton() {
        if (scanButton != null) {
            scanButton.active = menu.isFormed() && !menu.isScanning()
                    && menu.cooldownTicks() <= 0 && menu.getSlot(0).hasItem();
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF111315);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF3B3F43);
    }
}
