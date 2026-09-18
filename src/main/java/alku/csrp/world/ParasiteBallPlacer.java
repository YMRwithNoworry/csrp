package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/**
 * 寄生球体：SRParasites 1.10.9 {@code WorldGenParasiteBall} 与
 * {@code WorldGenParasiteBigBall} 的移植。
 *
 * <p>原实现是 1.12.2 的 {@code IWorldGenerator} 特性，在 SRP 生物群系里按概率装饰生成；
 * 本工程没有自定义生物群系（星型改造是全局的），因此与 {@link ColdStarTreeHandler} /
 * {@link DeadheadTreePlacer} 保持同一范式：由 {@link StarBiomeGenerationEvents} 的
 * chunk 钩子驱动，用星型作为选点判据。
 *
 * <p>结构体本身（{@code ball.nbt} 7×10×7 / {@code ballbig.nbt} 13×21×13）已随捐赠分支同步到位，
 * 由 {@link MeteorStructureLoader} 放置（同一条命名空间改写路径）。
 *
 * <p>与 1.10.9 的差异（记为近似）：
 * <ul>
 *   <li>原版 {@code placeThin} 会在空气处放 {@code ParasiteThin}、否则放
 *       {@code ParasiteTrunk(variant=TREE)}。本工程的 {@code parasitethin} 默认态就是
 *       「无连接」的细枝，直接使用；不另做连接状态推演。</li>
 *   <li>原版根系用 {@code ParasiteTrunk(variant=TREE)}，本工程对应 {@code csrp:parasitetrunk}。</li>
 * </ul>
 */
public final class ParasiteBallPlacer {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 1.10.9 两个特性共用的净空高度。 */
    private static final int CLEARANCE_HEIGHT = 12;
    /** 1.10.9 净空检测半径（水平）。 */
    private static final int CLEARANCE_RADIUS = 4;

    /** 1.10.9 {@code WorldGenParasiteBall}: 结构锚点偏移 (3,0,3)，附加高度 2 + rand(4)。 */
    private static final String BALL_STRUCTURE = "ball";
    private static final BlockPos BALL_ANCHOR = new BlockPos(3, 0, 3);
    private static final int BALL_EXTRA_MIN = 0;
    private static final int BALL_EXTRA_BOUND = 4;
    private static final int BALL_BASE_LIFT = 2;
    private static final int BALL_ROOT_LIFT = 3;
    /** 1.10.9: 每个方向上有 0.5 概率被跳过。 */
    private static final double BALL_SKIP_CHANCE = 0.5;

    /** 1.10.9 {@code WorldGenParasiteBigBall}: 锚点偏移 (6,0,6)，附加高度 9 + 2 + rand(6)。 */
    private static final String BIG_BALL_STRUCTURE = "ballbig";
    private static final BlockPos BIG_BALL_ANCHOR = new BlockPos(6, 0, 6);
    private static final int BIG_BALL_EXTRA_BASE = 2;
    private static final int BIG_BALL_EXTRA_BOUND = 6;
    private static final int BIG_BALL_BASE_LIFT = 9;
    private static final int BIG_BALL_ROOT_LIFT = 12;
    /** 1.10.9: 4 个方向上第一个有 0.15 概率被跳过。 */
    private static final double BIG_BALL_SKIP_CHANCE = 0.15;
    /** 1.10.9 bigball 侧向分支概率。 */
    private static final double BIG_BALL_SIDE_CHANCE = 0.2;

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    private ParasiteBallPlacer() {
    }

    /** 1.10.9 {@code WorldGenParasiteBall.generate}。 */
    public static boolean placeBall(ServerLevel level, BlockPos position, RandomSource random) {
        if (!hasClearance(level, position)) {
            return false;
        }
        int extra = BALL_EXTRA_MIN + random.nextInt(BALL_EXTRA_BOUND);
        BlockPos origin = position.offset(BALL_ANCHOR.getX(), BALL_BASE_LIFT + extra, BALL_ANCHOR.getZ());
        if (!MeteorStructureLoader.place(level, BALL_STRUCTURE, origin, random)) {
            return false;
        }

        boolean skip = true;
        // 1.10.9 从 1 数到 4（每根细枝半径 3 起步）
        for (Direction direction : HORIZONTAL) {
            if (random.nextDouble() <= BALL_SKIP_CHANCE && skip) {
                skip = false;
                continue;
            }
            BlockPos root = position.above(BALL_ROOT_LIFT + extra);
            root = root.relative(direction, 3);
            placeThin(level, root);
            root = root.below();
            placeThin(level, root);
            root = root.relative(direction, 1);
            placeThin(level, root);

            // 一直向下垂到不可替换为止
            int guard = 0;
            while (guard++ < 256 && level.getBlockState(root.below()).canBeReplaced()) {
                root = root.below();
                placeThin(level, root);
            }
            // 底端向四个方向各补一格
            for (Direction side : HORIZONTAL) {
                BlockPos arm = root.relative(side, 1);
                if (!level.getBlockState(arm).canBeReplaced()) {
                    continue;
                }
                placeThin(level, arm);
            }
        }
        return true;
    }

    /** 1.10.9 {@code WorldGenParasiteBigBall.generate}。 */
    public static boolean placeBigBall(ServerLevel level, BlockPos position, RandomSource random) {
        if (!hasClearance(level, position)) {
            return false;
        }
        int extra = BIG_BALL_EXTRA_BASE + random.nextInt(BIG_BALL_EXTRA_BOUND);
        BlockPos origin = position.offset(BIG_BALL_ANCHOR.getX(), BIG_BALL_BASE_LIFT + extra,
                BIG_BALL_ANCHOR.getZ());
        if (!MeteorStructureLoader.place(level, BIG_BALL_STRUCTURE, origin, random)) {
            return false;
        }

        boolean skip = true;
        for (Direction direction : HORIZONTAL) {
            if (random.nextDouble() <= BIG_BALL_SKIP_CHANCE && skip) {
                skip = false;
                continue;
            }
            Direction heading = direction;
            boolean side = false;

            BlockPos root = position.above(BIG_BALL_ROOT_LIFT + extra).relative(heading, 4);
            root = placeColumn(level, root, 3);

            int guard = 0;
            // 1.10.9: while (glag) { ... glag = world.getBlockState(root).isReplaceable(); }
            while (guard++ < 64 && level.getBlockState(root).canBeReplaced()) {
                int grow = 2 + random.nextInt(4);
                root = root.below().relative(heading, side ? -1 : 1);
                if (random.nextDouble() <= BIG_BALL_SIDE_CHANCE) {
                    BlockPos side1 = root.relative(heading, side ? -1 : 1);
                    BlockPos side2 = side1.relative(heading, side ? -1 : 1);
                    placeColumn(level, side2, grow);
                }
                BlockPos bottom = placeColumn(level, root, grow);
                BlockPos next = bottom.below().relative(heading, side ? 1 : -1);
                root = placeColumn(level, next, 2 + random.nextInt(2));
                side = !side;
                heading = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
            }
        }
        return true;
    }

    /**
     * 1.10.9 {@code WorldGenParasiteGenAbstract.isReplaceable} 用于净空检测的等价判据：
     * 空气、树叶、原木、以及该抽象类 {@code canGrowInto} 认可的草/泥土/沙/砾石等。
     */
    private static boolean hasClearance(ServerLevel level, BlockPos position) {
        if (level.getBlockState(position.below()).isAir()) {
            return false;
        }
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int top = position.getY() + 1 + CLEARANCE_HEIGHT;
        if (position.getY() < 1 || top > maxY) {
            return false;
        }
        for (int y = position.getY(); y <= top; y++) {
            if (y < minY) {
                return false;
            }
            for (int dx = -CLEARANCE_RADIUS; dx <= CLEARANCE_RADIUS; dx++) {
                for (int dz = -CLEARANCE_RADIUS; dz <= CLEARANCE_RADIUS; dz++) {
                    if (!isReplaceable(level, position.offset(dx, y - position.getY(), dz))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isReplaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
            return true;
        }
        if (state.canBeReplaced()) {
            return true;
        }
        Block block = state.getBlock();
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT
                || block == Blocks.PODZOL || block == Blocks.SAND || block == Blocks.RED_SAND
                || block == Blocks.GRAVEL || block == Blocks.MYCELIUM
                // 1.10.9 认 SRPBlocks.ParasiteBush；本工程把 infestedbush 更名为 residue_plants
                // （见 MeteorStructureLoader.BLOCK_RENAMES）
                || block == ModBlocks.RESIDUE_PLANTS.get();
    }

    /** 1.10.9 {@code placeThin}：空气处放细枝，否则放寄生树干。 */
    private static void placeThin(ServerLevel level, BlockPos pos) {
        BlockState target = level.getBlockState(pos).isAir()
                ? ModBlocks.PARASITETHIN.get().defaultBlockState()
                : ModBlocks.PARASITETRUNK.get().defaultBlockState();
        level.setBlock(pos, target, Block.UPDATE_CLIENTS);
    }

    /**
     * 1.10.9 {@code placeColumn}：从 pos 向下连续放 {@code length} 格寄生树干，返回最底一格。
     *
     * <p>原实现先把 {@code in} 减一，然后从 pos 起向下写 {@code in} 格，最后在落点再补一格，
     * 合计正好 {@code length} 格，返回值为最底那一格。
     */
    private static BlockPos placeColumn(ServerLevel level, BlockPos pos, int length) {
        BlockPos current = pos;
        for (int i = 0; i < length; i++) {
            level.setBlock(current, ModBlocks.PARASITETRUNK.get().defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            if (i < length - 1) {
                current = current.below();
            }
        }
        return current;
    }

    /**
     * 由 {@link StarBiomeGenerationEvents} 调用：在一个区块上按概率尝试生成球体。
     *
     * <p>1.10.9 是在 SRP 生物群系里按每区块概率装饰；本工程以星型作为选点判据，
     * 概率沿用原版数量级（小球常见、大球稀有）。
     */
    public static void decorate(ServerLevel level, int chunkX, int chunkZ) {
        RandomSource random = level.getRandom();
        int baseX = (chunkX << 4) + 8;
        int baseZ = (chunkZ << 4) + 8;

        if (random.nextInt(24) == 0) {
            BlockPos surface = surfaceAt(level, baseX + random.nextInt(9) - 4, baseZ + random.nextInt(9) - 4);
            if (surface != null) {
                placeBall(level, surface, random);
            }
        }
        if (random.nextInt(96) == 0) {
            BlockPos surface = surfaceAt(level, baseX + random.nextInt(9) - 4, baseZ + random.nextInt(9) - 4);
            if (surface != null) {
                placeBigBall(level, surface, random);
            }
        }
    }

    private static BlockPos surfaceAt(ServerLevel level, int x, int z) {
        BlockPos probe = new BlockPos(x, 0, z);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
        if (surface.getY() <= level.getMinBuildHeight() + 1) {
            return null;
        }
        return surface;
    }
}
