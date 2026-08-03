package alku.csrp.client.screen;

import alku.csrp.Csrp;
import alku.csrp.inventory.ParasiteLootMenu;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ParasiteLootScreen extends AbstractContainerScreen<ParasiteLootMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/gui/parasite_loot.png");
    private static final ResourceLocation BUBBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "textures/gui/blood_bubble.png");
    private static final int MAX_BUBBLES = 28;
    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random random = new Random();
    private int previousFullness = -1;
    private int boostTicks;
    private int guiTick;

    public ParasiteLootScreen(ParasiteLootMenu menu, Inventory inventory, Component title) {
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
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawBubbles(graphics);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);

        float fullness = Math.max(0.0F, Math.min(1.0F, menu.fullnessScaled() / 1_000.0F));
        int barX = leftPos + 8;
        int barY = topPos + 16;
        graphics.fill(barX, barY, barX + 160, barY + 6, 0xFF202020);
        graphics.fill(barX, barY, barX + Math.round(160.0F * fullness), barY + 6,
                colorRedYellowGreen(fullness));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        guiTick++;
        int fullness = menu.fullnessScaled();
        if (previousFullness >= 0 && previousFullness != fullness) {
            boostTicks = 10;
        }
        previousFullness = fullness;
        float multiplier = boostTicks > 0 ? 2.25F : 1.0F;
        if (boostTicks > 0) {
            boostTicks--;
        }

        List<Bubble> splitBubbles = new ArrayList<>();
        Iterator<Bubble> iterator = bubbles.iterator();
        while (iterator.hasNext()) {
            Bubble bubble = iterator.next();
            bubble.y -= bubble.speed * multiplier;
            bubble.x += bubble.drift * multiplier;
            bubble.rotation += bubble.rotationSpeed * multiplier;
            if (bubble.splitTick >= 0 && guiTick >= bubble.splitTick) {
                bubble.splitTick = -1;
                splitBubble(bubble, splitBubbles);
            }
            if (bubble.y + bubble.size < 0.0F) {
                iterator.remove();
            }
        }
        int availableSlots = MAX_BUBBLES - bubbles.size();
        if (availableSlots > 0 && !splitBubbles.isEmpty()) {
            bubbles.addAll(splitBubbles.subList(0, Math.min(availableSlots, splitBubbles.size())));
        }
        float normalized = Math.max(0.0F, Math.min(1.0F, fullness / 1_000.0F));
        float spawnChance = 0.18F + (1.0F - normalized) * 0.22F;
        if (bubbles.size() < MAX_BUBBLES && random.nextFloat() < spawnChance) {
            bubbles.add(new Bubble(
                    random.nextFloat() * Math.max(1.0F, width - 24.0F),
                    height + 8.0F, 8.0F + random.nextFloat() * 16.0F,
                    0.25F + random.nextFloat() * 0.65F,
                    (random.nextFloat() - 0.5F) * 0.35F,
                    random.nextFloat() * 360.0F,
                    (random.nextFloat() - 0.5F) * 0.6F,
                    0.35F + random.nextFloat() * 0.45F,
                    random.nextFloat() < 0.35F ? guiTick + 100 + random.nextInt(101) : -1));
        }
    }

    private void splitBubble(Bubble parent, List<Bubble> splitBubbles) {
        if (parent.size < 12.8F) {
            return;
        }
        for (int side = -1; side <= 1; side += 2) {
            float childSize = Math.max(8.0F,
                    parent.size * (0.5F + random.nextFloat() * 0.2F));
            int childSplitTick = childSize > 14.4F && random.nextFloat() < 0.15F
                    ? guiTick + 80 + random.nextInt(81)
                    : -1;
            splitBubbles.add(new Bubble(
                    parent.x + side * childSize * 0.4F,
                    parent.y + random.nextFloat() * 2.0F,
                    childSize,
                    Math.min(1.125F, parent.speed * (1.05F + random.nextFloat() * 0.15F)),
                    parent.drift,
                    parent.rotation,
                    parent.rotationSpeed,
                    Math.min(1.0F, parent.alpha * (0.9F + random.nextFloat() * 0.2F)),
                    childSplitTick));
        }
    }

    private void drawBubbles(GuiGraphics graphics) {
        for (Bubble bubble : bubbles) {
            int size = Math.round(bubble.size);
            graphics.pose().pushPose();
            graphics.pose().translate(bubble.x + bubble.size / 2.0F,
                    bubble.y + bubble.size / 2.0F, -100.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(bubble.rotation));
            graphics.setColor(1.0F, 1.0F, 1.0F, bubble.alpha);
            graphics.blit(BUBBLE_TEXTURE, -size / 2, -size / 2, size, size,
                    0.0F, 0.0F, 7, 7, 7, 7);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().popPose();
        }
    }

    private static int colorRedYellowGreen(float fullness) {
        float value = Math.max(0.0F, Math.min(1.0F, fullness));
        int red;
        int green;
        if (value < 0.5F) {
            red = 255;
            green = (int) (value / 0.5F * 255.0F);
        } else {
            red = (int) ((1.0F - value) / 0.5F * 255.0F);
            green = 255;
        }
        return 0xFF000000 | red << 16 | green << 8;
    }

    private static final class Bubble {
        private float x;
        private float y;
        private final float size;
        private final float speed;
        private final float drift;
        private float rotation;
        private final float rotationSpeed;
        private final float alpha;
        private int splitTick;

        private Bubble(float x, float y, float size, float speed, float drift,
                float rotation, float rotationSpeed, float alpha, int splitTick) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.drift = drift;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.alpha = alpha;
            this.splitTick = splitTick;
        }
    }
}
