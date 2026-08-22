package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class RupterKillMilestoneEvents {
    public static final String RUPTER_KILL_COUNT_KEY = "csrpRupterKills";
    public static final int RUPTER_KILL_TARGET = 1000;
    private static final String CRITERION = "reached_1000_rupter_kills";
    private static final ResourceLocation ADVANCEMENT_ID =
            new ResourceLocation(Csrp.MODID, "cut_roots");

    private RupterKillMilestoneEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().getType() != ModEntities.RUPTER.get()
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int kills = data.getInt(RUPTER_KILL_COUNT_KEY) + 1;
        data.putInt(RUPTER_KILL_COUNT_KEY, kills);
        if (kills < RUPTER_KILL_TARGET) {
            return;
        }

        AdvancementHolder advancement = player.server.getAdvancements().get(ADVANCEMENT_ID);
        if (advancement != null) {
            player.getAdvancements().award(advancement, CRITERION);
        }
    }

    @SubscribeEvent
    public static void copyKillCount(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        int kills = event.getOriginal().getPersistentData().getInt(RUPTER_KILL_COUNT_KEY);
        event.getEntity().getPersistentData().putInt(RUPTER_KILL_COUNT_KEY, kills);
    }
}
