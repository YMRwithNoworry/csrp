package alku.csrp.block;

import alku.csrp.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockLeafLike}
 * (out109 {@code block/BlockLeafLike.java}) — registered twice as {@code srparasites:infested_leaves}
 * and {@code srparasites:infested_leaves_fast} ({@code init/SRPBlocks.java:629-630}).
 *
 * <p>The original ran a single random-tick decay ladder
 * ({@code BlockLeafLike.java:45-64}):</p>
 * <ul>
 *   <li>while any of the six neighbours belongs to the {@code srparasites} namespace the leaf heals
 *       — {@code decay_age} drops by one with probability {@code 1/6} ({@code rand.nextInt(6) == 0})
 *       when it is above 0;</li>
 *   <li>otherwise it decays: probability {@code 1/12} ({@code rand.nextInt(12) == 0}) either raises
 *       {@code decay_age} (capped at 5) or, once {@code decay_age} already sits at 5, destroys the
 *       block with drops.</li>
 * </ul>
 *
 * <p>Drops come from {@code getDrops} ({@code BlockLeafLike.java:88-94}): the block itself never
 * drops ({@code getItemDropped} returns {@code null}, line 96, and {@code quantityDropped} returns 0,
 * line 100) — only the {@code falseapple} item does, with a {@code 1 / max(1, 40 - 8 * fortune)}
 * chance.  Shearing yields the block itself; that is modelled by the {@code shears} loot-table
 * convention, and the vanilla {@code GET_DROPS} path is reproduced here explicitly.</p>
 *
 * <p>The {@code _fast} id used the very same class in 1.10.9 (both entries are bare
 * {@code new BlockLeafLike(name)} calls), so the {@code fast} flag only records which registry id
 * this instance serves.</p>
 */
public final class InfestedLeavesBlock extends Block {
    /** {@code BlockLeafLike.DECAY_AGE} — 0..5. */
    public static final IntegerProperty DECAY_AGE = IntegerProperty.create("decay_age", 0, 5);

    /** {@code BASE_FALSE_APPLE_CHANCE} — the 1-in-40 base apple chance. */
    private static final int BASE_FALSE_APPLE_CHANCE = 40;
    /** Fortune removes 8 from the divisor per level ({@code 40 - 8 * fortune}). */
    private static final int FALSE_APPLE_FORTUNE_STEP = 8;
    /** {@code rand.nextInt(6) == 0} healing chance while SRP blocks surround the leaf. */
    private static final int HEAL_DIVISOR = 6;
    /** {@code rand.nextInt(12) == 0} decay chance. */
    private static final int DECAY_DIVISOR = 12;
    private static final int MAX_DECAY_AGE = 5;

    private final boolean fast;

    public InfestedLeavesBlock(Properties properties, boolean fast) {
        super(properties.randomTicks());
        this.fast = fast;
        registerDefaultState(stateDefinition.any().setValue(DECAY_AGE, 0));
    }

    /** {@code true} for the {@code infested_leaves_fast} registry entry. */
    public boolean fast() {
        return fast;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECAY_AGE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(DECAY_AGE);
        if (touchingAnySrp(level, pos)) {
            if (age > 0 && random.nextInt(HEAL_DIVISOR) == 0) {
                level.setBlock(pos, state.setValue(DECAY_AGE, age - 1), Block.UPDATE_CLIENTS);
            }
            return;
        }
        if (random.nextInt(DECAY_DIVISOR) != 0) {
            return;
        }
        if (age < MAX_DECAY_AGE) {
            level.setBlock(pos, state.setValue(DECAY_AGE, age + 1), Block.UPDATE_CLIENTS);
        } else {
            level.destroyBlock(pos, true);
        }
    }

    /** {@code BlockLeafLike.touchingAnySRP} — any of the six neighbours in the SRP namespace. */
    private static boolean touchingAnySrp(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            Block block = level.getBlockState(pos.relative(direction)).getBlock();
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null && "srparasites".equals(id.getNamespace())) {
                return true;
            }
        }
        return false;
    }

    /** {@code BlockLeafLike.java:88-94} — only the false apple drops, scaled by Fortune. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int fortune = 0;
        ItemInstance tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool instanceof ItemStack stack && !stack.isEmpty()) {
            fortune = EnchantmentHelper.getItemEnchantmentLevel(
                    params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.FORTUNE),
                    stack);
        }
        int chance = Math.max(1, BASE_FALSE_APPLE_CHANCE - FALSE_APPLE_FORTUNE_STEP * Math.max(0, fortune));
        List<ItemStack> drops = new ArrayList<>();
        if (params.getLevel().getRandom().nextInt(chance) == 0) {
            drops.add(new ItemStack(ModItems.FALSE_APPLE.get()));
        }
        return drops;
    }
}
