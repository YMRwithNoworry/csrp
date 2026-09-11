package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.block.ParasiteLootBlock;
import alku.csrp.block.entity.ParasiteLootBlockEntity;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
 * Port of SRParasites 1.10.8 {@code WorldGenParasiteMeteorCrash}. Fragment meteors (type != 5)
 * scatter a small structure and fires; the root meteor (type == 5) carves the huge crater and
 * places the hive satellite structure.
 */
public final class MeteorCrashFeature {
    private static final String[] FRAGMENT_LARGE = {
            "meteor_fragment_large1", "meteor_fragment_large2", "meteor_fragment_large3"
    };
    private static final String[] FRAGMENT_SMALL = {
            "meteor_fragment_small1", "meteor_fragment_small2", "meteor_fragment_small3",
            "meteor_fragment_small4", "meteor_fragment_small5", "meteor_fragment_small6"
    };

    private MeteorCrashFeature() {
    }

    public static void generate(ServerLevel level, RandomSource random, BlockPos pos, int type) {
        BlockPos impactCenter = MeteorImpactUtil.topSolidOrLiquid(level, pos).below();
        if (type != 5) {
            generateFragment(level, random, impactCenter);
        } else {
            generateMainCrash(level, random, impactCenter, type);
        }
    }

    private static void generateFragment(ServerLevel level, RandomSource random, BlockPos impactCenter) {
        String out = "meteor_fragment_large1";
        switch (random.nextInt(9)) {
            case 1 -> out = FRAGMENT_LARGE[1];
            case 2 -> out = FRAGMENT_LARGE[2];
            case 3 -> out = FRAGMENT_SMALL[0];
            case 4 -> out = FRAGMENT_SMALL[1];
            case 5 -> out = FRAGMENT_SMALL[2];
            case 6 -> out = FRAGMENT_SMALL[3];
            case 7 -> out = FRAGMENT_SMALL[4];
            case 8 -> out = FRAGMENT_SMALL[5];
            default -> {
            }
        }
        StructurePlacer.place(level, ResourceLocation.fromNamespaceAndPath(Csrp.MODID, out), impactCenter);

        int fires = 18 + random.nextInt(18);
        int fireRadius = 10;
        for (int i = 0; i < fires; i++) {
            int dx = random.nextInt(fireRadius * 2 + 1) - fireRadius;
            int dz = random.nextInt(fireRadius * 2 + 1) - fireRadius;
            if (dx * dx + dz * dz > fireRadius * fireRadius) {
                continue;
            }
            BlockPos top = MeteorImpactUtil.topSolidOrLiquid(level,
                    impactCenter.offset(dx, 0, dz)).below();
            if (top.getY() <= 5) {
                continue;
            }
            BlockPos firePos = top.above();
            if (!level.isLoaded(firePos)) {
                continue;
            }
            BlockState below = level.getBlockState(top);
            if (below.isAir() || below.getFluidState().is(Fluids.WATER)
                    || below.getFluidState().is(Fluids.LAVA)) {
                continue;
            }
            if (level.getBlockState(firePos).isAir()) {
                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 2);
            }
        }
    }

    private static void generateMainCrash(ServerLevel level, RandomSource random, BlockPos impactCenter, int type) {
        BlockPos enter = impactCenter.below(10);
        BlockState stainRed = ModBlocks.PARASITESTAIN_RED.get().defaultBlockState();
        for (int i = 0; i < 20; i++) {
            replaceCircleGround(level, enter.above(i), type * 7, stainRed);
        }

        int rad = type;
        BlockPos center = impactCenter.below(rad + rad);
        int minCenterY = rad * 16 + 6;
        if (center.getY() < minCenterY) {
            center = new BlockPos(center.getX(), minCenterY, center.getZ());
        }
        carveAirSphere(level, random, center, rad * 16, rad * 16, 1, 1, 1, 5, 2);

        float yaw = random.nextFloat() * 360.0F;
        double dirX = -Mth.sin(yaw * (float) (Math.PI / 180.0D));
        double dirZ = Mth.cos(yaw * (float) (Math.PI / 180.0D));
        float steepness = 0.25F + random.nextFloat() * 0.75F;
        double dirY = -steepness;

        BlockState rim = ModBlocks.PARASITERUBBLEDENSE.get().defaultBlockState();
        BlockState rubble = ModBlocks.PARASITERUBBLE_BRICKS.get().defaultBlockState();
        BlockState cooked = ModBlocks.COOKED_FLESH.get().defaultBlockState();
        BlockPos craterSurface = impactCenter;
        BlockPos tunnelStart = craterSurface.below(3);
        MeteorImpactUtil.TunnelResult tunnel = MeteorImpactUtil.carveAngledTunnel(
                level, tunnelStart, 10, 30, dirX, dirY, dirZ);

        int baseR = rad * 8;
        int baseDepth = (int) (baseR * (0.4F + random.nextFloat() * 0.2F));
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
        MeteorImpactUtil.carveCraterBowl(level, random, craterSurface, adjustedR, adjustedDepth,
                steepness, rim, stainRed, cooked);
        MeteorImpactUtil.scorchRings(level, random, craterSurface, adjustedR, stainRed);
        MeteorImpactUtil.spawnEjecta(level, random, craterSurface, adjustedR, dirX, dirZ, rubble, stainRed);
        MeteorImpactUtil.microCraters(level, random, craterSurface, adjustedR, dirX, dirZ, stainRed);

        int poolR = Math.max(4, adjustedR / 6);
        int poolRR = poolR * poolR;
        int skipR = 10;
        int skipRR = skipR * skipR;
        int bottomY2 = Math.max(6, craterSurface.getY() - adjustedDepth + 1);
        int topY2 = bottomY2 + 4;
        BlockState deadBlood = ModBlocks.DEAD_BLOOD.get().defaultBlockState();
        for (int x = -poolR; x <= poolR; x++) {
            for (int z = -poolR; z <= poolR; z++) {
                int d2 = x * x + z * z;
                if (d2 > poolRR || d2 <= skipRR) {
                    continue;
                }
                for (int y = bottomY2; y <= topY2; y++) {
                    BlockPos p = new BlockPos(craterSurface.getX() + x, y, craterSurface.getZ() + z);
                    if (level.isLoaded(p) && level.getBlockState(p).isAir()) {
                        level.setBlock(p, deadBlood, 2);
                    }
                }
            }
        }

        int half = 22;
        int fix = 2;
        BlockPos meteorPos = structPos.above(14).offset(-half - fix, 0, -half - fix);
        StructurePlacer.place(level, ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "meteor"), meteorPos);

        int i1 = meteorPos.above(14).getY();
        int l1 = meteorPos.above(14).offset(half, 0, 0).getX();
        int i2 = meteorPos.above(14).offset(0, 0, half).getZ();
        int bgRange = 11;
        for (int k2 = -bgRange; k2 <= bgRange; k2++) {
            for (int l2 = -bgRange; l2 <= bgRange; l2++) {
                for (int j = -bgRange; j <= bgRange; j++) {
                    BlockPos blockpos = new BlockPos(l1 + k2, i1 + j, i2 + l2);
                    if (!level.isLoaded(blockpos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(blockpos);
                    Block block = state.getBlock();
                    if (!isGlassLike(state)) {
                        if (block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK
                                || block == Blocks.DIAMOND_BLOCK) {
                            int rollRare = 10;
                            int rollUncommon = 4;
                            if (block == Blocks.GOLD_BLOCK) {
                                rollRare = 7;
                                rollUncommon = 3;
                            } else if (block == Blocks.DIAMOND_BLOCK) {
                                rollRare = 4;
                                rollUncommon = 2;
                            }
                            if (random.nextInt(rollRare) == 0) {
                                placeLoot(level, blockpos, ParasiteLootBlock.Tier.RARE, random);
                            } else if (random.nextInt(rollUncommon) == 0) {
                                placeLoot(level, blockpos, ParasiteLootBlock.Tier.UNCOMMON, random);
                            } else {
                                placeLoot(level, blockpos, ParasiteLootBlock.Tier.COMMON, random);
                            }
                            level.setBlock(blockpos.above(), deadBlood, 3);
                        }
                    } else {
                        level.setBlock(blockpos, ModBlocks.PARASITESTAIN_FLESH.get().defaultBlockState(), 2);
                    }
                }
            }
        }

        MeteorImpactUtil.updateWaterAfterImpact(level, craterSurface, adjustedR, adjustedDepth);
        MeteorImpactUtil.markMainMeteor(level, craterSurface);
    }

    /** Original {@code replaceCircleGround}: replaces a horizontal disc, skipping parasite blocks and air. */
    private static void replaceCircleGround(ServerLevel level, BlockPos center, int radius, BlockState target) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        if (cy <= 2 || cy >= 240) {
            return;
        }
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                BlockPos pos = new BlockPos(cx + dx, cy, cz + dz);
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState current = level.getBlockState(pos);
                if (!current.isAir() && !isParasiteBlock(current)) {
                    level.setBlockAndUpdate(pos, target);
                }
            }
        }
    }

    /**
     * Port of the original {@code generateSphere} call used by the root meteor. With all three
     * states set to air the generic sphere routine degenerates into a large skipped air carve.
     */
    private static void carveAirSphere(ServerLevel level, RandomSource random, BlockPos center,
            int innerHeight, int outerHeight, int valueStarting, int heightBelow, int heightAbove,
            int tip, int incomplete) {
        int xx = valueStarting;
        int zz = valueStarting;
        int ticc = 2;
        BlockPos pos = center;

        int test = innerHeight;
        while (test > 0) {
            test--;
            int heig = heightBelow;
            while (heig > 0) {
                heig--;
                carveCircleAir(level, random, pos, xx, zz, 1, incomplete);
                carveCircleAir(level, random, pos, xx - ticc, zz - ticc, 1, 50000);
                pos = pos.above();
            }
            xx++;
            zz++;
        }

        for (int i = outerHeight; i > 0; i--) {
            carveCircleAir(level, random, pos, xx, zz, 1, incomplete);
            carveCircleAir(level, random, pos, xx - 2, zz - 2, 1, 50000);
            pos = pos.above();
        }

        test = valueStarting + tip;
        while (test > 0) {
            test--;
            for (int heig = heightAbove; heig > 0; heig--) {
                carveCircleAir(level, random, pos, xx, zz, 1, incomplete);
                carveCircleAir(level, random, pos, xx - ticc, zz - ticc, 1, 50000);
                pos = pos.above();
            }
            xx--;
            zz--;
        }
    }

    private static void carveCircleAir(ServerLevel level, RandomSource random, BlockPos pos,
            int radiusX, int radiusZ, int height, int incomplete) {
        if (pos.getY() <= 2 || pos.getY() >= 240 || radiusX <= 0 || radiusZ <= 0) {
            return;
        }
        for (int y = 0; y < height; y++) {
            BlockPos layerPos = pos.above(y);
            for (int x = -radiusX; x <= radiusX; x++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double normalizedX = (double) x / radiusX;
                    double normalizedZ = (double) z / radiusZ;
                    if (normalizedX * normalizedX + normalizedZ * normalizedZ > 1.0D) {
                        continue;
                    }
                    if (random.nextInt(incomplete) == 0) {
                        continue;
                    }
                    BlockPos target = layerPos.offset(x, 0, z);
                    if (level.isLoaded(target)) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void placeLoot(ServerLevel level, BlockPos pos, ParasiteLootBlock.Tier tier,
            RandomSource random) {
        Block block = switch (tier) {
            case COMMON -> ModBlocks.PARASITE_LOOT_COMMON.get();
            case UNCOMMON -> ModBlocks.PARASITE_LOOT_UNCOMMON.get();
            case RARE -> ModBlocks.PARASITE_LOOT_RARE.get();
        };
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof ParasiteLootBlockEntity loot) {
            loot.generateLoot(tier, random);
        }
    }

    private static boolean isGlassLike(BlockState state) {
        if (state.is(BlockTags.IMPERMEABLE)) {
            return true;
        }
        if (state.is(Blocks.GLASS_PANE)) {
            return true;
        }
        return state.getBlock() instanceof StainedGlassPaneBlock;
    }

    private static boolean isParasiteBlock(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals(Csrp.MODID);
    }
}
