package alku.csrp.block;

import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockDod}
 * (out109 {@code block/BlockDod.java}) — registered as {@code srparasites:dispatchern}
 * ({@code init/SRPBlocks.java:973}: {@code new BlockDod("dispatchern", 0.1F, true, true, 0.1F)}).
 *
 * <p>Reproduced behaviour:</p>
 * <ul>
 *   <li>{@code DOD_AABB} = {@code AABB(0.0625, 0, 0.0625, 0.9375, 1, 0.9375)}
 *       ({@code BlockDod.java:7,47}) used for both the shape and the collision box;
 *       hardness 0.1F, resistance 0.1F, flesh sound.</li>
 *   <li>{@code pushAndHit} ({@code BlockDod.java:51-73}), reached from both {@code entityInside} and
 *       {@code onEntityWalk}: the player is nudged backwards along their look vector by
 *       {@code 0.3} (plus {@code 0.05} upward), and — at most once every 20 ticks
 *       ({@code srp_dod_last_hit} NBT key) — takes 4.0F from the {@code srp_dod_block} damage
 *       source, is thrown back by {@code 1.2}, hears the adaptation sound and receives the
 *       DOD smoke-trail effect for 20 ticks.</li>
 * </ul>
 */
public final class DispatcherNBlock extends Block {
    /** {@code DOD_AABB}. */
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    private static final String NBT_LAST_HIT = "srp_dod_last_hit";
    private static final double SOFT_STRENGTH = 0.3D;
    private static final double SOFT_Y = 0.05D;
    private static final double HIT_STRENGTH = 1.2D;
    private static final double HIT_Y = 0.25D;
    private static final float HIT_DAMAGE = 4.0F;
    private static final long HIT_COOLDOWN_TICKS = 20L;

    public DispatcherNBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        pushAndHit(level, pos, entity);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        pushAndHit(level, pos, entity);
    }

    /** {@code BlockDod.pushAndHit}. */
    private static void pushAndHit(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof Player player) || player.isRemoved()) {
            return;
        }
        float yawRadians = (float) Math.toRadians(player.getYRot());
        double lookX = -Math.sin(yawRadians);
        double lookZ = Math.cos(yawRadians);
        player.push(-lookX * SOFT_STRENGTH, SOFT_Y, -lookZ * SOFT_STRENGTH);
        player.syncVelocity = true;
        CompoundTag data = player.getPersistentData();
        long now = level.getGameTime();
        if (now - data.getLongOr(NBT_LAST_HIT, Long.MIN_VALUE) < HIT_COOLDOWN_TICKS) {
            return;
        }
        data.putLong(NBT_LAST_HIT, now);
        player.push(-lookX * HIT_STRENGTH, HIT_Y, -lookZ * HIT_STRENGTH);
        player.syncVelocity = true;
        player.hurt(level.damageSources().generic(), HIT_DAMAGE);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, pos, alku.csrp.registry.ModSounds.ADAPTATION_FULL.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (!player.hasEffect(ModMobEffects.DOD_SMOKE_TRAIL)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    ModMobEffects.DOD_SMOKE_TRAIL, 20, 0, false, false));
        }
    }

    /** Unused, retained for parity with the original's empty tick hook ({@code BlockDod.java:26-32}). */
    static void tickNoop(Level level, BlockPos pos) {
        // The 1.12.2 updateTick only performed an isAreaLoaded guard and did nothing else.
    }
}
