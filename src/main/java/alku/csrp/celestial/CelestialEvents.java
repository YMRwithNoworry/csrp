package alku.csrp.celestial;

import alku.csrp.Csrp;
import alku.csrp.compendium.CompendiumEvents;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class CelestialEvents {
    private CelestialEvents() {
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        CelestialSystem.tick(level);
        if (level.getGameTime() % 20L != 0L) return;
        for (ServerPlayer player : level.players()) {
            if (!player.getInventory().contains(ModItems.SRP_FIELD_GUIDE.get().getDefaultInstance())) continue;
            CelestialSystem.visible(level).forEach(id -> {
                if (id.equals(CelestialSystem.DARK_DAYS) || isNight(level)) {
                    CompendiumEvents.markCelestial(player, id);
                }
            });
        }
    }

    @SubscribeEvent
    public static void applyTwentySeven(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Parasite)
                || !(event.getEntity() instanceof LivingEntity living) || !isNight(level)
                || !CelestialSystem.isActive(level, "twenty_seven")) return;
        living.addEffect(new MobEffectInstance(ModMobEffects.RAGE.get(), 12000, 1, false, false));
    }

    @SubscribeEvent
    public static void dropArrowShrimp(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Parasite)
                || !(event.getEntity().level() instanceof ServerLevel level)
                || !CelestialSystem.isActive(level, "arrow") || level.random.nextFloat() >= 0.25F) return;
        ItemStack shrimp = new ItemStack(ModItems.SHRIMP.get(), 1 + level.random.nextInt(5));
        event.getDrops().add(new ItemEntity(level, event.getEntity().getX(), event.getEntity().getY(),
                event.getEntity().getZ(), shrimp));
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CelestialSystem.sync(player);
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CelestialSystem.sync(player);
    }

    private static boolean isNight(ServerLevel level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return time >= 13000L && time <= 23000L;
    }
}
