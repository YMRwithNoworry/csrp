package alku.csrp.client;

import alku.csrp.Csrp;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CreditsTitleScreenEvents {
    private static final Map<Screen, ModularUI> ACTIVE_CREDITS = new WeakHashMap<>();

    private CreditsTitleScreenEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen titleScreen)) {
            return;
        }

        ModularUI previous = ACTIVE_CREDITS.remove(titleScreen);
        if (previous != null) {
            previous.onRemoved();
        }

        ModularUI credits = createCreditsUI();
        credits.setAllowDebugMode(false);
        credits.setScreenAndInit(titleScreen);
        event.addListener(credits.getWidget());
        ACTIVE_CREDITS.put(titleScreen, credits);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        ModularUI credits = ACTIVE_CREDITS.remove(event.getScreen());
        if (credits != null) {
            credits.onRemoved();
        }
    }

    private static ModularUI createCreditsUI() {
        UIElement panel = new UIElement()
                .layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(10)
                        .bottom(26)
                        .width(176)
                        .height(58)
                        .paddingHorizontal(10)
                        .paddingVertical(7)
                        .gapAll(3))
                .style(style -> style.background(
                        SDFRectTexture.of(0xC0141820)
                                .setRadius(4)
                                .setStroke(1)
                                .setBorderColor(0x6079B8D6)))
                .setAllowHitTest(false);

        panel.addChildren(
                label("感谢名单", 0xFFFFD166, 11),
                label("程序：Paojiao134", 0xFFE8EEF2, 9),
                label("动画移植：无聊的保护者", 0xFFE8EEF2, 9));

        UIElement root = new UIElement()
                .layout(layout -> layout.widthPercent(100).heightPercent(100))
                .setAllowHitTest(false)
                .addChild(panel);

        return ModularUI.of(UI.of(root));
    }

    private static UIElement label(String text, int color, float fontSize) {
        return new Label()
                .setText(text)
                .textStyle(style -> style
                        .adaptiveWidth(true)
                        .adaptiveHeight(true)
                        .fontSize(fontSize)
                        .textColor(color)
                        .textShadow(true))
                .setAllowHitTest(false);
    }
}
