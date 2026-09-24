package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteNexusProtection1}.
 *
 * <p>The first nexus protection: a 6..13 block feeler/fog shell around the nexus column, the inner
 * two 5-high discs at {@code y - 5} and a felt disc on the ground.  Called by the Dispatcher nidus
 * entity in the original ({@code EntityPDispatcher} line 278) and by the {@code /srp summon_nidus}
 * command; both live outside this task's write scope, so the entry point is public here.</p>
 */
public final class WorldGenParasiteNexusProtection1 extends WorldGenParasiteColonyBase {
    public WorldGenParasiteNexusProtection1(int stage) {
        super(stage);
        this.wall = ParasiteGenContext.DENSE_WALL;
        this.tacle = ParasiteGenContext.STAIN_FEELER;
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        generateSphere(level, posss, 3, 3, rand, false, 6, false, 1, 1, 5,
                ParasiteGenContext.STAIN_FLESH, ParasiteGenContext.STAIN_FEELER,
                ParasiteGenContext.FOG, 2);

        generateCircle(ParasiteGenContext.STAIN_FEELER, ParasiteGenContext.STAIN_FLESH, level, rand,
                posss.below(5), 6, 6, 5, 2, 0);
        generateCircle(ParasiteGenContext.FOG, ParasiteGenContext.FOG, level, rand,
                posss.below(5), 4, 4, 5, 20_000_000, 0);

        replaceCircleGround(level, posss, 8, ParasiteGenContext.STAIN_FEELER);
        return true;
    }
}
