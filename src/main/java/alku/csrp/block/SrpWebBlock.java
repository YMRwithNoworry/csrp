package alku.csrp.block;

import alku.csrp.entity.Parasite;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

/**
 * SRP webbing blocks (Thin / Toxicant / Tarnished). They slow down and
 * optionally damage non-parasite entities, are harmless to parasites, and
 * dissolve on their own after a while.
 */
public class SrpWebBlock extends Block {
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    public enum Kind implements StringRepresentable {
        THIN("thin", 0.25D, 0.0F),
        TOXICANT("toxicant", 0.18D, 2.0F),
        TARNISHED("tarnished", 0.12D, 4.0F);

        private final String name;
        private final double slowFactor;
        private final float damage;

        Kind(String name, double slowFactor, float damage) {
            this.name = name;
            this.slowFactor = slowFactor;
            this.damage = damage;
        }

        public double slowFactor() {
            return slowFactor;
        }

        public float damage() {
            return damage;
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

    public SrpWebBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(KIND, Kind.THIN)
                .setValue(AGE, 0));
    }

    @Override
public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, AGE);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof Parasite) {
            return;
        }
        Kind kind = state.getValue(KIND);
        entity.makeStuckInBlock(state, new Vec3(kind.slowFactor(), 0.05D, kind.slowFactor()));
        if (!level.isClientSide && kind.damage() > 0.0F && level.getGameTime() % 20L == 0L) {
            entity.hurt(level.damageSources().magic(), kind.damage());
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE) + 1;
        if (age > 7) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            level.setBlockAndUpdate(pos, state.setValue(AGE, age));
        }
    }
}
