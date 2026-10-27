package alku.csrp.world.gen;

import alku.csrp.block.SrpCoreBlock;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of 1.10.9 {@code WorldGenParasiteNodeCore}.
 *
 * <p>The node core decorates the parasite biome heart.  {@code type == 3} uses the "harlequin" heart
 * (a no-op in the original) and every other type uses the "shrouded" heart, whose stages build the
 * dense-rubble pedestal, the feeler tendrils and the dead-blood veins around the heart.  The heart
 * itself is written last, carrying its 1.10.9 {@code ACTIVE} stage.</p>
 */
public final class WorldGenParasiteNodeCore {
    private static final int HARLEQUIN_TYPE = 3;
    private static final int MAX_STEPS = 512;

    private final int stage;
    private final int biomeType;

    public WorldGenParasiteNodeCore(int stage, int biomeType) {
        this.stage = stage;
        this.biomeType = biomeType;
    }

    public boolean generate(ServerLevel level, RandomSource random, BlockPos position) {
        if (biomeType == HARLEQUIN_TYPE) {
            placeHeartHarlequin();
        } else {
            placeHeartShrouded(level, random, position);
        }
        placeCore(level, position, stage);
        return true;
    }

    /** The 1.10.9 harlequin switch had no cases: only the core is written (by the caller). */
    private void placeHeartHarlequin() {
    }

    private void placeHeartShrouded(ServerLevel level, RandomSource random, BlockPos position) {
        switch (stage) {
            case 1 -> placeShroudedStageOne(level, position);
            case 2 -> placeShroudedStageTwo(level, random, position);
            default -> {
            }
        }
    }

    private void placeShroudedStageOne(ServerLevel level, BlockPos position) {
        BlockPos helper = position;
        int down = 5;

        // Pedestal: five layers, each a 3x3 dense-rubble plate with a one-block feeler collar.
        while (down >= 0 && helper.below().getY() >= 1) {
            helper = helper.below();
            placeTrunk(level, helper);
            down--;

            for (int xs = -1; xs <= 1; xs++) {
                for (int zs = -1; zs <= 1; zs++) {
                    BlockPos center = new BlockPos(helper.getX() + xs, helper.getY(), helper.getZ() + zs);
                    placeTrunk(level, center);

                    for (int z = 0; z <= 3; z++) {
                        placeTrunk(level, ParasiteGenContext.horizontal(center, z));
                        if (xs == 0 && zs == 0) {
                            BlockPos probe = center;
                            for (int kkk = 0; kkk <= 2; kkk++) {
                                probe = ParasiteGenContext.horizontal(probe, z);
                            }
                            placeDirt(level, probe);
                        }
                    }
                }
            }
        }

        // Root: three more layers, replacing everything but dense rubble with stain.
        down = 2;
        while (down >= 0 && helper.below().getY() >= 1) {
            helper = helper.below();
            placeTrunk(level, helper);
            down--;

            for (int xs = -1; xs <= 1; xs++) {
                for (int zs = -1; zs <= 1; zs++) {
                    BlockPos center = new BlockPos(helper.getX() + xs, helper.getY(), helper.getZ() + zs);
                    placeTrunk(level, center);
                    for (int z = 0; z <= 3; z++) {
                        BlockPos side = ParasiteGenContext.horizontal(center, z);
                        if (!isDenseRubble(level, side)) {
                            placeDirt(level, side);
                        }
                    }
                }
            }
        }

        placeDirt(level, position.below());

        // 四条主臂各延伸至第三段，侧枝只生长在前两段。
        for (int i = 0; i <= 3; i++) {
            BlockPos anchor = ParasiteGenContext.horizontal(position, i);
            placeGroundedTrunk(level, anchor);
            placeGroundedBranches(level, anchor, i);

            anchor = ParasiteGenContext.horizontal(anchor, i);
            placeGroundedTrunk(level, anchor);
            placeGroundedBranches(level, anchor, i);

            anchor = ParasiteGenContext.horizontal(anchor, i);
            placeGroundedTrunk(level, anchor);
        }
        position = position.above();
        placeTrunk(level, position);
        placePeri(level, position);
        position = position.above();
        placeTrunk(level, position);
    }

    private void placeShroudedStageTwo(ServerLevel level, RandomSource random, BlockPos position) {
        position = position.above(2);
        placeLiquid(level, position);

        for (int i = 0; i < 3; i++) {
            placePeri(level, position);
            placeLiquid(level, position);
            position = position.above();
        }

        for (int i = 0; i <= 3; i++) {
            BlockPos root = ParasiteGenContext.steps(position.below(), i, 2);
            tendrilRun(level, random, root, i, 2, false, true);
        }

        placeLiquid(level, position);

        for (int i = 0; i < 5; i++) {
            placePeri(level, position);
            placeLiquid(level, position);
            position = position.above();
        }

        for (int i = 0; i <= 3; i++) {
            BlockPos root = ParasiteGenContext.steps(position.below(), i, 2);
            tendrilRun(level, random, root, i, 1, true, true);
        }

        placeLiquid(level, position);

        for (int i = 0; i < 5; i++) {
            placePeri(level, position);
            placeLiquid(level, position);
            position = position.above();
        }

        placeTrunk(level, position);
    }

    /** 第一轮只给第二分支补 FEELER，第二轮两个分支都会补。 */
    private static void tendrilRun(ServerLevel level, RandomSource random, BlockPos root, int i, int step,
            boolean extraFeelerForFirstBranch, boolean extraFeelerForSecondBranch) {
        for (int o = 0; o < 2; o++) {
            if (random.nextInt(2) != 0) {
                continue;
            }
            int sideDirection = o == 0 ? (i + 1) % 4 : (i + 3) % 4;
            BlockPos tip = ParasiteGenContext.horizontal(root, sideDirection);
            placeTen(level, tip);
            tip = ParasiteGenContext.horizontal(tip, i);
            placeTen(level, tip);

            int guard = 0;
            while (!ParasiteGenContext.isFullCube(level, tip.below())
                    && tip.below().getY() >= 1 && guard++ < MAX_STEPS) {
                if (random.nextInt(1) == 0) {
                    tip = ParasiteGenContext.sideCurse(tip, i * 10 + (o == 1 ? 1 : 0));
                } else {
                    tip = ParasiteGenContext.horizontal(tip, i);
                }
                if (o == 0 ? extraFeelerForFirstBranch : extraFeelerForSecondBranch) {
                    placeTen(level, tip);
                }
                tip = placeColumn(level, tip, random.nextInt(2) + step);
            }

            placeDirt(level, tip.below());
        }
    }

    private static boolean isDenseRubble(ServerLevel level, BlockPos pos) {
        return ParasiteGenContext.get(level, pos).getBlock()
                == ModBlocks.PARASITERUBBLEDENSE_BIOME.get();
    }

    private static void placeTen(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.STAIN_FEELER);
    }

    private static void placeTrunk(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos,
                ModBlocks.PARASITERUBBLEDENSE_BIOME.get().defaultBlockState());
    }

    private static void placeCore(ServerLevel level, BlockPos pos, int stage) {
        BlockState state = ModBlocks.BIOMEHEART.get().defaultBlockState();
        int active = Math.max(0, Math.min(3, stage));
        ParasiteGenContext.setBlock(level, pos, state.setValue(SrpCoreBlock.ACTIVE, active));
    }

    private static void placeDirt(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.legacyBlock("parasitestain").get().defaultBlockState());
    }

    private static void placeLiquid(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ModBlocks.DEAD_BLOOD.get().defaultBlockState());
    }

    private static void placeGroundedTrunk(ServerLevel level, BlockPos pos) {
        if (!ParasiteGenContext.isFullCube(level, pos.below())) {
            placeTrunk(level, pos.below());
        }
        placeTrunk(level, pos);
    }

    private static void placeGroundedBranches(ServerLevel level, BlockPos anchor, int direction) {
        for (int o = 0; o < 2; o++) {
            BlockPos side = ParasiteGenContext.horizontal(anchor, (direction + (o == 0 ? 1 : 3)) % 4);
            placeGroundedTrunk(level, side);
        }
    }

    /** 原版 {@code placePeri}：四个方向各放置一格主干和两格侧枝。 */
    private static void placePeri(ServerLevel level, BlockPos position) {
        for (int i = 0; i <= 3; i++) {
            BlockPos helper = ParasiteGenContext.horizontal(position, i);
            placeGroundedTrunk(level, helper);
            placeGroundedBranches(level, helper, i);
        }
    }

    /** 原版 {@code placeColumn} 写入高度格，并额外写入返回位置这一格。 */
    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int height) {
        BlockPos current = pos;
        for (int i = 0; i < height; i++) {
            placeTen(level, current);
            current = current.below();
        }
        placeTen(level, current);
        return current;
    }
}
