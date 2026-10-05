package alku.csrp.client.screen;

import alku.csrp.Csrp;
import alku.csrp.inventory.InfuserFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Port of SRP 1.10.9 {@code client/gui/GuiInfuserFurnace}.
 *
 * <p>Draws the mod's own {@code infuser_furnace.png} rather than the vanilla furnace background, and
 * reproduces the original's sprite offsets: the progress arrow is the 24x16 sprite at (176,14) drawn at
 * (80,35) — the original draws both the smelting and the infusion progress there, so the infusion bar
 * simply overdraws the smelting one — and the burn flame is the 14x13 sprite at (176,0) drawn upwards
 * from (56,36).</p>
 */
public final class InfuserFurnaceScreen extends AbstractContainerScreen<InfuserFurnaceMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Csrp.MODID, "textures/gui/infuser_furnace.png");
    private static final int ARROW_U = 176;
    private static final int ARROW_V = 14;
    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 35;
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_WIDTH = 14;

    public InfuserFurnaceScreen(InfuserFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, 256, 256);

        int smelt = Math.min(menu.smeltScaled(), 24);
        if (smelt > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y,
                    (float) ARROW_U, (float) ARROW_V, smelt + 1, 16, 256, 256);
        }

        if (menu.isBurning()) {
            int flame = menu.burnScaled();
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + FLAME_X,
                    topPos + FLAME_Y + 12 - flame, (float) ARROW_U, (float) (12 - flame),
                    FLAME_WIDTH, flame + 1, 256, 256);
        }

        int infuse = Math.min(menu.infuseScaled(), 24);
        if (infuse > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos + ARROW_X, topPos + ARROW_Y,
                    (float) ARROW_U, (float) ARROW_V, infuse + 1, 16, 256, 256);
        }
    }
}
