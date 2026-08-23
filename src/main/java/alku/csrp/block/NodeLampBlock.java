package alku.csrp.block;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Detects nearby Dispatcher stages and exposes distance through comparator output. */
public final class NodeLampBlock extends Block {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final IntegerProperty RANGE_LEVEL = IntegerProperty.create("range_level", 0, 5);
    private static final double RANGE = 250.0D;

    public NodeLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(RANGE_LEVEL, 0));
    }

    @Override
public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, RANGE_LEVEL);
    }

    @Override
public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            updateLamp(level, pos, state);
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateLamp(level, pos, state);
        level.scheduleTick(pos, this, 100);
    }

    @Override
public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
            BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            updateLamp(level, pos, state);
        }
    }

    @Override
public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(RANGE_LEVEL) * 3;
    }

    @Override
public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            List<Entity> dispatchers = dispatchers(level, pos);
            int strength = dispatchers.stream().mapToInt(NodeLampBlock::dispatcherStage).max().orElse(0);
            double nearest = dispatchers.stream().mapToDouble(entity -> entity.distanceToSqr(pos.getCenter()))
                    .min().orElse(Double.MAX_VALUE);
            int rawRange = rangeLevel(nearest);
            if (!state.getValue(POWERED)) {
                strength = 0;
                rawRange = 0;
            }
            int displayedDistance = rawRange == 0 ? 0 : 6 - rawRange;
            player.sendSystemMessage(Component.translatable("message.csrp.node_lamp.strength", roman(strength)));
            player.sendSystemMessage(Component.translatable("message.csrp.node_lamp.distance", displayedDistance));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void updateLamp(Level level, BlockPos pos, BlockState state) {
        double nearest = dispatchers(level, pos).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getCenter())))
                .map(entity -> entity.distanceToSqr(pos.getCenter())).orElse(Double.MAX_VALUE);
        int rangeLevel = rangeLevel(nearest);
        boolean powered = rangeLevel > 0;
        if (powered != state.getValue(POWERED) || rangeLevel != state.getValue(RANGE_LEVEL)) {
            level.setBlock(pos, state.setValue(POWERED, powered).setValue(RANGE_LEVEL, rangeLevel), 3);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    private static List<Entity> dispatchers(Level level, BlockPos pos) {
        return level.getEntities((Entity) null, new AABB(pos).inflate(RANGE), entity ->
                entity.isAlive() && dispatcherStage(entity) > 0);
    }

    private static int dispatcherStage(Entity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return switch (id) {
            case "dispatcher_si" -> 1;
            case "dispatcher_sii" -> 2;
            case "dispatcher_siii" -> 3;
            case "dispatcher_siv" -> 4;
            default -> 0;
        };
    }

    private static int rangeLevel(double distanceSquared) {
        if (distanceSquared <= 2_500.0D) return 5;
        if (distanceSquared <= 5_625.0D) return 4;
        if (distanceSquared <= 10_000.0D) return 3;
        if (distanceSquared <= 22_500.0D) return 2;
        if (distanceSquared <= 40_000.0D) return 1;
        return 0;
    }

    private static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "0";
        };
    }
}
