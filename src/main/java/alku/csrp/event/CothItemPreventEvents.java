package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Port of {@code SRPEventHandlerBus#itemEntity} (out109 lines 927-973).
 *
 * <p>Using an item listed in {@code SRPConfigSystems.COTHItemPrevent} (default golden apple 10% /
 * golden carrot 3%, 300 s / 150 s) on a living entity can mark it with the Repel effect. The original
 * consumed one item unless the player was in creative, and rolled the configured chance; creative
 * players always applied the effect without consuming the item.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class CothItemPreventEvents {
    private CothItemPreventEvents() {
    }

    @SubscribeEvent
    public static void applyRepelItem(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Player player = event.getEntity();
        for (String row : Config.cothItemPrevent()) {
            String[] parts = row.split(";", -1);
            if (parts.length != 3 || !parts[0].trim().equals(itemId)) {
                continue;
            }
            double chance;
            int seconds;
            try {
                chance = Double.parseDouble(parts[1].trim());
                seconds = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (player.getAbilities().instabuild) {
                target.addEffect(new MobEffectInstance(ModMobEffects.REPEL, seconds * 20, 0));
            } else {
                stack.shrink(1);
                if (player.getRandom().nextDouble() < chance) {
                    target.addEffect(new MobEffectInstance(ModMobEffects.REPEL, seconds * 20, 0));
                }
            }
        }
    }
}
