package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.block.SrpCoreBlock;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.SrpCoreSystems;
import alku.csrp.world.gen.WorldGenParasiteNodeCore;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 原版寄生节点核心结构生成入口的运行时验证。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class NodeCoreGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 20;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final BlockPos CORE = new BlockPos(1, 8, 1);

    private NodeCoreGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "node_core_generation"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "node_core_generation/stage_one_structure"),
                new StageOneStructureTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class StageOneStructureTest extends GameTestInstance {
        StageOneStructureTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            BlockPos core = helper.absolutePos(CORE);
            boolean placed = SrpCoreSystems.placeNode(helper.getLevel(), core, 1);

            helper.assertTrue(placed, Component.literal("the stage-1 parasite node was not placed"));
            helper.assertTrue(helper.getLevel().getBlockState(core).is(ModBlocks.BIOMEHEART.get())
                            && helper.getLevel().getBlockState(core).getValue(SrpCoreBlock.ACTIVE) == 1,
                    Component.literal("the generated biome heart does not retain stage 1"));
            helper.assertTrue(helper.getLevel().getBlockState(core.below(2))
                            .is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get()),
                    Component.literal("stage-1 node pedestal was not generated below the heart"));
            helper.assertTrue(helper.getLevel().getBlockState(core.above(2))
                            .is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get()),
                    Component.literal("stage-1 node crown was not generated above the heart"));

            BlockPos northOne = core.north();
            BlockPos northTwo = northOne.north();
            BlockPos northThree = northTwo.north();
            helper.assertTrue(helper.getLevel().getBlockState(northOne).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get())
                            && helper.getLevel().getBlockState(northTwo).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get())
                            && helper.getLevel().getBlockState(northThree).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get()),
                    Component.literal("stage-1 north arm did not stop at its original three segments"));
            helper.assertTrue(helper.getLevel().getBlockState(northOne.east()).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get())
                            && helper.getLevel().getBlockState(northOne.west()).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get())
                            && helper.getLevel().getBlockState(northTwo.east()).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get())
                            && helper.getLevel().getBlockState(northTwo.west()).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get()),
                    Component.literal("stage-1 north arm did not retain its two original side-branch rows"));
            helper.assertTrue(!helper.getLevel().getBlockState(northThree.east()).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get())
                            && !helper.getLevel().getBlockState(northThree.west()).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get())
                            && !helper.getLevel().getBlockState(northThree.north()).is(ModBlocks.PARASITERUBBLEDENSE_BIOME.get()),
                    Component.literal("stage-1 north arm contains an extra side branch or segment"));

            BlockPos raisedCore = core.above();
            new WorldGenParasiteNodeCore(2, 1).generate(helper.getLevel(), RandomSource.create(2L), raisedCore);
            helper.assertTrue(helper.getLevel().getBlockState(raisedCore).is(ModBlocks.BIOMEHEART.get())
                            && helper.getLevel().getBlockState(raisedCore).getValue(SrpCoreBlock.ACTIVE) == 2,
                    Component.literal("stage-2 transition did not generate its elevated heart"));
            helper.assertTrue(helper.getLevel().getBlockState(core.above(3)).is(ModBlocks.DEAD_BLOOD.get()),
                    Component.literal("stage-2 transition did not generate the original blood structure"));

            new WorldGenParasiteNodeCore(3, 1).generate(helper.getLevel(), RandomSource.create(3L), raisedCore);
            helper.assertTrue(helper.getLevel().getBlockState(raisedCore).is(ModBlocks.BIOMEHEART.get())
                            && helper.getLevel().getBlockState(raisedCore).getValue(SrpCoreBlock.ACTIVE) == 3,
                    Component.literal("stage-3 transition did not update its elevated heart"));

            helper.getLevel().removeBlock(raisedCore, false);
            helper.assertTrue(SrpCoreSystems.removeNode(helper.getLevel(), core),
                    Component.literal("the generated node record was not removable after the test"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp stage-1 parasite node core structure");
        }
    }
}
