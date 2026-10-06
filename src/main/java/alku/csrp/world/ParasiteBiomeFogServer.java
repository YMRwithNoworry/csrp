package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.network.BiomeFogPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of the parasite biome fog half of the 1.10.9 {@code SRPEventHandlerBus#playerTick}
 * ({@code SRPEventHandlerBus.java:2237-2277}).
 *
 * <p>Every player is ramped towards {@code SRPConfigWorld.biomeFogDensity} at {@code 4.5E-4F} per tick
 * while they are inside a parasite biome or inside a node's effect range, and away from it at
 * {@code 8.0E-4F} per tick once neither holds.  The colour comes from the biome: the terrain biome
 * when the player stands in one, otherwise the biome the nearest covering node's type maps onto
 * ({@code SRPReference#getBiomeFromInt}).  A packet is only sent while the value is still changing,
 * exactly like the original.</p>
 *
 * <p><strong>Faithful quirk:</strong> 1.10.9 kept the ramp in the <em>static</em>
 * {@code SRPEventHandlerBus.fog / fogRed / fogGreen / fogBlue} fields, so a server shares one
 * accumulator between every player (and every dimension) and the last player ticked wins.  The port
 * keeps that, because changing it would alter the original's behaviour.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteBiomeFogServer {
    /** {@code fog = Math.min(fog + 4.5E-4F, biomeFogDensity)}. */
    private static final float RAMP_UP = 4.5E-4F;
    /** {@code fog = Math.max(fog - 8.0E-4F, 0.0F)}. */
    private static final float RAMP_DOWN = 8.0E-4F;

    private static float fog;
    private static float fogRed;
    private static float fogGreen;
    private static float fogBlue;

    private ParasiteBiomeFogServer() {
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

    /** One player's {@code SRPEventHandlerBus#playerTick} fog ramp. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        SrpWorldData data = SrpWorldData.get(level);
        BlockPos pos = player.blockPosition();
        int terrainBiome = ParasiteBiomeDecorator.parasiteBiomeIndex(level.getBiome(pos));
        int heartAge = data.nearestHeartAge(pos);
        float density = WorldConfig.biomeFogDensity();

        // `if (canBiomeStillExist(world, pos, false) < 1 && !isParaBiome)`.
        if (heartAge < 1 && terrainBiome == 0) {
            if (fog > 0.0F) {
                fog = Math.max(fog - RAMP_DOWN, 0.0F);
                fogRed = 0.0F;
                fogGreen = 0.0F;
                fogBlue = 0.0F;
                BiomeFogPayload.sendTo(player, fog, 0.0F, 0.0F, 0.0F);
            }
            return;
        }

        int biome = terrainBiome != 0
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

    /** The ramped values, for tests and diagnostics. */
    public static float fog() {
        return fog;
    }

    /** Test hook: {@code SRPEventHandlerBus.fog} starts at 0 on world load. */
    static void reset() {
        fog = 0.0F;
        fogRed = 0.0F;
        fogGreen = 0.0F;
        fogBlue = 0.0F;
    }
}
