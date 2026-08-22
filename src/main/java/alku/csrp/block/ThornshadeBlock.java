package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Thornshade's legacy growth, harvest, snow, and withering states. */
public final class ThornshadeBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<ThornshadeBlock> CODEC = simpleCodec(ThornshadeBlock::new);
    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final VoxelShape SEEDLING_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 5.0D, 13.0D);
    private static final VoxelShape GROWING_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 10.0D, 14.0D);
    private static final VoxelShape MATURE_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);

    public ThornshadeBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.STAGE0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Stage current = state.getValue(STAGE);
        boolean snowy = isSnowy(level, pos);
        if (!hasParasiticSoil(level, pos)) {
            level.setBlock(pos, Stage.DEAD.withSnow(snowy).apply(state), Block.UPDATE_CLIENTS);
            return;
        }
        Stage next = switch (current.base()) {
            case STAGE0 -> Stage.STAGE1;
            case STAGE1, STAGE2_NO_BERRY -> Stage.STAGE2;
            default -> current.base();
        };
        next = next.withSnow(snowy);
        if (next != current) {
            level.setBlock(pos, next.apply(state), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        Stage current = state.getValue(STAGE);
        if (current.base() != Stage.STAGE2) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int berries = 1 + level.random.nextInt(2);
            popResource(level, pos, new ItemStack(ModItems.THORNSHADE_BERRY.get(), berries));
            level.setBlock(pos, Stage.STAGE2_NO_BERRY.withSnow(current.snowy()).apply(state), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, ModSounds.MOVING_FLESH_GROW.get(), SoundSource.BLOCKS,
                    0.65F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(STAGE).base()) {
            case STAGE0, DEAD -> SEEDLING_SHAPE;
            case STAGE1 -> GROWING_SHAPE;
            default -> MATURE_SHAPE;
        };
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        Stage stage = state.getValue(STAGE).base();
        return hasParasiticSoil(level, pos)
                && (stage == Stage.STAGE0 || stage == Stage.STAGE1 || stage == Stage.STAGE2_NO_BERRY);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        Stage current = state.getValue(STAGE);
        Stage next = switch (current.base()) {
            case STAGE0 -> Stage.STAGE1;
            case STAGE1, STAGE2_NO_BERRY -> Stage.STAGE2;
            default -> current.base();
        };
        level.setBlock(pos, next.withSnow(isSnowy(level, pos)).apply(state), Block.UPDATE_CLIENTS);
    }

    public BlockState initialState(Level level, BlockPos pos) {
        return Stage.STAGE0.withSnow(isSnowy(level, pos)).apply(defaultBlockState());
    }

    private static boolean isParasiticSoil(BlockState state) {
        return state.getBlock() instanceof InfestedBlock
                || state.is(ModBlocks.INFESTED_REMAINS.get())
                || state.is(ModBlocks.RESIDUE_BLOCK.get())
                || state.is(ModBlocks.RESIDUE_BRICKS.get())
                || state.is(ModBlocks.BIOMASS_BLOCK.get())
                || state.is(ModBlocks.ALVEOLI.get())
                || state.is(ModBlocks.SICK_ALVEOLI.get())
                || state.is(ModBlocks.SOLID_ALVEOLI_BLOCK.get());
    }

    private static boolean hasParasiticSoil(LevelReader level, BlockPos pos) {
        BlockState soil = level.getBlockState(pos.below());
        if (isParasiticSoil(soil)) {
            return true;
        }
        return (soil.is(net.minecraft.world.level.block.Blocks.SNOW)
                || soil.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK))
                && isParasiticSoil(level.getBlockState(pos.below(2)));
    }

    private static boolean isSnowy(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
                || level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.SNOW);
    }

    public enum Stage implements StringRepresentable {
        STAGE0("stage0_ts", false),
        STAGE0_SNOW("stage0_ts_snow", true),
        STAGE1("stage1_ts", false),
        STAGE1_SNOW("stage1_ts_snow", true),
        STAGE2("stage2_ts", false),
        STAGE2_SNOW("stage2_ts_snow", true),
        STAGE2_NO_BERRY("stage2_ts_noberry", false),
        STAGE2_NO_BERRY_SNOW("stage2_ts_noberry_snow", true),
        DEAD("dead_ts", false),
        DEAD_SNOW("dead_ts_snow", true);

        private final String serializedName;
        private final boolean snowy;

        Stage(String serializedName, boolean snowy) {
            this.serializedName = serializedName;
            this.snowy = snowy;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public boolean snowy() {
            return snowy;
        }

        public Stage base() {
            return switch (this) {
                case STAGE0_SNOW -> STAGE0;
                case STAGE1_SNOW -> STAGE1;
                case STAGE2_SNOW -> STAGE2;
                case STAGE2_NO_BERRY_SNOW -> STAGE2_NO_BERRY;
                case DEAD_SNOW -> DEAD;
                default -> this;
            };
        }

        public Stage withSnow(boolean snow) {
            return switch (base()) {
                case STAGE0 -> snow ? STAGE0_SNOW : STAGE0;
                case STAGE1 -> snow ? STAGE1_SNOW : STAGE1;
                case STAGE2 -> snow ? STAGE2_SNOW : STAGE2;
                case STAGE2_NO_BERRY -> snow ? STAGE2_NO_BERRY_SNOW : STAGE2_NO_BERRY;
                case DEAD -> snow ? DEAD_SNOW : DEAD;
                default -> this;
            };
        }

        private BlockState apply(BlockState state) {
            return state.setValue(STAGE, this);
        }
    }
}
