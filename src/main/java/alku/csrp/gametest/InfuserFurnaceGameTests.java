package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.block.entity.InfuserFurnaceBlockEntity;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Runtime tests for the infuser furnace, ported from 1.10.9
 * {@code tileentity/TileEntityInfuserFurnace}.
 *
 * <p>They drive the real {@code serverTick} so the whole original pipeline runs: fuel burn, the 200 tick
 * infusion counter, one item consumed from each input, the main output plus the bottle byproduct, and
 * the vanilla smelting path that shares the smelt slot.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class InfuserFurnaceGameTests {
    private static final String MODID = Csrp.MODID;
    /** {@code COOK_TIME_TOTAL}: both counters finish after 200 ticks. */
    private static final int COOK_TICKS = 200;
    private static final int MAX_TICKS = 400;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final BlockPos FURNACE_POS = new BlockPos(1, 1, 1);

    private InfuserFurnaceGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "infuser_furnace"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "infuser_furnace/infuse_with_byproduct"),
                new InfuseWithByproductTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "infuser_furnace/glass_swap_yields_two"),
                new GlassSwapTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "infuser_furnace/vanilla_smelting"),
                new VanillaSmeltingTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static InfuserFurnaceBlockEntity placeFurnace(GameTestHelper helper) {
        helper.setBlock(FURNACE_POS, ModBlocks.INFUSER_FURNACE.get());
        InfuserFurnaceBlockEntity furnace =
                helper.getBlockEntity(FURNACE_POS, InfuserFurnaceBlockEntity.class);
        helper.assertTrue(furnace != null, Component.literal("the infuser furnace has no block entity"));
        return furnace;
    }

    /** Runs the real tick loop; the original needs one burning tick per tick of progress. */
    private static void runTicks(GameTestHelper helper, InfuserFurnaceBlockEntity furnace, int ticks) {
        BlockState state = helper.getBlockState(FURNACE_POS);
        for (int tick = 0; tick < ticks; tick++) {
            InfuserFurnaceBlockEntity.serverTick(helper.getLevel(), FURNACE_POS, state, furnace);
        }
    }

    /** Iron ingot + dead blood fluid -> semi-organic ingot, leaving the original's glass bottle. */
    private static final class InfuseWithByproductTest extends GameTestInstance {
        InfuseWithByproductTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            InfuserFurnaceBlockEntity furnace = placeFurnace(helper);
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_SMELT_IN, new ItemStack(Items.IRON_INGOT, 2));
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_INFUSE_IN,
                    new ItemStack(ModItems.DEADBLOOD_FLUID.get(), 2));
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));

            runTicks(helper, furnace, COOK_TICKS);

            ItemStack output = furnace.getItem(InfuserFurnaceBlockEntity.SLOT_INFUSE_OUT);
            ItemStack bottle = furnace.getItem(InfuserFurnaceBlockEntity.SLOT_BOTTLE_OUT);
            helper.assertTrue(output.is(ModItems.SEMIORGANIC_INGOT.get()),
                    Component.literal("infuse output was " + output));
            helper.assertTrue(bottle.is(Items.GLASS_BOTTLE),
                    Component.literal("byproduct was " + bottle + " instead of a glass bottle"));
            helper.assertTrue(furnace.getItem(InfuserFurnaceBlockEntity.SLOT_SMELT_IN).getCount() == 1,
                    Component.literal("the smelt input was not consumed once per operation"));
            helper.assertTrue(furnace.getItem(InfuserFurnaceBlockEntity.SLOT_INFUSE_IN).getCount() == 1,
                    Component.literal("the infuse input was not consumed once per operation"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp infuser furnace infuse with byproduct");
        }
    }

    /** Infested glass + cooked flesh -> two bloody glass, matching {@code addGlassSwap}. */
    private static final class GlassSwapTest extends GameTestInstance {
        GlassSwapTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            InfuserFurnaceBlockEntity furnace = placeFurnace(helper);
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_SMELT_IN,
                    new ItemStack(ModBlocks.INFESTED_GLASS.get().asItem(), 2));
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_INFUSE_IN,
                    new ItemStack(ModItems.COOKED_FLESH.get(), 2));
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));

            runTicks(helper, furnace, COOK_TICKS);

            ItemStack output = furnace.getItem(InfuserFurnaceBlockEntity.SLOT_INFUSE_OUT);
            helper.assertTrue(output.is(ModBlocks.BLOODY_GLASS.get().asItem()),
                    Component.literal("glass swap output was " + output));
            helper.assertTrue(output.getCount() == 2,
                    Component.literal("glass swap produced " + output.getCount() + " instead of 2"));
            helper.assertTrue(furnace.getItem(InfuserFurnaceBlockEntity.SLOT_BOTTLE_OUT).isEmpty(),
                    Component.literal("a glass swap must not leave a byproduct"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp infuser furnace glass swap yields two");
        }
    }

    /** The original also runs vanilla smelting from the smelt slot into the smelt output. */
    private static final class VanillaSmeltingTest extends GameTestInstance {
        VanillaSmeltingTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            InfuserFurnaceBlockEntity furnace = placeFurnace(helper);
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_SMELT_IN, new ItemStack(Items.RAW_IRON, 2));
            furnace.setItem(InfuserFurnaceBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));

            runTicks(helper, furnace, COOK_TICKS);

            ItemStack smelted = furnace.getItem(InfuserFurnaceBlockEntity.SLOT_SMELT_OUT);
            helper.assertTrue(smelted.is(Items.IRON_INGOT),
                    Component.literal("vanilla smelting output was " + smelted));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp infuser furnace vanilla smelting");
        }
    }
}
