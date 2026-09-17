package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 死头树干（1.10.9 {@code BlockParasiteTrunk} 的 {@code variant=DEADHEAD} 形态）。
 *
 * <p>1.10.9 用一个自定义属性 {@code variant} 区分普通/死头树干，枯骸树放置时显式写入
 * {@code variant=deadhead}，掉皮逻辑也靠它判断。本工程没有给 {@code csrp:parasitetrunk}
 * 加 {@code variant} 属性——那会把该方块的状态空间从 3（axis）扩到 15（axis×variant），
 * 连带 {@code parasitetrunk_ball} / {@code _plant} / {@code _treestairs} 等既有
 * blockstate 全部要补组合，否则出现 missing model。改用**独立方块**承载同语义：
 * <ul>
 *   <li>枯骸树结构 NBT 里的树干方块 id 已转换为 {@code csrp:parasitetrunk_deadhead}；</li>
 *   <li>玩家/世界生成放置时，若相邻已有死头方块（树干或枯骸树叶），自动改用本方块，
 *       于是整棵树保持死头外观（对应 1.10.9 的变体传播）；</li>
 *   <li>稀有纹理由 blockstate 的 19:1 权重表达，不占存档状态空间。</li>
 * </ul>
 */
public class DeadheadTrunkBlock extends RotatedPillarBlock {

    public DeadheadTrunkBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        // 相邻（含上方树冠方向）已有死头方块时，本柱也取死头形态
        if (touchesDeadhead(context.getLevel(), context.getClickedPos())) {
            return state;
        }
        // 否则退回普通寄生树干，避免玩家随手放一根就得到死头纹理
        return ModBlocks.PARASITETRUNK.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
    }

    /**
     * 判断该位置是否紧邻死头方块（死头树干或枯骸树叶）。
     *
     * <p>用六向邻接而非 1.10.9 的「坐标哈希 + 树冠扫描」：后者依赖 1.12.2 世界生成阶段才
     * 有的树冠信息，在 1.20.1 的放置时机拿不到。行为等价性标注为近似。
     */
    public static boolean touchesDeadhead(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(ModBlocks.DEADHEAD_LEAVES.get())
                    || neighbor.is(ModBlocks.PARASITETRUNK_DEADHEAD.get())
                    || neighbor.is(ModBlocks.DEADHEAD_GRASS_SHORT.get())
                    || neighbor.is(ModBlocks.DEADHEAD_GRASS_TALL.get())) {
                return true;
            }
        }
        return false;
    }
}
