package alku.csrp.world.star;

import alku.csrp.world.SrpStarType;
import alku.csrp.world.SrpWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * 26.3 shape of the 1.10.9 {@code GenLayerSRPDynamicStar}.
 *
 * <p>1.12.2 built a {@code GenLayer} chain - the vanilla parent layer plus a
 * {@code GenLayerSRPColdStar} and a {@code GenLayerSRPWarmStar} child - and
 * {@code getInts(areaX, areaY, areaWidth, areaHeight)} dispatched on
 * {@code SRPStarWorldEvents.getActiveStarTypeForGeneration()}. 26.3 deleted the whole
 * {@code GenLayer} stack: biome layout is produced by the datapack multi-noise
 * {@link BiomeSource}, which a mod cannot swap per world at generation time through a public hook.</p>
 *
 * <p>What survives is the decision the original class actually made, and that is kept here:</p>
 * <ul>
 *   <li>{@link #select} is the three-way choice ({@code cold} / {@code warm} / {@code original}),
 *       expressed over biome sources instead of layers;</li>
 *   <li>{@link #activeGenerationStarType} is the 26.3 form of
 *       {@code SRPStarWorldEvents#getActiveStarTypeForGeneration()} - the star type the overworld
 *       generator must honour. It reads {@link SrpWorldData}, the same single source every other
 *       star consumer in the port uses.</li>
 * </ul>
 *
 * <p>The live biome swap itself is performed by {@code world/StarBiomeGenerationEvents} (biome
 * replacement on newly generated chunks) which is outside this class; this type exists so the
 * generation-time star source is not duplicated across the star features.</p>
 */
public final class GenLayerSRPDynamicStar {
    private GenLayerSRPDynamicStar() {
    }

    /** 1.10.9 {@code GenLayerSRPDynamicStar#getInts} dispatch, over 26.3 biome sources. */
    public static BiomeSource select(SrpStarType starType, BiomeSource original, BiomeSource cold,
            BiomeSource warm) {
        return switch (starType) {
            case COLD -> cold;
            case WARM -> warm;
            case NORMAL -> original;
        };
    }

    /** 1.10.9 {@code SRPStarWorldEvents#getActiveStarTypeForGeneration()}. */
    public static SrpStarType activeGenerationStarType(ServerLevel overworld) {
        return SrpWorldData.get(overworld).starType();
    }
}
