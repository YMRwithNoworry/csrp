package alku.csrp.relay.client;

import alku.csrp.item.RelayReportItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class RelayReportScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 230;
    private final RelayReportItem.Type reportType;
    private final CompoundTag data;
    private final List<FormattedCharSequence> wrappedLines = new ArrayList<>();
    private int scroll;

    RelayReportScreen(RelayReportItem.Type reportType, CompoundTag data) {
        super(Component.translatable("report.csrp." + reportType.id() + ".title"));
        this.reportType = reportType;
        this.data = data.copy();
    }

    @Override
    protected void init() {
        wrappedLines.clear();
        for (Component line : RelayReportItem.reportLines(reportType, data)) {
            if (line.getString().isEmpty()) {
                wrappedLines.add(FormattedCharSequence.EMPTY);
            } else {
                wrappedLines.addAll(font.split(line, PANEL_WIDTH - 38));
            }
        }
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + PANEL_WIDTH - 78, top + PANEL_HEIGHT - 28, 64, 20).build());
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF111315);
        graphics.fill(left + 2, top + 2, left + PANEL_WIDTH - 2, top + PANEL_HEIGHT - 2, 0xFFE5DFCF);
        graphics.fill(left + 10, top + 36, left + PANEL_WIDTH - 10, top + PANEL_HEIGHT - 36, 0xFFF2ECDC);
        graphics.drawCenteredString(font, title, width / 2, top + 14, 0xFF342B2D);

        int bodyTop = top + 43;
        int bodyBottom = top + PANEL_HEIGHT - 42;
        graphics.enableScissor(left + 14, bodyTop, left + PANEL_WIDTH - 14, bodyBottom);
        int lineY = bodyTop - scroll * 11;
        for (FormattedCharSequence line : wrappedLines) {
            if (lineY >= bodyTop - 10 && lineY < bodyBottom) {
                graphics.drawString(font, line, left + 20, lineY, 0xFF3A3132, false);
            }
            lineY += 11;
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            int trackTop = bodyTop;
            int trackHeight = bodyBottom - bodyTop;
            graphics.fill(left + PANEL_WIDTH - 18, trackTop,
                    left + PANEL_WIDTH - 15, bodyBottom, 0xFFB8AD99);
            int thumbHeight = Math.max(12, trackHeight * visibleLines() / wrappedLines.size());
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(left + PANEL_WIDTH - 19, thumbY,
                    left + PANEL_WIDTH - 14, thumbY + thumbHeight, 0xFF6D5C5F);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY) * 3));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int visibleLines() {
        return Math.max(1, (PANEL_HEIGHT - 86) / 11);
    }

    private int maxScroll() {
        return Math.max(0, wrappedLines.size() - visibleLines());
    }
}
