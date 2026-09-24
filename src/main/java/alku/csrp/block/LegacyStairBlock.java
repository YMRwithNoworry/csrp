package alku.csrp.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 stair family — {@code BlockStairBase} (out109 {@code block/BlockStairBase.java})
 * and {@code BlockHarleskinnStairs} (out109 {@code block/BlockHarleskinnStairs.java}).
 *
 * <p>Both are plain {@code BlockStairs} subclasses.  {@code BlockStairBase} only re-registered the
 * vanilla default state ({@code NORTH}, {@code BOTTOM}, {@code STRAIGHT} —
 * {@code BlockStairBase.java:16-22}), set the light opacity to 255 and added a harvest-tool helper;
 * {@code BlockHarleskinnStairs} set hardness 1.5F, resistance 10.0F and the flesh sound.  The 26.3
 * {@link StairBlock} already carries {@code FACING} / {@code HALF} / {@code SHAPE}, which is what all
 * twelve {@code *stairs.json} blockstates require.</p>
 *
 * <p>The {@code harvestLevel}/{@code isToolEffective} helpers of the original are obsolete in 26.3
 * (tool requirements are tag-based), so only the state shape and material properties are ported.</p>
 */
public class LegacyStairBlock extends StairBlock {
    public LegacyStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }

    /** Helper mirroring the original's {@code setToolStats} for the registry call sites. */
    static Properties stairProperties(Properties properties, SoundType sound) {
        return properties.sound(sound);
    }
}
