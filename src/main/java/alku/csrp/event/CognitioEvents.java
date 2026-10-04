package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Cognitio achievements: Ricardo shearing, guerilla kills, creeper assisted
 * kills, and the fifty-kills-in-one-day hunt season.
 *
 * <p>Criterion names follow SRP 1.10.9 exactly: {@code kill_fear_splashed}
 * (guerilla), {@code creeper_killed_parasite} (enemy_enemy) and
 * {@code fifty_in_day} (hunt_season).
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class CognitioEvents {
    /** Rolling-window bookkeeping, same keys as the original HuntSeasonAdvancement. */
    public static final String HUNT_START_KEY = "srpHuntStart";
    public static final String HUNT_COUNT_KEY = "srpHuntCount";
    private static final long HUNT_WINDOW_TICKS = 24000L;
    private static final int HUNT_TARGET = 50;

    private static final String GUERILLA = "guerilla";
    private static final String GUERILLA_CRITERION = "kill_fear_splashed";
    private static final String ENEMY_ENEMY = "enemy_enemy";
    private static final String ENEMY_ENEMY_CRITERION = "creeper_killed_parasite";
    private static final String HUNT_SEASON = "hunt_season";
    private static final String HUNT_SEASON_CRITERION = "fifty_in_day";
    private static final double CREEPER_ASSIST_RANGE_SQR = 4096.0D;

    private CognitioEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof AssimilatedParasiteEntity wolf
                && wolf.getKind() == AssimilatedParasiteEntity.Kind.WOLF
                && event.getItemStack().is(Items.NAME_TAG)
                && event.getItemStack().get(DataComponents.CUSTOM_NAME) != null
                && event.getItemStack().get(DataComponents.CUSTOM_NAME).getString().equals("Paojiao134")
                && event.getEntity() instanceof ServerPlayer player) {
            AdvancementGrants.award(player, "paojiao_wolf", "named_wolf");
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (victim instanceof Player && victim.hasEffect(ModMobEffects.FEAR)
                && sourceEntity instanceof ServerPlayer killer) {
            AdvancementGrants.award(killer, GUERILLA, GUERILLA_CRITERION);
        }
        // SRP checks the killer entity type (EntityCreeper), not the damage type.
        if (victim instanceof Parasite && sourceEntity instanceof Creeper) {
            awardCreeperAssist(victim);
        }
        if (victim instanceof Parasite && sourceEntity instanceof ServerPlayer hunter) {
            trackHuntSeason(hunter);
        }
    }

    /** Every player within 64 blocks of the corpse, mirroring EnemyOfMyEnemyAdvancement. */
    private static void awardCreeperAssist(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(victim) <= CREEPER_ASSIST_RANGE_SQR) {
                AdvancementGrants.award(player, ENEMY_ENEMY, ENEMY_ENEMY_CRITERION);
            }
        }
    }

    /**
     * Rolling window: the first kill starts the window, further kills within
     * 24000 ticks increase the count, otherwise the window restarts at one.
     */
    private static void trackHuntSeason(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();
        long start = data.getLong(HUNT_START_KEY);
        int count;
        if (start != 0L && now - start <= HUNT_WINDOW_TICKS) {
            count = data.getInt(HUNT_COUNT_KEY) + 1;
        } else {
            start = now;
            count = 1;
        }
        data.putLong(HUNT_START_KEY, start);
        data.putInt(HUNT_COUNT_KEY, count);
        if (count >= HUNT_TARGET) {
            AdvancementGrants.award(player, HUNT_SEASON, HUNT_SEASON_CRITERION);
        }
    }
}
