package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.network.BiomeFogPayload;
import alku.csrp.network.NodeHeartPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of the parasite biome ambience half of the 1.10.9 {@code SRPEventHandlerBus#playerTick}
 * ({@code SRPEventHandlerBus.java:2237-2291}) — the biome fog ramp and the node heart sound.  Both live
 * in one per-player tick in the original, so they live in one method here too.
 *
 * <p><strong>Fog.</strong> Every player is ramped towards {@code SRPConfigWorld.biomeFogDensity} at
 * {@code 4.5E-4F} per tick while they are inside a parasite biome or inside a node's effect range, and
 * away from it at {@code 8.0E-4F} per tick once neither holds.  The colour comes from the biome: the
 * terrain biome when the player stands in one, otherwise the biome the nearest covering node's type
 * maps onto ({@code SRPReference#getBiomeFromInt}).  A packet is only sent while the value is still
 * changing, exactly like the original.</p>
 *
 * <p><strong>Node heart.</strong> A shared counter is bumped once per player tick; every
 * {@code biomeHeartFreq} (default 30) ticks it resets and, when the current player stands in a parasite
 * biome, the node heart sound is sent with the volume
 * {@code 1 - isInRangeOfHeart(pos, spreadRadius²) / spreadRadius²} — the closer the node, the louder.</p>
 *
 * <p><strong>Faithful quirks:</strong> 1.10.9 kept the fog ramp and the heart counter in the
 * <em>static / instance</em> fields {@code SRPEventHandlerBus.fog*} and {@code SRPEventHandlerBus.heart},
 * so a server shares one fog accumulator and one heart counter between every player (and every
 * dimension) — with two players the heart fires twice as often and the last player ticked decides the
 * fog everyone sees.  The port keeps both, because changing them would alter the original's
 * behaviour.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteBiomeAmbienceServer {
    /** {@code fog = Math.min(fog + 4.5E-4F, biomeFogDensity)}. */
    private static final float RAMP_UP = 4.5E-4F;
    /** {@code fog = Math.max(fog - 8.0E-4F, 0.0F)}. */
    private static final float RAMP_DOWN = 8.0E-4F;

    private static float fog;
    private static float fogRed;
    private static float fogGreen;
    private static float fogBlue;
    /** {@code SRPEventHandlerBus.heart} — the original shared one counter between every player. */
    private static int heart;

    private ParasiteBiomeAmbienceServer() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            tick(level, player);
        }
    }

    /** One player's {@code SRPEventHandlerBus#playerTick} ambience tick. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        SrpWorldData data = SrpWorldData.get(level);
        BlockPos pos = player.blockPosition();
        int terrainBiome = ParasiteBiomeDecorator.parasiteBiomeIndex(level.getBiome(pos));
        boolean inParasiteBiome = terrainBiome != 0;
        int heartAge = data.nearestHeartAge(pos);
        float density = WorldConfig.biomeFogDensity();

        // `if (canBiomeStillExist(world, pos, false) < 1 && !isParaBiome)`.
        if (heartAge < 1 && !inParasiteBiome) {
            if (fog > 0.0F) {
                fog = Math.max(fog - RAMP_DOWN, 0.0F);
                fogRed = 0.0F;
                fogGreen = 0.0F;
                fogBlue = 0.0F;
                BiomeFogPayload.sendTo(player, fog, 0.0F, 0.0F, 0.0F);
            }
        } else {
            int biome = inParasiteBiome
                    ? terrainBiome
                    : ParasiteBiomeDecorator.nodeBiomeIndex(data.nearestHeartType(pos));
            fog = Math.min(fog + RAMP_UP, density);
            fogRed = WorldConfig.biomeFogRed(biome) / 255.0F;
            fogGreen = WorldConfig.biomeFogGreen(biome) / 255.0F;
            fogBlue = WorldConfig.biomeFogBlue(biome) / 255.0F;
            if (fog < density) {
                BiomeFogPayload.sendTo(player, fog, fogRed, fogGreen, fogBlue);
            }
        }

        // `this.heart++; if (this.heart < SRPConfigWorld.biomeHeartFreq) return; this.heart = 0;`
        if (++heart < WorldConfig.biomeHeartFreq()) {
            return;
        }
        heart = 0;
        if (inParasiteBiome) {
            playHeart(data, pos, player);
        }
    }

    /** {@code if (isParaBiome) { ... SRPPacketMovingSound(-1, vol) ... }}. */
    private static void playHeart(SrpWorldData data, BlockPos pos, ServerPlayer player) {
        int age = data.nearestHeartAge(pos, true);
        if (age <= 0) {
            return;
        }
        int spreadSquared = data.getDistanceSpreadByAge(age, true);
        if (spreadSquared <= 0) {
            // The original divides by this radius; with a 0 spread range that is a division by zero.
            return;
        }
        int distanceSquared = data.isInRangeOfHeart(pos, spreadSquared);
        float volume = ((float) distanceSquared / spreadSquared - 1.0F) * -1.0F;
        NodeHeartPayload.sendTo(player, volume);
    }

    /** The ramped fog value, for tests and diagnostics. */
    public static float fog() {
        return fog;
    }

    /** Test hook: {@code SRPEventHandlerBus.fog} and {@code heart} start at 0 on world load. */
    static void reset() {
        fog = 0.0F;
        fogRed = 0.0F;
        fogGreen = 0.0F;
        fogBlue = 0.0F;
        heart = 0;
    }
}
