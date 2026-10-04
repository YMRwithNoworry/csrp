package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * "monument" and "qliphoth_overload": the two stage IV nexus detection
 * advancements.
 *
 * <p>Ported from SRP 1.10.9 {@code BeckonStage4DetectedAdvancement} and
 * {@code DispatcherStage4DetectedAdvancement}: whenever a Beckon SIV or a
 * Dispatcher SIV enters a server level, every player within 256 blocks
 * (squared distance &lt;= 65536) of the same dimension is granted the matching
 * criterion, and the detection sting is played for the players that actually
 * gained the advancement on this occasion.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class NexusDetectionEvents {
    private static final double DETECTION_RANGE_SQR = 65536.0D;

    private static final String BECKON_ADVANCEMENT = "monument";
    private static final String BECKON_CRITERION = "detected_beckon_siv";
    private static final String BECKON_SOUND = "dislo.s11";

    private static final String DISPATCHER_ADVANCEMENT = "qliphoth_overload";
    private static final String DISPATCHER_CRITERION = "detected_dispatcher_siv";
    private static final String DISPATCHER_SOUND = "dislo.s28";

    private NexusDetectionEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity.getType() == ModEntities.BECKON_SIV.get()) {
            detect(level, entity, BECKON_ADVANCEMENT, BECKON_CRITERION, BECKON_SOUND);
        } else if (entity.getType() == ModEntities.DISPATCHER_SIV.get()) {
            detect(level, entity, DISPATCHER_ADVANCEMENT, DISPATCHER_CRITERION, DISPATCHER_SOUND);
        }
    }

    private static void detect(ServerLevel level, Entity source, String advancement,
            String criterion, String sound) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(source) > DETECTION_RANGE_SQR) {
                continue;
            }
            if (AdvancementGrants.award(player, advancement, criterion)) {
                playDetectionSound(player, sound);
            }
        }
    }

    private static void playDetectionSound(ServerPlayer player, String name) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT
                .get(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, name));
        if (sound != null) {
            player.playNotifySound(sound, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}
