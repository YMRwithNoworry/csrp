package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.event.ParasiteLootEvents;
import alku.csrp.registry.ModItems;
import alku.csrp.world.ParasiteLootTables;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Runtime test for the config-driven parasite drop tables ported from 1.10.9
 * {@code SRPEventHandlerBus#setLoot} / {@code #loot}.
 *
 * <p>The 1.20.1 sibling port's data-driven loot tables (which this project originally inherited)
 * dropped different items than the 1.10.9 configs; {@link ParasiteLootEvents} now reproduces the
 * original roll over {@link ParasiteLootTables}. This test exercises the real roll, so a wrong
 * chance/count/pool transcription fails here.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class LootTableGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private LootTableGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "parasite_loot"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "parasite_loot/legacy_roll"),
                new LegacyRollTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class LegacyRollTest extends GameTestInstance {
        LegacyRollTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            // rupter -> mudoLoot: lurecomponent1;70;2;true. Only lurecomponent1 may drop, 1..2 of it.
            List<ParasiteLootTables.Entry> rupter = ParasiteLootTables.forEntity("rupter");
            helper.assertTrue(rupter != null && rupter.size() == 1,
                    Component.literal("rupter should have exactly one loot entry"));
            for (int seed = 0; seed < 300; seed++) {
                for (ItemStack stack : ParasiteLootEvents.roll(rupter, new Random(seed))) {
                    helper.assertTrue(stack.is(ModItems.LURECOMPONENT1.get()),
                            Component.literal("rupter dropped " + stack));
                    helper.assertTrue(stack.getCount() >= 1 && stack.getCount() <= 2,
                            Component.literal("rupter drop count out of range: " + stack.getCount()));
                }
            }

            // sim_human -> infhumanLoot: assimilated_flesh;80;1;false pooled + lurecomponent2;10;1;true.
            // Both outcomes must be reachable, and each stack must be a single item.
            List<ParasiteLootTables.Entry> human = ParasiteLootTables.forEntity("sim_human");
            helper.assertTrue(human != null && human.size() == 2,
                    Component.literal("sim_human should have two loot entries"));
            boolean sawFlesh = false;
            boolean sawLure = false;
            for (int seed = 0; seed < 600; seed++) {
                for (ItemStack stack : ParasiteLootEvents.roll(human, new Random(seed))) {
                    helper.assertTrue(stack.getCount() == 1,
                            Component.literal("sim_human drop count should be one: " + stack.getCount()));
                    if (stack.is(ModItems.ASSIMILATED_FLESH.get())) sawFlesh = true;
                    if (stack.is(ModItems.LURECOMPONENT2.get())) sawLure = true;
                }
            }
            helper.assertTrue(sawFlesh, Component.literal("sim_human never dropped assimilated_flesh"));
            helper.assertTrue(sawLure, Component.literal("sim_human never dropped lurecomponent2"));

            // ada_burrower resolves to shycoLoot through getParasiteIDRegister() == 57. Its
            // ada_longarms_drop entry is registered, but the original's ada_burrower_drop string is not.
            List<ParasiteLootTables.Entry> burrower = ParasiteLootTables.forEntity("ada_burrower");
            helper.assertTrue(burrower != null && burrower.size() == 3,
                    Component.literal("ada_burrower should inherit the three shycoLoot entries"));
            boolean sawLongarmsDrop = false;
            for (int seed = 0; seed < 600; seed++) {
                for (ItemStack stack : ParasiteLootEvents.roll(burrower, new Random(seed))) {
                    // ada_burrower_drop is intentionally unregistered in this port, so it must never
                    // resolve; the original's Item.func_111206_d would have returned null for it too.
                    helper.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ITEM
                                    .getKey(stack.getItem()).getPath().equals("ada_burrower_drop"),
                            Component.literal("unregistered ada_burrower_drop was dropped"));
                    if (stack.is(ModItems.ADA_LONGARMS_DROP.get())) sawLongarmsDrop = true;
                }
            }
            helper.assertTrue(sawLongarmsDrop,
                    Component.literal("ada_burrower never dropped its registered ada_longarms_drop"));

            // pri_burrower (getParasiteIDRegister() == 318) has no case in the original switch.
            helper.assertTrue(ParasiteLootTables.forEntity("pri_burrower") == null,
                    Component.literal("pri_burrower should have no config loot table"));

            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected net.minecraft.network.chat.MutableComponent typeDescription() {
            return Component.literal("csrp legacy parasite loot roll");
        }
    }
}
