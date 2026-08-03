package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.KirinEntity;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Client-side GUI corruption emitted by nearby derived parasites. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class DerivedTextDistortion {
    private static final double RANGE = 100.0D;
    private static final Style DARK_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    private static final Style DISTORTED_STYLE = DARK_STYLE.withObfuscated(true);
    private static final ThreadLocal<Integer> RENDER_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private static boolean active;

    private DerivedTextDistortion() {
    }

    @SubscribeEvent
    public static void updateState(ClientTickEvent.Post event) {
        RENDER_SCOPE_DEPTH.remove();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.hasEffect(ModMobEffects.THE_SIGN)) {
            active = false;
            return;
        }
        active = player.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT)
                || !minecraft.level.getEntitiesOfClass(KirinEntity.class,
                        player.getBoundingBox().inflate(RANGE),
                        entity -> entity.isAlive() && entity.distanceToSqr(player) <= RANGE * RANGE).isEmpty()
                || !minecraft.level.getEntitiesOfClass(DraconiteEntity.class,
                        player.getBoundingBox().inflate(RANGE),
                        entity -> entity.isAlive() && entity.distanceToSqr(player) <= RANGE * RANGE).isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beginScreenRender(ScreenEvent.Render.Pre event) {
        beginRenderScope();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void endScreenRender(ScreenEvent.Render.Post event) {
        endRenderScope();
    }

    @SubscribeEvent
    public static void distortItemTooltips(ItemTooltipEvent event) {
        if (!active || event.getEntity() == null) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        for (int index = 0; index < tooltip.size(); index++) {
            String text = tooltip.get(index).getString();
            if (!text.isEmpty()) {
                tooltip.set(index, distortComponent(text));
            }
        }
    }

    public static void beginRenderScope() {
        RENDER_SCOPE_DEPTH.set(RENDER_SCOPE_DEPTH.get() + 1);
    }

    public static void endRenderScope() {
        int depth = RENDER_SCOPE_DEPTH.get();
        if (depth <= 1) {
            RENDER_SCOPE_DEPTH.remove();
        } else {
            RENDER_SCOPE_DEPTH.set(depth - 1);
        }
    }

    public static String distort(String text) {
        if (!shouldDistort() || text == null || text.isEmpty()) {
            return text;
        }
        String stripped = ChatFormatting.stripFormatting(text);
        if (stripped == null || stripped.isEmpty()) {
            return stripped;
        }
        StringBuilder result = new StringBuilder(stripped.length() * 7).append("\u00a78");
        stripped.codePoints().forEach(codePoint -> {
            if (Character.isWhitespace(codePoint)) {
                result.appendCodePoint(codePoint);
            } else {
                result.append("\u00a7k").appendCodePoint(codePoint).append("\u00a7r\u00a78");
            }
        });
        return result.toString();
    }

    public static FormattedCharSequence distort(FormattedCharSequence text) {
        if (!shouldDistort() || text == null || text instanceof BypassSequence
                || text instanceof DistortedSequence) {
            return text;
        }
        return new DistortedSequence(text);
    }

    public static FormattedCharSequence bypass(FormattedCharSequence text) {
        return text instanceof BypassSequence ? text : new BypassSequence(text);
    }

    private static boolean shouldDistort() {
        return active && RENDER_SCOPE_DEPTH.get() > 0;
    }

    private static Component distortComponent(String text) {
        MutableComponent result = Component.empty();
        text.codePoints().forEach(codePoint -> result.append(Component.literal(
                new String(Character.toChars(codePoint))).setStyle(
                Character.isWhitespace(codePoint) ? DARK_STYLE : DISTORTED_STYLE)));
        return result;
    }

    private static final class DistortedSequence implements FormattedCharSequence {
        private final FormattedCharSequence delegate;

        private DistortedSequence(FormattedCharSequence delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean accept(FormattedCharSink sink) {
            return delegate.accept((index, style, codePoint) -> sink.accept(index,
                    Character.isWhitespace(codePoint) ? DARK_STYLE : DISTORTED_STYLE, codePoint));
        }
    }

    private static final class BypassSequence implements FormattedCharSequence {
        private final FormattedCharSequence delegate;

        private BypassSequence(FormattedCharSequence delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean accept(FormattedCharSink sink) {
            return delegate.accept(sink);
        }
    }
}
