package alku.csrp.block;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasitePlank}
 * (out109 {@code block/BlockParasitePlank.java}) — registered as {@code srparasites:parasiteplank}
 * ({@code init/SRPBlocks.java:548}, hardness 2.2F, resistance 5.0F, harvest axe level 0,
 * {@code SoundType.WOOD}).
 *
 * <p>Reproduced behaviour: the 2-value {@code variant} metadata with {@code DEADHEAD} as the default
 * ({@code BlockParasitePlank.java:18,25}).  The original's {@code randomDisplayTick}
 * ({@code BlockParasitePlank.java:71-84}) only spawned the client-side SPORE particle while the block
 * above was air or a parasite bush; that is a pure client effect and lives in the client renderer for
 * this port, so the server-side behaviour here is complete.</p>
 */
public final class ParasitePlankBlock extends Block {
    /** {@code BlockParasitePlank.VARIANT}. */
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public ParasitePlankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.DEADHEAD));
    }

    /** The 2 original metadata constants, in the original declaration order. */
    public enum Variant implements StringRepresentable {
        DEADHEAD("deadhead"),
        DEADHEADS("deadheads");

        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String toString() {
            return name.toLowerCase(Locale.ROOT);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }
}
