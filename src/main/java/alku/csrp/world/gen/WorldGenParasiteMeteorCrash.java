package alku.csrp.world.gen;

import alku.csrp.Csrp;
import alku.csrp.world.MeteorImpactUtil;
import alku.csrp.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteMeteorCrash}.
 *
 * <p>Line-by-line port of the class the meteor projectile runs on impact.  {@code type != 5} is the
 * fragment meteor (a small {@code meteor_fragment_*} template plus 18..35 fires in a radius of 10);
 * {@code type == 5} is the root meteor, which carves the huge crater through the shared colony
 * primitives and then scatters the {@code meteor} satellite template and its loot tumors.</p>
 *
 * <p>Differences from the earlier hand-rolled {@code MeteorCrashFeature} body this replaces, all of
 * them restorations of the original:</p>
 * <ol>
 *   <li>The air carve now runs through {@link #generateSphere} with three air states and the
 *       incomplete roll of 2, exactly as the original did, instead of a bespoke
 *       {@code carveAirSphere}/{@code carveCircleAir} pair.  The visible effect is the same (every
 *       interior block of the ellipse is cleared with probability 1 - 1/incomplete and the rim roll
 *       is a no-op because {@code flagAir} is set), but the original's own growth arithmetic — the
 *       two per-layer boolean draws that are discarded when {@code random} is false, and the
 *       elliptical normalisation inside {@code generateCircle} — is preserved.</li>
 *   <li>{@code replaceCircleGround} and {@code placeLoot} come from
 *       {@link WorldGenParasiteColonyBase} / {@link ParasiteGenContext} rather than being
 *       re-implemented locally.</li>
 *   <li>The {@code markMainMeteor} call the earlier body added is gone: the original crash path never
 *       called it ({@code markMainMeteor}/{@code isNearMainMeteor} had no caller at all in 1.10.9).
 *       The API stays available on {@link MeteorImpactUtil} but is no longer invoked here.</li>
 * </ol>
 *
 * <p>{@code StructurePlacer} replaces {@code WorldGenStructure}: the original computed
 * {@code origin = surface.add(2, 2, 2)} and then placed through
 * {@code WorldGenCustomStructures.generateInPosition(..., -2, -2, -2)}, so the template lands on the
 * impact surface itself.  1.12.2's {@code Template#addBlocksToWorld} took no {@code Random}, while
 * the modern {@code StructureTemplate#placeInWorld} only forwards its {@code RandomSource} to
 * structure processors; the settings used here carry none, so template placement still consumes no
 * random draws and the fires that follow see the same stream the original did.</p>
 */
public final class WorldGenParasiteMeteorCrash extends WorldGenParasiteColonyBase {
    private static final String[] FRAGMENTS = {
            "meteor_fragment_large1", "meteor_fragment_large2", "meteor_fragment_large3",
            "meteor_fragment_small1", "meteor_fragment_small2", "meteor_fragment_small3",
            "meteor_fragment_small4", "meteor_fragment_small5", "meteor_fragment_small6"
    };
    private static final int FIRE_COUNT_BASE = 18;
    private static final int FIRE_RADIUS = 10;
    private static final int LOOT_BG_RANGE = 11;

    public WorldGenParasiteMeteorCrash(int stage) {
        super(stage);
        this.wall = ParasiteGenContext.DENSE_WALL;
        this.tacle = ParasiteGenContext.STAIN_FEELER;
        this.floor = ParasiteGenContext.STAIN_DIRT;
    }

    @Override
    public boolean generate(ServerLevel level, RandomSource rand, BlockPos posss) {
        BlockPos impactCenter = MeteorImpactUtil.topSolidOrLiquid(level, posss).below();
        if (this.type != 5) {
            return generateFragment(level, rand, impactCenter);
        }
        return generateMainCrash(level, rand, impactCenter);
    }

    private boolean generateFragment(ServerLevel level, RandomSource rand, BlockPos impactCenter) {
        String out = FRAGMENTS[0];
        switch (rand.nextInt(9)) {
            case 1 -> out = FRAGMENTS[1];
            case 2 -> out = FRAGMENTS[2];
            case 3 -> out = FRAGMENTS[3];
            case 4 -> out = FRAGMENTS[4];
            case 5 -> out = FRAGMENTS[5];
            case 6 -> out = FRAGMENTS[6];
            case 7 -> out = FRAGMENTS[7];
            case 8 -> out = FRAGMENTS[8];
            default -> {
            }
        }

        StructurePlacer.place(level, Identifier.fromNamespaceAndPath(Csrp.MODID, out), impactCenter);

        int fires = FIRE_COUNT_BASE + rand.nextInt(FIRE_COUNT_BASE);
        for (int i = 0; i < fires; i++) {
            int dx = rand.nextInt(FIRE_RADIUS * 2 + 1) - FIRE_RADIUS;
            int dz = rand.nextInt(FIRE_RADIUS * 2 + 1) - FIRE_RADIUS;
            if (dx * dx + dz * dz > FIRE_RADIUS * FIRE_RADIUS) {
                continue;
            }
            BlockPos top = MeteorImpactUtil.topSolidOrLiquid(level, impactCenter.offset(dx, 0, dz)).below();
            if (top.getY() <= 5) {
                continue;
            }
            BlockPos firePos = top.above();
            if (!level.isLoaded(firePos)) {
                continue;
            }
            BlockState below = ParasiteGenContext.get(level, top);
            if (below.isAir() || below.getFluidState().is(Fluids.WATER)
                    || below.getFluidState().is(Fluids.LAVA)) {
                continue;
            }
            if (ParasiteGenContext.get(level, firePos).isAir()) {
                ParasiteGenContext.setBlock(level, firePos, Blocks.FIRE.defaultBlockState());
            }
        }
        return true;
    }

    private boolean generateMainCrash(ServerLevel level, RandomSource rand, BlockPos impactCenter) {
        BlockPos enter = impactCenter.below(10);
        for (int i = 0; i < 20; i++) {
            replaceCircleGround(level, enter.above(i), this.type * 7, ParasiteGenContext.STAIN_RED);
        }

        int rad = this.type;
        BlockPos posss = impactCenter.below(rad + rad);
        int minCenterY = rad * 16 + 6;
        if (posss.getY() < minCenterY) {
            posss = new BlockPos(posss.getX(), minCenterY, posss.getZ());
        }

        generateSphere(level, posss, rad * 16, rad * 16, rand, false, 1, false, 1, 1, 5,
                ParasiteGenContext.AIR, ParasiteGenContext.AIR, ParasiteGenContext.AIR, 2);

        float yaw = rand.nextFloat() * 360.0F;
        double dirX = -Mth.sin(yaw * (float) (Math.PI / 180.0D));
        double dirZ = Mth.cos(yaw * (float) (Math.PI / 180.0D));
        float steepness = 0.25F + rand.nextFloat() * 0.75F;
        double dirY = -steepness;

        BlockState rim = ParasiteGenContext.DENSE_WALL;
        BlockState rubble = ParasiteGenContext.RUBBLE_BRICKS;
        BlockState stainRed = ParasiteGenContext.STAIN_RED;
        BlockState cooked = ParasiteGenContext.COOKED_FLESH;
        BlockPos craterSurface = impactCenter;
        BlockPos tunnelStart = craterSurface.below(3);
        MeteorImpactUtil.TunnelResult tunnel = MeteorImpactUtil.carveAngledTunnel(
                level, tunnelStart, 10, 30, dirX, dirY, dirZ);

        int baseR = rad * 8;
        int baseDepth = (int) (baseR * (0.4F + rand.nextFloat() * 0.2F));
        int adjustedDepth = baseDepth;
        if (tunnel.anyBroken()) {
            int openNeeded = craterSurface.getY() - tunnel.lowestY();
            if (openNeeded > adjustedDepth) {
                adjustedDepth = openNeeded + 3;
            }
        }

        int adjustedR = baseR;
        int depthDrivenR = (int) (adjustedDepth * 1.6F);
        if (depthDrivenR > adjustedR) {
            adjustedR = depthDrivenR;
        }

        int bottomY = craterSurface.getY() - adjustedDepth;
        int placeY = Math.max(bottomY + 1, 6);
        BlockPos structPos = new BlockPos(craterSurface.getX(), placeY, craterSurface.getZ());

        MeteorImpactUtil.clearVegetationInArea(level, craterSurface, adjustedR * 2,
                craterSurface.getY() - adjustedDepth - 12, craterSurface.getY() + 50);
        MeteorImpactUtil.carveCraterBowl(level, rand, craterSurface, adjustedR, adjustedDepth, steepness,
                rim, stainRed, cooked);
        MeteorImpactUtil.scorchRings(level, rand, craterSurface, adjustedR, stainRed);
        MeteorImpactUtil.spawnEjecta(level, rand, craterSurface, adjustedR, dirX, dirZ, rubble, stainRed);
        MeteorImpactUtil.microCraters(level, rand, craterSurface, adjustedR, dirX, dirZ, stainRed);

        int poolR = Math.max(4, adjustedR / 6);
        int poolRR = poolR * poolR;
        int skipR = 10;
        int skipRR = skipR * skipR;
        int bottomY2 = craterSurface.getY() - adjustedDepth + 1;
        if (bottomY2 < 6) {
            bottomY2 = 6;
        }

        int poolHeight = 4;
        int topY2 = bottomY2 + poolHeight;

        for (int x = -poolR; x <= poolR; x++) {
            for (int z = -poolR; z <= poolR; z++) {
                int d2 = x * x + z * z;
                if (d2 > poolRR || d2 <= skipRR) {
                    continue;
                }
                for (int y = bottomY2; y <= topY2; y++) {
                    BlockPos p = new BlockPos(craterSurface.getX() + x, y, craterSurface.getZ() + z);
                    if (ParasiteGenContext.get(level, p).isAir()) {
                        ParasiteGenContext.setBlock(level, p, ParasiteGenContext.DEAD_BLOOD);
                    }
                }
            }
        }

        int half = 22;
        int fix = 2;
        BlockPos meteorPos = structPos.above(14).offset(-half - fix, 0, -half - fix);
        StructurePlacer.place(level, Identifier.fromNamespaceAndPath(Csrp.MODID, "meteor"), meteorPos);

        int i1 = meteorPos.above(14).getY();
        int l1 = meteorPos.above(14).offset(half, 0, 0).getX();
        int i2 = meteorPos.above(14).offset(0, 0, half).getZ();

        for (int k2 = -LOOT_BG_RANGE; k2 <= LOOT_BG_RANGE; k2++) {
            for (int l2 = -LOOT_BG_RANGE; l2 <= LOOT_BG_RANGE; l2++) {
                for (int j = -LOOT_BG_RANGE; j <= LOOT_BG_RANGE; j++) {
                    BlockPos blockpos = new BlockPos(l1 + k2, i1 + j, i2 + l2);
                    BlockState state = ParasiteGenContext.get(level, blockpos);
                    if (!level.isLoaded(blockpos)) {
                        continue;
                    }
                    if (isGlassLike(state)) {
                        ParasiteGenContext.setBlock(level, blockpos, ParasiteGenContext.STAIN_FLESH);
                        continue;
                    }
                    Block block = state.getBlock();
                    if (block != Blocks.IRON_BLOCK && block != Blocks.GOLD_BLOCK
                            && block != Blocks.DIAMOND_BLOCK) {
                        continue;
                    }
                    int rollRare = 10;
                    int rollUncommon = 4;
                    if (block == Blocks.GOLD_BLOCK) {
                        rollRare = 7;
                        rollUncommon = 3;
                    } else if (block == Blocks.DIAMOND_BLOCK) {
                        rollRare = 4;
                        rollUncommon = 2;
                    }
                    if (rand.nextInt(rollRare) == 0) {
                        ParasiteGenContext.placeLoot(level, blockpos, LOOT_RARE, rand);
                    } else if (rand.nextInt(rollUncommon) == 0) {
                        ParasiteGenContext.placeLoot(level, blockpos, LOOT_UNCOMMON, rand);
                    } else {
                        ParasiteGenContext.placeLoot(level, blockpos, LOOT_COMMON, rand);
                    }
                    ParasiteGenContext.setBlock(level, blockpos.above(), ParasiteGenContext.DEAD_BLOOD);
                }
            }
        }

        MeteorImpactUtil.updateWaterAfterImpact(level, craterSurface, adjustedR, adjustedDepth);
        return true;
    }

    /**
     * The original skip test: glass, stained glass, glass panes and stained glass panes — i.e. the
     * blocks the modern {@code minecraft:impermeable} tag collects (iron bars are a pane with an iron
     * material and were deliberately not skipped, so the tag is the exact modern equivalent).
     */
    private static boolean isGlassLike(BlockState state) {
        if (state.is(BlockTags.IMPERMEABLE)) {
            return true;
        }
        if (state.is(Blocks.GLASS_PANE)) {
            return true;
        }
        return state.getBlock() instanceof StainedGlassPaneBlock;
    }
}
