package alku.csrp.block;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockParasiteStain}
 * (out109 {@code block/BlockParasiteStain.java}) — registered as {@code srparasites:parasitestain}
 * ({@code init/SRPBlocks.java:550}, hardness 0.8F, infested = {@code false}).
 *
 * <p>Reproduced behaviour:</p>
 * <ul>
 *   <li>the 7-value {@code variant} metadata with {@code DIRT} as the default
 *       ({@code BlockParasiteStain.java:22,25}).</li>
 *   <li>{@code getSoundType} ({@code BlockParasiteStain.java:28-40}): {@code DIRT} uses
 *       {@code SoundType.GROUND}, {@code MUD} uses {@code SoundType.MUD}, {@code SPORE} uses
 *       {@code SoundType.GRASS}, everything else uses the SRP flesh sound that the registry supplies
 *       as the block default.</li>
 *   <li>the spreading random tick is inherited from {@link ParasiteSpreadingBlock} with
 *       {@code infested = false}, exactly like the original constructor call.</li>
 * </ul>
 */
public final class ParasiteStainBlock extends ParasiteSpreadingBlock {
    /** {@code BlockParasiteStain.VARIANT}; ordering matches the original enum. */
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public ParasiteStainBlock(Properties properties) {
        super(properties, false);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.DIRT));
    }

    /** The 7 original metadata constants, in the original declaration order. */
    public enum Variant implements StringRepresentable {
        DIRT("dirt"),
        MUD("mud"),
        FLESH("flesh"),
        FEELER("feeler"),
        SPORE("spore"),
        RED("red"),
        SACKFLESH("sackflesh");

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
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        return switch (state.getValue(VARIANT)) {
            case DIRT -> SoundType.GRAVEL;
            case MUD -> SoundType.MUD;
            case SPORE -> SoundType.GRASS;
            default -> super.getSoundType(state);
        };
    }
}
