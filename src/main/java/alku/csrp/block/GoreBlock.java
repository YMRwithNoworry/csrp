package alku.csrp.block;

import alku.csrp.entity.Parasite;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockGore}
 * (out109 {@code block/BlockGore.java}) — the six {@code gore*} ids
 * ({@code goreada}, {@code gorefer}, {@code goremar}, {@code gorepri}, {@code gorepur},
 * {@code goresim}) all share this class and differ only by texture.
 *
 * <p>Original behaviour that is reproduced here:</p>
 * <ul>
 *   <li>{@code VARIANT} metadata ({@code flat}/{@code small}/{@code big}), default {@code SMALL}
 *       ({@code BlockGore.java:46}); the variant only changes how fast the gore decays.</li>
 *   <li>Shape {@code TALL_GRASS_AABB = AABB(0.1, 0, 0.1, 0.9, 0.8, 0.9)}
 *       ({@code BlockGore.java:38,56}).</li>
 *   <li>Placement requires a solid block underneath ({@code checkBush -> state.isFullBlock()},
 *       {@code BlockGore.java:69}); a neighbour update pops the block when that is no longer true
 *       ({@code BlockGore.java:73}).</li>
 *   <li>Random tick removes the gore with probability {@code 1/10} ({@code 1/45} for
 *       {@code BIG}) — {@code BlockGore.java:97}.</li>
 *   <li>{@code entityInside} infects non-parasite living entities with COTH for 3600 ticks
 *       ({@code BlockGore.java:80}); 50&nbsp;% of the ticks are skipped unless the entity's age is
 *       a multiple of 20, and entities that already carry COTH or E.P.E.L. are left alone.</li>
 *   <li>No drops ({@code BlockGore.java:119}).</li>
 * </ul>
 */
public class GoreBlock extends BushBlock {
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    /** {@code TALL_GRASS_AABB} scaled to the 26.3 16-unit voxel space. */
    private static final VoxelShape SHAPE = Block.box(1.6D, 0.0D, 1.6D, 14.4D, 12.8D, 14.4D);

    /** Decay denominators from {@code BlockGore.java:99-104}. */
    private static final int DECAY_CHANCE_SMALL = 10;
    private static final int DECAY_CHANCE_BIG = 45;

    /** {@code EntityLivingBase.field_70173_aa % 20 != 0} gate from {@code BlockGore.java:82}. */
    private static final int INFECTION_AGE_MODULUS = 20;
    private static final double INFECTION_SKIP_CHANCE = 0.5D;

    public enum Variant implements StringRepresentable {
        FLAT("flat"),
        SMALL("small"),
        BIG("big");

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

    public GoreBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.SMALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** {@code BlockGore.checkBush} — the original accepted any full block below. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    /** {@code BlockGore.func_189540_a} — remove the gore when its support disappears. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            Orientation orientation, boolean movedByPiston) {
        if (!canSurvive(state, level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide()) {
            if (level.getRandom().nextDouble() < INFECTION_SKIP_CHANCE
                    && entity.tickCount % INFECTION_AGE_MODULUS != 0) {
                return;
            }
            if (!(entity instanceof Parasite) && entity instanceof LivingEntity living
                    && !living.hasEffect(ModMobEffects.COTH)
                    && !living.hasEffect(ModMobEffects.REPEL)) {
                // PotionEffect(COTH_E, 3600, 0, false, false) — BlockGore.java:89.
                InfectionMechanics.applyCoth(living, null);
            }
        }
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int decayChance = state.getValue(VARIANT) == Variant.BIG ? DECAY_CHANCE_BIG : DECAY_CHANCE_SMALL;
        if (random.nextInt(decayChance) == 0) {
            level.removeBlock(pos, false);
        }
    }

    /** {@code BlockGore.func_180660_a} returned {@code null}: gore never drops an item. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
