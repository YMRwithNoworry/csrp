package alku.csrp.item;

import alku.csrp.block.InfestedBushBlock;
import alku.csrp.block.ParasiteBushBlock;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 移植自 {@code item/ItemInfestedBonemeal}（out109，533 行）。
 *
 * <p>原件会感染点击方块周围半径 3 的球体（距离平方不超过 9），将石、土、沙、木材、树叶、楼梯、墙、栅栏、玻璃、羊毛、地毯、黏土及矿石映射为寄生方块；花、藤蔓与作物会被清除，并在新感染的方块上布置寄生植物（harleskinn 上按朝向布置 lipoma 或发丝）。
 * {@code onItemUse} 还会优先催长 Thornshade 和寄生树苗。</p>
 */
public final class InfestedBonemealItem extends BoneMealItem {
    /** Original {@code RADIUS = 3} / {@code r2 = 9}. */
    private static final int SPHERE_RADIUS = 3;
    private static final int SPHERE_RADIUS_SQ = 9;
    /** Original {@code SAFE_SET_FLAGS = 18}. */
    private static final int SAFE_SET_FLAGS = 18;

    public InfestedBonemealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        // Original handled BlockThornshade and BlockParasiteSapling through the vanilla bonemeal
        // path first; both implement BonemealableBlock in this port.
        if (clicked.getBlock() instanceof alku.csrp.block.ThornshadeBlock
                || clicked.getBlock() instanceof alku.csrp.block.ParasiteSaplingBlock) {
            return super.useOn(context);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean any = boneMealEffect((ServerLevel) level, pos, context.getClickedFace());
        if (any && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return any ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /** Original {@code boneMealEffect}: the 7-radius sphere conversion. */
    public static boolean boneMealEffect(ServerLevel level, BlockPos origin, Direction facing) {
        boolean any = false;
        RandomSource random = level.getRandom();
        for (int dx = -SPHERE_RADIUS; dx <= SPHERE_RADIUS; dx++) {
            for (int dy = -SPHERE_RADIUS; dy <= SPHERE_RADIUS; dy++) {
                for (int dz = -SPHERE_RADIUS; dz <= SPHERE_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > SPHERE_RADIUS_SQ) {
                        continue;
                    }
                    BlockPos target = origin.offset(dx, dy, dz);
                    if (level.getBlockEntity(target) != null || isLiquid(level, target)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(target);
                    if (state.isAir() || level.getBlockTicks().hasScheduledTick(target, state.getBlock())) {
                        continue;
                    }

                    BlockState converted = convert(level, target, state);
                    if (converted != null && level.setBlock(target, converted, SAFE_SET_FLAGS)) {
                        smoke(level, target);
                        any = true;
                    }

                    // 原件转换后会重新读取目标方块，再按注册路径布置装饰。
                    state = level.getBlockState(target);
                    Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if ("csrp".equals(key.getNamespace())) {
                        String path = key.getPath();
                        if (path.startsWith("infested") && !path.equals("infestedsand")) {
                            BlockState grow = pickInfestedPlant(random);
                            if (grow != null && canPlaceClean(level, target.above(), grow)
                                    && level.setBlock(target.above(), grow, SAFE_SET_FLAGS)) {
                                smoke(level, target.above());
                                any = true;
                            }
                        }
                        if (path.startsWith("harleskinn") || path.startsWith("poland_skin_block")) {
                            if (facing == Direction.DOWN) {
                                BlockState lipoma = ModBlocks.legacyBlock("lipoma_mass").get().defaultBlockState();
                                if (canPlaceClean(level, target.below(), lipoma)
                                        && level.setBlock(target.below(), lipoma, SAFE_SET_FLAGS)) {
                                    smoke(level, target.below());
                                    any = true;
                                }
                            } else {
                                Block hair = random.nextBoolean()
                                        ? ModBlocks.legacyBlock("tresses_hair").get()
                                        : ModBlocks.legacyBlock("hirsute_hair").get();
                                BlockState hairState = hair.defaultBlockState();
                                if (canPlaceClean(level, target.above(), hairState)
                                        && level.setBlock(target.above(), hairState, SAFE_SET_FLAGS)) {
                                    smoke(level, target.above());
                                    any = true;
                                }
                            }
                        }
                    }

                    // 保留原件的末端回退：整个球体尚未产生任何效果时，处理 harleskinn 下方格。
                    if (!any) {
                        BlockState above = level.getBlockState(target.above());
                        Identifier aboveKey = BuiltInRegistries.BLOCK.getKey(above.getBlock());
                        String aboveNamespace = aboveKey == null ? "" : aboveKey.getNamespace();
                        String abovePath = aboveKey == null ? "" : aboveKey.getPath();
                        if ("csrp".equals(aboveNamespace)
                                && (abovePath.startsWith("harleskinn") || abovePath.startsWith("poland_skin_block"))) {
                            BlockState lipoma = ModBlocks.legacyBlock("lipoma_mass").get().defaultBlockState();
                            if (canPlaceClean(level, target, lipoma)
                                    && level.setBlock(target, lipoma, SAFE_SET_FLAGS)) {
                                smoke(level, target);
                                any = true;
                            }
                        }
                    }
                }
            }
        }
        return any;
    }

    /** 按原件的注册路径与方块类型顺序转换单个方块。 */
    private static BlockState convert(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();

        // 原件先处理已点亮红石矿，再执行 oreMetaFor 的矿石映射。
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            level.setBlock(pos, Blocks.REDSTONE_ORE.defaultBlockState(), SAFE_SET_FLAGS);
            return ModBlocks.INFESTED_REDSTONE_ORE.get().defaultBlockState();
        }
        BlockState ore = infestedOre(block, path);
        if (ore != null) {
            return ore;
        }
        if (state.is(BlockTags.FLOWERS) || path.contains("flower")
                || state.is(Blocks.VINE) || path.contains("vine")
                || state.is(BlockTags.CROPS) || state.is(Blocks.SUGAR_CANE)
                || path.contains("wart")) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.is(Blocks.DIRT_PATH) || path.contains("grass_path") || path.endsWith("_path")) {
            return ModBlocks.INFESTED_SAND.get().defaultBlockState();
        }
        if (block == Blocks.COBBLESTONE || path.contains("cobblestone")) {
            return ModBlocks.INFESTED_COBBLESTONE.get().defaultBlockState();
        }
        if (block == Blocks.FARMLAND || path.contains("farmland")) {
            return ModBlocks.INFESTED_SAND.get().defaultBlockState();
        }
        if (state.is(BlockTags.WOOL) || path.contains("wool")) {
            BlockState rubble = ModBlocks.legacyBlock("parasiterubble").get().defaultBlockState();
            return rubble.hasProperty(alku.csrp.block.ParasiteRubbleBlock.VARIANT)
                    ? rubble.setValue(alku.csrp.block.ParasiteRubbleBlock.VARIANT,
                            alku.csrp.block.ParasiteRubbleBlock.Variant.FUNGUS)
                    : rubble;
        }

        // 1.10.9 的 sandstone 路径分支先于 stairs 分支；因此 sandstone stairs 实际落到 inf_ss。
        if (block == Blocks.SANDSTONE || path.contains("sandstone")) {
            if (path.contains("chiseled")) {
                return ModBlocks.CHISELED_INFESTED_SANDSTONE.get().defaultBlockState();
            }
            return ModBlocks.INFESTED_SANDSTONE.get().defaultBlockState();
        }
        if (block instanceof StairBlock) {
            BlockState stairs;
            if (state.is(BlockTags.WOODEN_STAIRS)) {
                stairs = ModBlocks.INFESTED_PLANKS_STAIRS.get().defaultBlockState();
            } else if (state.getSoundType() == SoundType.STONE) {
                stairs = ModBlocks.INFESTED_STONE_STAIRS.get().defaultBlockState();
            } else {
                return null;
            }
            return copyStairs(stairs, state);
        }
        if (block == Blocks.STONE || path.equals("stone") || block == Blocks.DEEPSLATE) {
            return ModBlocks.INFESTED_RUBBLE.get().defaultBlockState();
        }
        if (block == Blocks.DIRT || path.contains("dirt") || block == Blocks.GRASS_BLOCK) {
            return ModBlocks.INFESTED_STAIN.get().defaultBlockState();
        }
        if (block == Blocks.SAND || block == Blocks.RED_SAND || path.equals("sand")) {
            return ModBlocks.INFESTED_SAND.get().defaultBlockState();
        }
        if (state.is(BlockTags.LOGS) || path.contains("log")) {
            return ModBlocks.INFESTED_TRUNK.get().defaultBlockState();
        }
        if (state.is(BlockTags.PLANKS) || path.contains("plank")) {
            return ModBlocks.INFESTED_PLANKS.get().defaultBlockState();
        }
        if (state.is(BlockTags.LEAVES) || path.contains("leaves") || path.contains("leaf")) {
            return ModBlocks.legacyBlock("infested_leaves").get().defaultBlockState();
        }
        if (path.contains("stonebrick") || path.contains("stone_brick")) {
            return ModBlocks.INFESTED_STONE_BRICKS.get().defaultBlockState();
        }
        if (block instanceof WallBlock || path.endsWith("_wall") || path.contains("wall")) {
            return ModBlocks.INFESTED_RUBBLE_WALL.get().defaultBlockState();
        }
        if (block instanceof FenceBlock || path.endsWith("_fence") || path.contains("fence")) {
            return ModBlocks.INFESTED_FENCE.get().defaultBlockState();
        }
        if (block instanceof IronBarsBlock || path.contains("pane")) {
            return ModBlocks.INFESTED_GLASS_PANE.get().defaultBlockState();
        }
        if (path.contains("glass") && !path.contains("pane")) {
            return ModBlocks.INFESTED_GLASS.get().defaultBlockState();
        }
        if (block instanceof WoolCarpetBlock || path.contains("carpet")) {
            return ModBlocks.INFESTED_REMAINS.get().defaultBlockState();
        }
        if (block == Blocks.CLAY || path.contains("clay")) {
            return ModBlocks.INFESTED_SAND.get().defaultBlockState();
        }
        return null;
    }

    private static BlockState infestedOre(Block block, String path) {
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return ModBlocks.INFESTED_COAL_ORE.get().defaultBlockState();
        }
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return ModBlocks.INFESTED_DIAMOND_ORE.get().defaultBlockState();
        }
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return ModBlocks.INFESTED_EMERALD_ORE.get().defaultBlockState();
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return ModBlocks.INFESTED_GOLD_ORE.get().defaultBlockState();
        }
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return ModBlocks.INFESTED_IRON_ORE.get().defaultBlockState();
        }
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return ModBlocks.INFESTED_LAPIS_ORE.get().defaultBlockState();
        }
        return path.contains("ore") ? ModBlocks.INFESTED_ORE.get().defaultBlockState() : null;
    }

    /** 原件 {@code copyStairProps}：复制 facing、half、shape。 */
    private static BlockState copyStairs(BlockState target, BlockState from) {
        if (from.hasProperty(StairBlock.FACING) && target.hasProperty(StairBlock.FACING)) {
            target = target.setValue(StairBlock.FACING, from.getValue(StairBlock.FACING));
        }
        if (from.hasProperty(StairBlock.HALF) && target.hasProperty(StairBlock.HALF)) {
            target = target.setValue(StairBlock.HALF, from.getValue(StairBlock.HALF));
        }
        if (from.hasProperty(StairBlock.SHAPE) && target.hasProperty(StairBlock.SHAPE)) {
            target = target.setValue(StairBlock.SHAPE, from.getValue(StairBlock.SHAPE));
        }
        return target;
    }

    /** Original {@code pickInfestedPlant}: infestedbush 0/1/2/5, otherwise parasitebush variant 4. */
    private static BlockState pickInfestedPlant(RandomSource random) {
        Block bush = ModBlocks.legacyBlock("infestedbush").get();
        Block parasiteBush = ModBlocks.legacyBlock("parasitebush").get();
        return switch (random.nextInt(5)) {
            case 0 -> variant(bush, InfestedBushBlock.Variant.INFECTED);
            case 1 -> variant(bush, InfestedBushBlock.Variant.GRASS1);
            case 2 -> variant(bush, InfestedBushBlock.Variant.FLOWER1);
            case 3 -> variant(bush, InfestedBushBlock.Variant.ARC);
            default -> variant(parasiteBush, ParasiteBushBlock.Variant.TOOH);
        };
    }

    private static BlockState variant(Block block, Enum<?> value) {
        BlockState state = block.defaultBlockState();
        if (block instanceof InfestedBushBlock && value instanceof InfestedBushBlock.Variant variant) {
            return state.setValue(InfestedBushBlock.VARIANT, variant);
        }
        if (block instanceof ParasiteBushBlock && value instanceof ParasiteBushBlock.Variant variant) {
            return state.setValue(ParasiteBushBlock.VARIANT, variant);
        }
        return state;
    }

    private static boolean canPlaceClean(ServerLevel level, BlockPos pos, BlockState state) {
        return !isLiquid(level, pos) && level.getBlockState(pos).canBeReplaced() && state.canSurvive(level, pos);
    }

    private static boolean isLiquid(ServerLevel level, BlockPos pos) {
        return !level.getFluidState(pos).isEmpty();
    }

    /** Original {@code smoke}: SMOKE_LARGE x6 around the block centre. */
    private static void smoke(ServerLevel level, BlockPos pos) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D, 0.01D);
    }
}
