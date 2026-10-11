package alku.csrp.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Dedicated page for CSRP settings so they do not compete with vanilla's create-world layout. */
public final class SrpWorldOptionsScreen extends Screen {
    private static final int OPTION_WIDTH = 310;
    private static final int OPTION_MIN_WIDTH = 120;
    private static final int OPTION_HEIGHT = 20;
    private static final int OPTION_ROW_SPACING = 4;
    private static final int COMPACT_OPTION_ROW_SPACING = 0;
    private static final int TOP_MARGIN = 30;
    private static final int FOOTER_HEIGHT = 28;
    private static final int FOOTER_GAP = 8;

    private final CreateWorldScreen parent;
    private GridLayout optionLayout;
    private GridLayout.RowHelper optionRows;
    private int optionWidth;
    private int optionTop;
    private int optionRowSpacing;

    public SrpWorldOptionsScreen(CreateWorldScreen parent) {
        super(Component.translatable("options.csrp.world_options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        optionWidth = Math.min(OPTION_WIDTH, Math.max(OPTION_MIN_WIDTH, width - 20));
        optionRowSpacing = height < 180 ? COMPACT_OPTION_ROW_SPACING : OPTION_ROW_SPACING;
        optionLayout = new GridLayout().rowSpacing(optionRowSpacing);
        optionRows = optionLayout.createRowHelper(1);

        SrpDifficultyScreenEvents.addSelectors(this, parent);

        optionLayout.arrangeElements();
        int centeredTop = (height - optionLayout.getHeight()) / 2;
        int lastSafeTop = height - FOOTER_HEIGHT - optionLayout.getHeight() - FOOTER_GAP;
        optionTop = Math.max(TOP_MARGIN, Math.min(centeredTop, lastSafeTop));
        optionLayout.setX((width - optionLayout.getWidth()) / 2);
        optionLayout.setY(optionTop);
        optionLayout.visitWidgets(this::addRenderableWidget);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - 200) / 2, height - 28, 200, 20)
                .build());
    }

    public void addOption(AbstractWidget widget) {
        optionRows.addChild(widget);
    }

    int optionWidth() {
        return optionWidth;
    }

    int optionTop() {
        return optionTop;
    }

    int optionRowStep() {
        return OPTION_HEIGHT + optionRowSpacing;
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
