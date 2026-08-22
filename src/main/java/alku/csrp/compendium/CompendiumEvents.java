package alku.csrp.compendium;

import alku.csrp.network.CsrpNetwork;
import alku.csrp.Csrp;
import alku.csrp.compendium.network.CompendiumUnlockPayload;
import alku.csrp.entity.Parasite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class CompendiumEvents {
    private CompendiumEvents() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            ResourceLocation victimId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            if (victimId.getNamespace().equals(Csrp.MODID)) {
                CompendiumSavedData data = CompendiumSavedData.get(killer.getServer());
                int kills = data.progress(killer.getUUID()).addKill(victimId.toString());
                data.changed();
                if (kills == 1) {
                    notifyUnlock(killer, "entity");
                }
            }
        }
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof Parasite) {
            CompendiumSavedData data = CompendiumSavedData.get(player.getServer());
            data.progress(player.getUUID()).addDeathByParasites();
            data.changed();
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        float amount = event.getNewDamage();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() instanceof Parasite) {
            CompendiumSavedData data = CompendiumSavedData.get(attacker.getServer());
            data.progress(attacker.getUUID()).addDamageToParasites(amount);
            data.changed();
        }
        if (event.getEntity() instanceof ServerPlayer victim && event.getSource().getEntity() instanceof Parasite) {
            CompendiumSavedData data = CompendiumSavedData.get(victim.getServer());
            data.progress(victim.getUUID()).addDamageFromParasites(amount);
            data.changed();
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getOriginalStack().getItem() instanceof BlockItem blockItem)) {
            return;
        }
        unlockBlock(player, BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(
                event.getLevel().getBlockState(event.getPos()).getBlock());
        unlockBlock(player, id);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(event.getEffectInstance().getEffect());
        if (!id.getNamespace().equals(Csrp.MODID)) {
            return;
        }
        CompendiumSavedData data = CompendiumSavedData.get(player.getServer());
        if (data.progress(player.getUUID()).unlockEffect(id.toString())) {
            data.changed();
            notifyUnlock(player, "effect");
        }
    }

    public static void markCelestial(ServerPlayer player, String celestial) {
        if (!CompendiumCatalog.CELESTIALS.contains(celestial)) {
            return;
        }
        CompendiumSavedData data = CompendiumSavedData.get(player.getServer());
        if (data.progress(player.getUUID()).unlockCelestial(celestial)) {
            data.changed();
            notifyUnlock(player, "celestial");
        }
    }

    private static void unlockBlock(ServerPlayer player, ResourceLocation id) {
        if (!id.getNamespace().equals(Csrp.MODID)) {
            return;
        }
        CompendiumSavedData data = CompendiumSavedData.get(player.getServer());
        if (data.progress(player.getUUID()).unlockBlock(id.toString())) {
            data.changed();
            notifyUnlock(player, "block");
        }
    }

    private static void notifyUnlock(ServerPlayer player, String category) {
        CsrpNetwork.sendToPlayer(player, new CompendiumUnlockPayload(category));
    }
}
