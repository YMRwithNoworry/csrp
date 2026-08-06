package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import java.util.Comparator;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Remaining Cognitio achievements: Ricardo shearing, guerilla kills, creeper
 * assisted kills, and the fifty-kills-in-one-day hunt season.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class CognitioEvents {
    private static final String HUNT_DAY_KEY = "csrpHuntDay";
    private static final String HUNT_COUNT_KEY = "csrpHuntCount";
    private static final int HUNT_TARGET = 50;

    private CognitioEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Entity target = event.getTarget();
        if (target.getType() != ModEntities.PRI_REEKER.get()
                || target.getCustomName() == null
                || !target.getCustomName().getString().equals("Ricardo")
                || !event.getEntity().getMainHandItem().is(Items.SHEARS)) {
            return;
        }
        award((ServerPlayer) event.getEntity(), "tricked_me", "sheared_ricardo");
        ServerLevel level = (ServerLevel) event.getLevel();
        level.sendParticles(ParticleTypes.WAX_OFF,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                12, 0.3D, 0.3D, 0.3D, 0.02D);
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
            award(killer, "guerilla", "killed_fearful_player");
        }
        if (victim instanceof Parasite && event.getSource().is(DamageTypes.EXPLOSION)) {
            awardNearest(victim, "enemy_of_my_enemy", "creeper_kill");
        }
        if (victim instanceof Parasite && sourceEntity instanceof ServerPlayer hunter) {
            trackHuntSeason(hunter);
        }
    }

    private static void trackHuntSeason(ServerPlayer player) {
        var data = player.getPersistentData();
        long day = player.level().getDayTime() / 24000L;
        if (data.getLong(HUNT_DAY_KEY) != day) {
            data.putLong(HUNT_DAY_KEY, day);
            data.putInt(HUNT_COUNT_KEY, 0);
        }
        int count = data.getInt(HUNT_COUNT_KEY) + 1;
        data.putInt(HUNT_COUNT_KEY, count);
        if (count >= HUNT_TARGET) {
            data.putInt(HUNT_COUNT_KEY, 0);
            award(player, "hunt_season", "fifty_in_day");
        }
    }

    private static void awardNearest(LivingEntity victim, String advancement, String criterion) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        level.getEntitiesOfClass(ServerPlayer.class, new AABB(victim.blockPosition()).inflate(24.0D))
                .stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(victim)))
                .ifPresent(player -> award(player, advancement, criterion));
    }

    private static void award(ServerPlayer player, String advancement, String criterion) {
        AdvancementHolder holder = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, advancement));
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }
}
