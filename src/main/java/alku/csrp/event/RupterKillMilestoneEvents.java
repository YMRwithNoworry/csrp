package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class RupterKillMilestoneEvents {
    public static final String RUPTER_KILL_COUNT_KEY = "csrpRupterKills";
    public static final int RUPTER_KILL_TARGET = 1000;
    private static final String CRITERION = "reached_1000_rupter_kills";
    private static final Identifier ADVANCEMENT_ID =
            Identifier.fromNamespaceAndPath(Csrp.MODID, "cut_roots");

    private RupterKillMilestoneEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().getType() != ModEntities.RUPTER.get()
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int kills = data.getIntOr(RUPTER_KILL_COUNT_KEY, 0) + 1;
        data.putInt(RUPTER_KILL_COUNT_KEY, kills);
        if (kills < RUPTER_KILL_TARGET) {
            return;
        }

        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ADVANCEMENT_ID);
        if (advancement != null) {
            player.getAdvancements().award(advancement, CRITERION);
        }
    }

    @SubscribeEvent
    public static void copyKillCount(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        int kills = event.getOriginal().getPersistentData().getIntOr(RUPTER_KILL_COUNT_KEY, 0);
        event.getEntity().getPersistentData().putInt(RUPTER_KILL_COUNT_KEY, kills);
    }
}
