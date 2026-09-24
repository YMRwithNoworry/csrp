package alku.csrp.block;

import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scanandrunparasites.block.BlockParasiteCactus} —
 * out109 {@code block/BlockParasiteCactus.java} — registered as
 * {@code srparasites:infested_cactus} ({@code init/SRPBlocks.java:645}) with hardness 0.4F and
 * resistance 0.4F.
 *
 * <p>Behaviour reproduced from the original:</p>
 * <ul>
 *   <li>{@code canBlockStay} ({@code BlockParasiteCactus.java:76-89}): the block must stand on sand /
 *       red sand / infested sand / another infested cactus, and no horizontal neighbour may be a
 *       solid material.</li>
 *   <li>{@code entityInside} ({@code BlockParasiteCactus.java:91-119}): besides the inherited cactus
 *       damage it pushes players away from the block centre with
 *       {@code PUSH_H = 0.35}, {@code PUSH_Y = 0.08} and an 8-tick per-player cooldown stored under
 *       the NBT key {@code srp_pcactus_last_push}.</li>
 *   <li>{@code isEntityInsideOpaqueBlock}/parasite friendliness: parasites never take cactus
 *       damage ({@code func_189872_a}, line 70-72).</li>
 *   <li>{@code removedByPlayer} triggered the SRP "dislodgement" world event; the ported
 *       equivalent is the shared {@link alku.csrp.world.DislodgmentSystem} hook used by the other
 *       infested blocks, which is out of scope for this class.</li>
 * </ul>
 */
public final class InfestedCactusBlock extends CactusBlock {
    /** NBT key of the per-player push cooldown — {@code BlockParasiteCactus.NBT_PCACTUS_LAST_PUSH}. */
    private static final String NBT_LAST_PUSH = "srp_pcactus_last_push";
    /** {@code PUSH_H}. */
    private static final double PUSH_HORIZONTAL = 0.35D;
    /** {@code PUSH_Y}. */
    private static final double PUSH_VERTICAL = 0.08D;
    /** {@code PUSH_CD} — ticks. */
    private static final long PUSH_COOLDOWN_TICKS = 8L;

    public InfestedCactusBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (neighbour.isSolid() && !neighbour.is(this)) {
                return false;
            }
        }
        BlockState below = level.getBlockState(pos.below());
        // Original accepted Blocks.SAND, Blocks.RED_SAND, SRPBlocks.InfestedSand or itself.
        return below.is(BlockTags.SAND) || below.is(this) || below.is(ModBlocks.INFESTED_SAND.get());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(entity instanceof Parasite)) {
            entity.hurt(level.damageSources().cactus(), 1.0F);
        }
        if (level.isClientSide() || !(entity instanceof Player player) || player.isRemoved()) {
            return;
        }
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        long last = data.getLongOr(NBT_LAST_PUSH, Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && now - last < PUSH_COOLDOWN_TICKS) {
            return;
        }
        data.putLong(NBT_LAST_PUSH, now);
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        double magnitude = Math.sqrt(dx * dx + dz * dz);
        if (magnitude < 1.0E-4D) {
            dx = 0.0D;
            dz = 1.0D;
            magnitude = 1.0D;
        }
        // 26.3: Entity#push already flags the motion for a client sync (Entity.needsSync), which is
        // what the 1.12.2 `hurtMarked = true` assignment asked for — no extra flag exists here.
        player.push(dx / magnitude * PUSH_HORIZONTAL, PUSH_VERTICAL, dz / magnitude * PUSH_HORIZONTAL);
        // 1.12.2 forced the pushed velocity to the client with `hurtMarked`; 26.3 exposes the same
        // flag as `Entity#syncVelocity`.
        player.syncVelocity = true;
    }

    /** Kept so the block still reports a cactus-shaped collision like the original. */
    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    /** Unused helper retained for parity with the original's sand check. */
    static boolean isSandLike(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.SAND);
    }

    /** Server-side entry point used by the dislodgement system when this block is broken. */
    public static void onRemoved(ServerLevel level, BlockPos pos) {
        // The original raised the EVENTPARABLOCKBR dislodgement roll here; the ported system lives in
        // alku.csrp.world.DislodgmentSystem and is driven from the shared BlockBase-equivalent path.
    }
}
