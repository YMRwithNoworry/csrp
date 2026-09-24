package alku.csrp.block;

import alku.csrp.infection.BlockInfestation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockInfestedRemain}
 * (out109 {@code block/BlockInfestedRemain.java}) — registered as {@code srparasites:infestedremain}
 * ({@code init/SRPBlocks.java}, the "remain" layer left behind by carriers).
 *
 * <p>Reproduced behaviour:</p>
 * <ul>
 *   <li>{@code TALL_GRASS_AABB} = {@code AABB(0, 0, 0, 1, 0.125, 1)} and the {@code source} 0..1 /
 *       {@code infested_base} metadata ({@code BlockInfestedRemain.java:5-6,13}).</li>
 *   <li>slipperiness {@code 0.52} ({@code BlockInfestedRemain.java:23}) — supplied by the registry
 *       through {@code Properties#friction}.</li>
 *   <li>{@code onEntityWalk} ({@code BlockInfestedRemain.java:41-48}): a grounded living entity that
 *       is not sneaking has its horizontal motion damped to {@code 0.84}.</li>
 *   <li>harvest ({@code BlockInfestedRemain.java:50-64}): only a shovel yields the block.</li>
 *   <li>{@code updateTick} ({@code BlockInfestedRemain.java:92-...}): unsupported remains are cleared
 *       and half the ticks spread the surrounding infestation.</li>
 * </ul>
 */
public final class InfestedRemainBlock extends BushBlock {
    public static final IntegerProperty SOURCE = IntegerProperty.create("source", 0, 1);
    public static final BooleanProperty INFESTED_BASE = BooleanProperty.create("infested_base");

    /** {@code TALL_GRASS_AABB} = AABB(0.0, 0.0, 0.0, 1.0, 0.125, 1.0). */
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    /** {@code entityIn.motionX *= 0.84; entityIn.motionZ *= 0.84;}. */
    private static final double WALK_DAMPING = 0.84D;
    /** {@code rand.nextDouble() < 0.5} spread branch. */
    private static final double SPREAD_CHANCE = 0.5D;

    public InfestedRemainBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any()
                .setValue(SOURCE, 0)
                .setValue(INFESTED_BASE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOURCE, INFESTED_BASE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.onGround() && entity instanceof LivingEntity living && !living.isShiftKeyDown()) {
            living.setDeltaMovement(living.getDeltaMovement().multiply(WALK_DAMPING, 1.0D, WALK_DAMPING));
        }
    }

    /** {@code BlockInfestedRemain.getDrops} — a shovel (main or off hand) yields the block. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack tool
                && !tool.isEmpty() && (tool.is(Items.WOODEN_SHOVEL) || tool.is(Items.STONE_SHOVEL)
                        || tool.is(Items.IRON_SHOVEL) || tool.is(Items.GOLDEN_SHOVEL)
                        || tool.is(Items.DIAMOND_SHOVEL) || tool.is(Items.NETHERITE_SHOVEL))) {
            return List.of(new ItemStack(asItem()));
        }
        return List.of();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (random.nextDouble() < SPREAD_CHANCE) {
            BlockInfestation.spread(level, pos, 0, random);
        }
    }

    /** {@code BlockInfestedRemain.func_176199_a} also ran for players stepping on the layer. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide() || !(entity instanceof Player player) || player.isShiftKeyDown()) {
            return;
        }
        player.setDeltaMovement(player.getDeltaMovement().multiply(WALK_DAMPING, 1.0D, WALK_DAMPING));
    }
}
