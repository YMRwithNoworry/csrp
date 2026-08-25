package alku.csrp.event;

import alku.csrp.Csrp;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Displays the project notice and supporter credits when a player joins. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class WelcomeMessageEvents {
    private static final ChatFormatting[] RAINBOW = {
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE
    };

    private WelcomeMessageEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.sendSystemMessage(Component.empty()
                .append(Component.literal("亲爱的玩家您好，我是csrp的开发者泡椒味泡椒")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append("\n")
                .append(Component.literal("此模组为不侵犯srp原开发组利益，不会上传模组平台")
                        .withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.literal("csrp开发组为移植此模组已Coding了上百个小时")
                        .withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.literal("如果您愿意支持我们的话可以到QQ群中（1081198408）进行赞助")
                        .withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.literal("感谢您").withStyle(ChatFormatting.GOLD))
                .append("\n")
                .append(Component.literal("以下是感谢名单：").withStyle(ChatFormatting.GOLD))
                .append("\n")
                .append(rainbowNames()));
    }

    private static MutableComponent rainbowNames() {
        String[] names = {
                "烈焰幽阳(最大的支持者)", "ByteFish", "小真", "HYhachiiy", "therriau",
                "Roʊfi", "乌鸦", "阿那克萨戈拉斯", "Blue不是高坚果", "单身狗保护协会", "ArcticHu"
        };
        MutableComponent result = Component.empty();
        int colorIndex = 0;
        for (int nameIndex = 0; nameIndex < names.length; nameIndex++) {
            if (nameIndex > 0) {
                result.append(Component.literal(" ").withStyle(ChatFormatting.WHITE));
            }
            for (int codePoint : names[nameIndex].codePoints().toArray()) {
                result.append(Component.literal(new String(Character.toChars(codePoint)))
                        .withStyle(RAINBOW[colorIndex++ % RAINBOW.length]));
            }
        }
        return result;
    }
}
