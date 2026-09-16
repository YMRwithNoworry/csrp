package alku.csrp.item;

import alku.csrp.relay.network.RelayReportOpenPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** A printed, immutable snapshot produced by a Relay Tower scan. */
public final class RelayReportItem extends Item {
    private final Type type;

    public RelayReportItem(Type type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public Type type() {
        return type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            PacketDistributor.sendToPlayer(serverPlayer, new RelayReportOpenPayload(type.id, data));
        }
        return level.isClientSide()
                ? InteractionResult.SUCCESS.heldItemTransformedTo(stack)
                : InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tooltip.accept(Component.translatable("tooltip.csrp.relay_report.read")
                .withStyle(ChatFormatting.GRAY));
        if (data.contains("PrintDay")) {
            tooltip.accept(Component.translatable("tooltip.csrp.relay_report.printed",
                    data.getIntOr("PrintDay", 0), formatTime(data.getIntOr("PrintTime", 0)))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type == Type.SCAN && data.contains("TotalMobs")) {
            tooltip.accept(Component.empty());
            reportLines(type, data).forEach(tooltip);
        }
    }

    public static List<Component> reportLines(Type type, CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("report.csrp." + type.id + ".title")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        lines.add(Component.translatable("report.csrp.printed", data.getIntOr("PrintDay", 0),
                formatTime(data.getIntOr("PrintTime", 0))).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.empty());
        switch (type) {
            case SCAN -> addScanLines(lines, data);
            case PHASE -> addPhaseLines(lines, data);
            case VECTOR -> addVectorLines(lines, data);
            case DISLODGEMENT -> addDislodgementLines(lines, data);
        }
        return lines;
    }

    private static void addScanLines(List<Component> lines, CompoundTag data) {
        add(lines, "dimension", data.getStringOr("Dimension", ""));
        add(lines, "total_mobs", data.getIntOr("TotalMobs", 0));
        add(lines, "total_parasites", data.getIntOr("TotalParasites", 0));
        add(lines, "share", String.format(Locale.ROOT, "%.1f%%", data.getIntOr("ShareTenths", 0) / 10.0D));
        add(lines, "ratio", data.getStringOr("Ratio", ""));
        lines.add(Component.empty());
        lines.add(Component.translatable("report.csrp.scan.tiers").withStyle(ChatFormatting.DARK_GRAY));
        ListTag tiers = data.getListOrEmpty("Tiers");
        for (int index = 0; index < tiers.size(); index++) {
            String tier = tiers.getStringOr(index, "");
            lines.add(Component.translatable("report.csrp.scan.tier",
                    Component.translatable("report.csrp.tier." + tier), data.getIntOr("Tier_" + tier, 0))
                    .withStyle(tierColor(tier)));
        }
    }

    private static void addPhaseLines(List<Component> lines, CompoundTag data) {
        add(lines, "dimension", data.getStringOr("Dimension", ""));
        add(lines, "phase", data.getIntOr("Phase", 0));
        add(lines, "points", data.getIntOr("Points", 0));
        add(lines, "next_points", data.getIntOr("NextPoints", 0));
        add(lines, "progress", String.format(Locale.ROOT, "%.1f%%", data.getIntOr("ProgressTenths", 0) / 10.0D));
        add(lines, "cooldown", data.getIntOr("Cooldown", 0));
        add(lines, "mob_cap", data.getIntOr("MobCap", 0));
        add(lines, "generation", data.getIntOr("Generation", 0));
        add(lines, "generation_ticks", data.getIntOr("GenerationTicks", 0));
        add(lines, "parasites", data.getIntOr("ParasiteCount", 0));
        add(lines, "coth", data.getIntOr("CothCount", 0));
        add(lines, "total_mobs", data.getIntOr("TotalMobs", 0));
        add(lines, "can_gain", yesNo(data.getBooleanOr("CanGain", false)));
        add(lines, "can_lose", yesNo(data.getBooleanOr("CanLose", false)));
    }

    private static void addVectorLines(List<Component> lines, CompoundTag data) {
        add(lines, "dimension", data.getStringOr("Dimension", ""));
        add(lines, "scan_origin", data.getIntOr("CenterX", 0) + ", " + data.getIntOr("CenterZ", 0));
        add(lines, "index", data.getIntOr("Index", 0) + " / " + data.getIntOr("Total", 0));
        if (!data.getBooleanOr("Found", false)) {
            lines.add(Component.translatable("report.csrp.vector.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        add(lines, "position", data.getIntOr("VectorX", 0) + ", " + data.getIntOr("VectorY", 0)
                + ", " + data.getIntOr("VectorZ", 0));
        add(lines, "radius", data.getIntOr("Radius", 0));
        add(lines, "health", data.getIntOr("Health", 0));
        add(lines, "distance", data.getIntOr("Distance", 0));
    }

    private static void addDislodgementLines(List<Component> lines, CompoundTag data) {
        add(lines, "dimension", data.getStringOr("Dimension", ""));
        ListTag events = data.getListOrEmpty("Events");
        if (events.isEmpty()) {
            lines.add(Component.translatable("report.csrp.dislodgement.none")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int index = 0; index < events.size(); index++) {
            CompoundTag event = events.getCompoundOrEmpty(index);
            int code = event.getIntOr("Code", 0);
            String warning = "!".repeat(Math.max(1, event.getIntOr("Threat", 0)));
            lines.add(Component.translatable("report.csrp.dislodgement.event", code, warning)
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            lines.add(Component.translatable("report.csrp.dislodgement.effect",
                    Component.translatable("report.csrp.dislodgement.effect." + code))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("report.csrp.dislodgement.value", event.getIntOr("Value", 0))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("report.csrp.dislodgement.duration", event.getIntOr("Seconds", 0))
                    .withStyle(ChatFormatting.GRAY));
            if (index + 1 < events.size()) {
                lines.add(Component.empty());
            }
        }
    }

    private static void add(List<Component> lines, String key, Object value) {
        lines.add(Component.translatable("report.csrp.field." + key, value)
                .withStyle(ChatFormatting.GRAY));
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static ChatFormatting tierColor(String tier) {
        return switch (tier) {
            case "inborn" -> ChatFormatting.GREEN;
            case "assimilated" -> ChatFormatting.AQUA;
            case "assimara" -> ChatFormatting.DARK_AQUA;
            case "hijacked", "feral" -> ChatFormatting.RED;
            case "crude" -> ChatFormatting.DARK_GRAY;
            case "primitive" -> ChatFormatting.GRAY;
            case "adapted" -> ChatFormatting.GOLD;
            case "nexus" -> ChatFormatting.LIGHT_PURPLE;
            case "deterrent", "ancient" -> ChatFormatting.DARK_PURPLE;
            case "pure" -> ChatFormatting.BLUE;
            case "preeminent" -> ChatFormatting.DARK_GREEN;
            case "derived" -> ChatFormatting.DARK_BLUE;
            default -> ChatFormatting.WHITE;
        };
    }

    private static String formatTime(int ticks) {
        int normalized = Math.floorMod(ticks, 24_000);
        int totalMinutes = Math.floorMod((normalized + 6_000) * 60 / 1_000, 1_440);
        return String.format(Locale.ROOT, "%02d:%02d", totalMinutes / 60, totalMinutes % 60);
    }

    public enum Type {
        SCAN("scan"),
        PHASE("phase"),
        VECTOR("vector"),
        DISLODGEMENT("dislodgement");

        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Type byId(String id) {
            for (Type value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return SCAN;
        }
    }
}
