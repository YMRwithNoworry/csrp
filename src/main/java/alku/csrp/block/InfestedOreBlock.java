package alku.csrp.block;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockInfestedOre}
 * (out109 {@code block/BlockInfestedOre.java}) — registered as {@code srparasites:infestedore}
 * ({@code init/SRPBlocks.java:974}, hardness 3.5F).
 *
 * <p>The original extended {@code BlockBase} with {@code tickRandom = true}, but both tick hooks
 * ({@code updateTick}, line 76, and {@code onEntityWalk}, line 79) were empty overrides, so the ore
 * never spreads: the only observable behaviour is the 8-value {@code variant} metadata
 * ({@code EnumType}, default {@code CO}, line 25) and the {@code blockinfest.*} sound type, which the
 * registry supplies.  The 1.12.2 {@code removedByPlayer} override (line 68) only forwarded to
 * {@code breakBlock} plus a {@code setBlockToAir}, i.e. the vanilla break path, so nothing extra is
 * required on 26.3.</p>
 */
public final class InfestedOreBlock extends Block {
    /** {@code BlockInfestedOre.VARIANT}. */
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public InfestedOreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.CO));
    }

    /** The 8 original metadata constants, in the original declaration order. */
    public enum Variant implements StringRepresentable {
        CO("co"),
        DIA("dia"),
        EME("eme"),
        GOL("gol"),
        IRO("iro"),
        LAP("lap"),
        RED("red"),
        UN("un");

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
