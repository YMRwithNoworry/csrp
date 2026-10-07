package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Port of the Fear item/block failures from {@code SRPEventHandlerBus}:
 * {@code #itemPlayer} (out109 lines 974-1002) and {@code #blockPlayer} (1003-1026).
 *
 * <p>While the player carries Fear, the original could make item use and block interaction fail:
 * <ul>
 *   <li>{@code itemPlayer}: Fear I+ and a {@code fearItemChance} roll on an item not in
 *       {@code fearItemBlackList} cancels the item use with the {@code fearitem} message;</li>
 *   <li>{@code blockPlayer}: Fear II+ with a {@code fearBlockChance} roll denies the block and shows
 *       {@code fearblock}; otherwise Fear I+ with a {@code fearItemChance} roll denies the item and
 *       shows {@code fearitem}.</li>
 * </ul>
 * All three chances default to 0.0, so the feature is inert until configured, exactly like the
 * original.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class FearUseEvents {
    private FearUseEvents() {
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || !Config.fearActive()) {
            return;
        }
        MobEffectInstance fear = player.getEffect(ModMobEffects.FEAR);
        if (fear == null || fear.getAmplifier() < 1) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(event.getItem().getItem()).toString();
        if (Config.fearItemExempt(itemId)) {
            return;
        }
        if (player.getRandom().nextDouble() < Config.fearItemChance()) {
            player.sendOverlayMessage(
                    Component.translatable("message.csrp.fearitem").withStyle(ChatFormatting.RED));
            event.setDuration(-1);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!Config.fearActive()) {
            return;
        }
        MobEffectInstance fear = player.getEffect(ModMobEffects.FEAR);
        if (fear == null) {
            return;
        }
        int amplifier = fear.getAmplifier();
        if (amplifier >= 2 && player.getRandom().nextDouble() < Config.fearBlockChance()) {
            player.sendOverlayMessage(
                    Component.translatable("message.csrp.fearblock").withStyle(ChatFormatting.RED));
            event.setUseBlock(TriState.FALSE);
        } else if (amplifier >= 1 && player.getRandom().nextDouble() < Config.fearItemChance()) {
            player.sendOverlayMessage(
                    Component.translatable("message.csrp.fearitem").withStyle(ChatFormatting.RED));
            event.setUseItem(TriState.FALSE);
        }
    }
}
