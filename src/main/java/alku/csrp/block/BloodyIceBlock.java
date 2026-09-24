package alku.csrp.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockBloodyIce}
 * (out109 {@code block/BlockBloodyIce.java}) — registered as
 * {@code srparasites:bloodyice} ({@code init/SRPBlocks.java:423}) and placed by the parasite
 * biome decorator ({@code world/ParasiteBiomeDecorator.java:692,775}).
 *
 * <p>Faithful details:</p>
 * <ul>
 *   <li>It is a {@link ParasiteSpreadingBlock} with {@code isInfestedBlock == true}
 *       ({@code BlockBloodyIce.java:24} forwards {@code infested} to the parent); the parent
 *       reproduces the original {@code updateTick}.</li>
 *   <li>Slipperiness {@code 0.98} ({@code this.field_149765_K = 0.98F}, line 25) — supplied
 *       through {@code BlockBehaviour.Properties#friction} by the registry entry.</li>
 *   <li>{@code SoundType.GLASS} (line 26), translucent and non-opaque rendering (lines 36-63).</li>
 *   <li>No drops at all ({@code func_149745_a} returns 0, line 40).</li>
 *   <li>Falling on it hard shatters a {@code bloodyIceBreakDiameter}-wide patch of bloody ice when a
 *       player lands after more than {@code bloodyIceBreakFallDistance} blocks
 *       ({@code func_180658_a}, lines 67-94). The 1.12.2 configuration defaults are used verbatim
 *       because {@code config/**} is outside this task's write scope:
 *       {@code bloodyIceBreakOnHardLanding = true},
 *       {@code bloodyIceBreakFallDistance = 5.0},
 *       {@code bloodyIceBreakDiameter = 3}
 *       ({@code util/config/SRPConfigWorld.java:26,32,35}).</li>
 * </ul>
 */
public final class BloodyIceBlock extends ParasiteSpreadingBlock {
    /** {@code SRPConfigWorld.bloodyIceBreakOnHardLanding}. */
    public static final boolean BREAK_ON_HARD_LANDING = true;
    /** {@code SRPConfigWorld.bloodyIceBreakFallDistance} (blocks). */
    public static final float BREAK_FALL_DISTANCE = 5.0F;
    /** {@code SRPConfigWorld.bloodyIceBreakDiameter} (blocks, clamped to at least 1). */
    public static final int BREAK_DIAMETER = 3;

    public BloodyIceBlock(Properties properties) {
        super(properties, true);
    }

    /** {@code BlockBloodyIce.func_149745_a} returned 0 — the block never drops itself. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    /**
     * 26.3 replacement for the 1.12.2 {@code onFallenUpon(World, BlockPos, Entity, float)} hook:
     * the engine calls {@code fallOn} for the block that was landed on and still passes the fall
     * distance, which is exactly the value the original tested.
     */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (!level.isClientSide() && BREAK_ON_HARD_LANDING && entity instanceof Player
                && fallDistance >= BREAK_FALL_DISTANCE) {
            breakPatch(level, pos);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    /** {@code BlockBloodyIce.java:76-89} — shatter every bloody ice block in the square patch. */
    private static void breakPatch(Level level, BlockPos center) {
        int radius = (Math.max(1, BREAK_DIAMETER) - 1) / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos target = center.offset(dx, 0, dz);
                BlockState targetState = level.getBlockState(target);
                if (targetState.getBlock() instanceof BloodyIceBlock) {
                    level.levelEvent(2001, target, Block.getId(targetState));
                    level.destroyBlock(target, true);
                }
            }
        }
    }
}
