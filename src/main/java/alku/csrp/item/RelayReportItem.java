package alku.csrp.item;

import alku.csrp.relay.network.RelayReportOpenPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            PacketDistributor.sendToPlayer(serverPlayer, new RelayReportOpenPayload(type.id, data));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tooltip.add(Component.translatable("tooltip.csrp.relay_report.read")
                .withStyle(ChatFormatting.GRAY));
        if (data.contains("PrintDay")) {
            tooltip.add(Component.translatable("tooltip.csrp.relay_report.printed",
                    data.getInt("PrintDay"), formatTime(data.getInt("PrintTime")))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static List<Component> reportLines(Type type, CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("report.csrp." + type.id + ".title")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        lines.add(Component.translatable("report.csrp.printed", data.getInt("PrintDay"),
                formatTime(data.getInt("PrintTime"))).withStyle(ChatFormatting.DARK_GRAY));
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
        add(lines, "dimension", data.getString("Dimension"));
        add(lines, "total_mobs", data.getInt("TotalMobs"));
        add(lines, "total_parasites", data.getInt("TotalParasites"));
        add(lines, "share", String.format(Locale.ROOT, "%.1f%%", data.getInt("ShareTenths") / 10.0D));
        add(lines, "ratio", data.getString("Ratio"));
        lines.add(Component.empty());
        lines.add(Component.translatable("report.csrp.scan.tiers").withStyle(ChatFormatting.DARK_GRAY));
        ListTag tiers = data.getList("Tiers", Tag.TAG_STRING);
        for (int index = 0; index < tiers.size(); index++) {
            String tier = tiers.getString(index);
            lines.add(Component.translatable("report.csrp.scan.tier",
                    Component.translatable("report.csrp.tier." + tier), data.getInt("Tier_" + tier))
                    .withStyle(tierColor(tier)));
        }
    }

    private static void addPhaseLines(List<Component> lines, CompoundTag data) {
        add(lines, "dimension", data.getString("Dimension"));
        add(lines, "phase", data.getInt("Phase"));
        add(lines, "points", data.getInt("Points"));
        add(lines, "next_points", data.getInt("NextPoints"));
        add(lines, "progress", String.format(Locale.ROOT, "%.1f%%", data.getInt("ProgressTenths") / 10.0D));
        add(lines, "cooldown", data.getInt("Cooldown"));
        add(lines, "mob_cap", data.getInt("MobCap"));
        add(lines, "generation", data.getInt("Generation"));
        add(lines, "generation_ticks", data.getInt("GenerationTicks"));
        add(lines, "parasites", data.getInt("ParasiteCount"));
        add(lines, "coth", data.getInt("CothCount"));
        add(lines, "total_mobs", data.getInt("TotalMobs"));
        add(lines, "can_gain", yesNo(data.getBoolean("CanGain")));
        add(lines, "can_lose", yesNo(data.getBoolean("CanLose")));
    }

    private static void addVectorLines(List<Component> lines, CompoundTag data) {
        add(lines, "dimension", data.getString("Dimension"));
        add(lines, "scan_origin", data.getInt("CenterX") + ", " + data.getInt("CenterZ"));
        add(lines, "index", data.getInt("Index") + " / " + data.getInt("Total"));
        if (!data.getBoolean("Found")) {
            lines.add(Component.translatable("report.csrp.vector.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        add(lines, "position", data.getInt("VectorX") + ", " + data.getInt("VectorY")
                + ", " + data.getInt("VectorZ"));
        add(lines, "radius", data.getInt("Radius"));
        add(lines, "health", data.getInt("Health"));
        add(lines, "distance", data.getInt("Distance"));
    }

    private static void addDislodgementLines(List<Component> lines, CompoundTag data) {
        add(lines, "dimension", data.getString("Dimension"));
        ListTag events = data.getList("Events", Tag.TAG_COMPOUND);
        if (events.isEmpty()) {
            lines.add(Component.translatable("report.csrp.dislodgement.none")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int index = 0; index < events.size(); index++) {
            CompoundTag event = events.getCompound(index);
            int code = event.getInt("Code");
            String warning = "!".repeat(Math.max(1, event.getInt("Threat")));
            lines.add(Component.translatable("report.csrp.dislodgement.event", code, warning)
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            lines.add(Component.translatable("report.csrp.dislodgement.effect",
                    Component.translatable("report.csrp.dislodgement.effect." + code))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("report.csrp.dislodgement.value", event.getInt("Value"))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("report.csrp.dislodgement.duration", event.getInt("Seconds"))
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
