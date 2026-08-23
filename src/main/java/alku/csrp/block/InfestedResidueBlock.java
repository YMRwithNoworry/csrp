package alku.csrp.block;

import alku.csrp.entity.Parasite;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.ReinforcementSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Thin infested remains left by carriers and used by the reinforcement system. */
public final class InfestedResidueBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    public InfestedResidueBlock(Properties properties) {
        super(properties);
    }

    @Override
public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ReinforcementSystem.tryFromResidue(level, pos, random);
    }

    @Override
public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living) || living instanceof Parasite
                || living instanceof Player player && player.isShiftKeyDown()) {
            return;
        }
        living.setDeltaMovement(living.getDeltaMovement().multiply(0.84D, 1.0D, 0.86D));
        if (!living.hasEffect(ModMobEffects.COTH.get()) && !living.hasEffect(ModMobEffects.REPEL.get())) {
            InfectionMechanics.applyCoth(living, null);
        }
    }
}
