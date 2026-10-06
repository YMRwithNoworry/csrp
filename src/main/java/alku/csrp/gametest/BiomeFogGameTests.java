package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.world.SrpWorldData;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Runtime checks for the node influence query behind the parasite biome fog
 * ({@code SRPWorldData#nearestHeartAge} / {@code nearestHeartType}) and for the configurable node
 * stage thresholds.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class BiomeFogGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private BiomeFogGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "biome_fog"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "biome_fog/node_influence"),
                new NodeInfluenceTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "biome_fog/node_stage_thresholds"),
                new NodeStageTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    /** The effect range of a node really is what decides whether a position is "under influence". */
    private static final class NodeInfluenceTest extends GameTestInstance {
        NodeInfluenceTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            SrpWorldData data = SrpWorldData.get(level);
            BlockPos origin = helper.absolutePos(BlockPos.ZERO);
            int stageOneRange = WorldConfig.nodeRangeEffects(1);

            data.clearNodes();
            helper.assertTrue(data.nearestHeartAge(origin) == -1,
                    Component.literal("an empty node list still reports a heart"));

            data.setNode(origin, WorldConfig.nodeTwoTime() - 1, 2);
            helper.assertTrue(data.nearestHeartAge(origin) == 1,
                    Component.literal("a node below nodeTwoTime is not stage 1"));
            helper.assertTrue(data.nearestHeartType(origin) == 2,
                    Component.literal("the covering node's type is not reported"));
            helper.assertTrue(data.nearestHeartAge(origin.offset(stageOneRange, 0, 0)) == 1,
                    Component.literal("a stage 1 node no longer covers its own effect radius"));
            helper.assertTrue(data.nearestHeartAge(origin.offset(stageOneRange + 1, 0, 0)) == -1,
                    Component.literal("a stage 1 node reaches past its effect radius"));

            data.setNode(origin, WorldConfig.nodeThreeTime(), 3);
            helper.assertTrue(data.nearestHeartAge(origin) == 3,
                    Component.literal("a node at nodeThreeTime is not stage 3"));
            helper.assertTrue(data.nearestHeartType(origin) == 3,
                    Component.literal("the promoted node's type is not reported"));
            helper.assertTrue(data.nearestHeartAge(origin.offset(stageOneRange + 1, 0, 0)) == 3,
                    Component.literal("a stage 3 node does not cover a stage 1 distance"));

            data.clearNodes();
            helper.assertTrue(data.nearestHeartAge(origin) == -1,
                    Component.literal("clearing the nodes left a heart behind"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp biome fog node influence");
        }
    }

    /** {@code convertDayToAgeNode} and the two radius tables, driven through their config values. */
    private static final class NodeStageTest extends GameTestInstance {
        NodeStageTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            int two = WorldConfig.nodeTwoTime();
            int three = WorldConfig.nodeThreeTime();
            helper.assertTrue(SrpWorldData.nodeStageFor(two - 1) == 1,
                    Component.literal("an age below nodeTwoTime is not stage 1"));
            helper.assertTrue(SrpWorldData.nodeStageFor(two) == 2,
                    Component.literal("nodeTwoTime does not promote a node to stage 2"));
            helper.assertTrue(SrpWorldData.nodeStageFor(three - 1) == 2,
                    Component.literal("an age below nodeThreeTime is not stage 2"));
            helper.assertTrue(SrpWorldData.nodeStageFor(three) == 3,
                    Component.literal("nodeThreeTime does not promote a node to stage 3"));

            SrpWorldData data = SrpWorldData.get(helper.getLevel());
            int effect = WorldConfig.nodeRangeEffects(1);
            helper.assertTrue(data.getDistanceEffectByAge(1, false) == effect,
                    Component.literal("the stage 1 effect radius is not returned unsquared"));
            helper.assertTrue(data.getDistanceEffectByAge(1, true) == effect * effect,
                    Component.literal("the stage 1 effect radius is not squared on request"));
            helper.assertTrue(data.getDistanceEffectByAge(4, false) == -1,
                    Component.literal("a stage outside 1..3 must report -1"));
            helper.assertTrue(data.getDistanceSpreadByAge(2, false) == WorldConfig.nodeRangeSpread(2),
                    Component.literal("the stage 2 spread radius changed"));
            helper.assertTrue(WorldConfig.biomeFogDensity() > 0.0F,
                    Component.literal("the biome fog density is not positive"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp biome fog node stage thresholds");
        }
    }
}
