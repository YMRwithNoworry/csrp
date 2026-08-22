package alku.csrp.fluid;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModFluidTypes;
import alku.csrp.registry.ModFluids;
import alku.csrp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.fluids.FluidType;

/**
 * Dead Blood: a dangerous parasite hemolymph liquid. It damages non-parasites,
 * heals parasites, cannot form infinite sources, and converts water into
 * Visceral Mud and lava into Bleeding Obsidian.
 */
public abstract class DeadBloodFluid extends FlowingFluid {
    @Override
    public Fluid getFlowing() {
        return ModFluids.DEADBLOOD_FLOWING.get();
    }

    @Override
    public Fluid getSource() {
        return ModFluids.DEADBLOOD.get();
    }

    @Override
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 4;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 1;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return ModBlocks.DEAD_BLOOD.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == ModFluids.DEADBLOOD.get() || fluid == ModFluids.DEADBLOOD_FLOWING.get();
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
            Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !fluid.is(FluidTags.WATER);
    }

    @Override
    public FluidType getFluidType() {
        return ModFluidTypes.DEAD_BLOOD.get();
    }

    @Override
    public Item getBucket() {
        return ModItems.DEADBLOOD_BUCKET.get();
    }

    @Override
    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState,
            Direction direction, FluidState fluidState) {
        if (direction == Direction.DOWN) {
            FluidState targetFluid = level.getFluidState(pos);
            if (targetFluid.is(Fluids.WATER)) {
                level.setBlock(pos, ModBlocks.VISCERAL_MUD.get().defaultBlockState(), 3);
                return;
            }
            if (targetFluid.is(Fluids.LAVA)) {
                level.setBlock(pos, ModBlocks.BLEEDING_OBSIDIAN.get().defaultBlockState(), 3);
                return;
            }
        }
        super.spreadTo(level, pos, blockState, direction, fluidState);
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (state.isSource() && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                    pos.getY() + 0.7D + random.nextDouble() * 0.2D,
                    pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                    0.0D, 0.03D, 0.0D);
        }
    }

    public static final class Source extends DeadBloodFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends DeadBloodFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
