package alku.csrp.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Port of the block-state half of the 1.12.2 relay family —
 * {@code BlockRelayController} (out109 {@code block/BlockRelayController.java}),
 * {@code BlockRelay} (out109 {@code block/BlockRelay.java}, {@code relay_controller_dummy}) and
 * {@code BlockNodeRelay} (out109 {@code block/BlockNodeRelay.java}, {@code noderelay}).
 *
 * <p>All three are SRP network machines whose simulation lived in their tile entities
 * ({@code TileEntityRelayController}, {@code TileEntityNodeRelay}).  The 26.3 port already models
 * that network through {@link RelayTerminalBlock} / {@code RelayTerminalBlockEntity} and
 * {@link NodeLampBlock}.  This class reproduces the parts that belong to the <em>block</em>:</p>
 * <ul>
 *   <li>the horizontal {@code facing} metadata of {@code relaycontroller}, default {@code NORTH}
 *       ({@code BlockRelayController.java}, and the shipped
 *       {@code blockstates/relaycontroller.json} which only lists the four facings);</li>
 *   <li>an optional {@code lit} boolean used by the lit relay/controller states, mirroring the
 *       {@code lit} flag the original toggled when a relay was powered.</li>
 * </ul>
 *
 * <p>The tile-entity simulation (relay linking, controller GUI, node healing) stays with the ported
 * relay machinery; wiring these legacy ids to a registered {@code BlockEntityType} requires an entry
 * in {@code registry/ModBlockEntities.java}, which is outside this task's write scope.</p>
 */
public class LegacyRelayBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /**
     * {@link #createBlockStateDefinition} runs from the superclass constructor, i.e. before instance
     * fields are assigned, so {@code hasLitState} cannot be a plain field here — reading it there left
     * the {@code lit} property out of the state definition and made the constructor's
     * {@code setValue(LIT, ...)} throw
     * {@code IllegalArgumentException: Cannot set property BooleanProperty{name=lit ...}}.
     * The factory publishes the flag before constructing the block instead.
     */
    private static final ThreadLocal<Boolean> PENDING_LIT_STATE = new ThreadLocal<>();

    private final boolean hasLitState;

    private LegacyRelayBlock(Properties properties, boolean hasLitState) {
        super(properties);
        this.hasLitState = hasLitState;
    }

    /** Creates a relay block; {@code hasLitState} mirrors the shipped {@code relaycontroller} asset. */
    public static LegacyRelayBlock create(Properties properties, boolean hasLitState) {
        PENDING_LIT_STATE.set(hasLitState);
        LegacyRelayBlock block;
        try {
            block = new LegacyRelayBlock(properties, hasLitState);
        } finally {
            PENDING_LIT_STATE.remove();
        }
        block.registerDefaultState(hasLitState
                ? block.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false)
                : block.stateDefinition.any().setValue(FACING, Direction.NORTH));
        return block;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        if (Boolean.TRUE.equals(PENDING_LIT_STATE.get())) {
            builder.add(LIT);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return hasLitState ? state.setValue(LIT, false) : state;
    }

    /** {@code relay_controller_dummy} exposes no state at all in the shipped asset. */
    public static final class Dummy extends Block {
        public Dummy(Properties properties) {
            super(properties);
        }
    }

    /** {@code noderelay} exposes no state either. */
    public static final class Node extends Block {
        public Node(Properties properties) {
            super(properties);
        }
    }
}
