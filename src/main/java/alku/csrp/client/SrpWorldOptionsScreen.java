package alku.csrp.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** World settings live here so they never overlap vanilla's tabs or footer. */
public final class SrpWorldOptionsScreen extends Screen {
    private final CreateWorldScreen parent;

    public SrpWorldOptionsScreen(CreateWorldScreen parent) {
        super(Component.translatable("options.csrp.world_options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SrpDifficultyScreenEvents.addSelectors(this, parent);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - 200) / 2, height - 28, 200, 20).build());
    }

    public void addOption(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        SrpDifficultyScreenEvents.renderMeteorPreview(this, parent, graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
