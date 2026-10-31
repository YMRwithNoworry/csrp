package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.block.entity.InfuserFurnaceBlockEntity;
import alku.csrp.block.ParasiteSaplingBlock;
import alku.csrp.item.InfestedBonemealItem;
import alku.csrp.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 原件 1.10.9 InfestedBonemeal 球形方块转换的运行时验证。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class InfestedBonemealGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 20;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    private InfestedBonemealGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "infested_bonemeal"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "infested_bonemeal/sphere_and_guards"),
                new SphereAndGuardsTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class SphereAndGuardsTest extends GameTestInstance {
        SphereAndGuardsTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            BlockPos edge = ORIGIN.offset(3, 0, 0);
            BlockPos outside = ORIGIN.offset(3, 1, 0);
            BlockPos scheduled = ORIGIN.offset(0, 0, 1);
            BlockPos water = ORIGIN.offset(0, 1, 0);
            BlockPos blockEntity = ORIGIN.offset(0, 0, -1);
            BlockPos coalOre = ORIGIN.offset(0, 0, 2);
            BlockPos copperOre = ORIGIN.offset(1, 0, 2);
            BlockPos clay = ORIGIN.offset(2, 0, 1);
            BlockPos planks = ORIGIN.offset(2, 0, 2);

            helper.setBlock(ORIGIN, Blocks.STONE);
            helper.setBlock(edge, Blocks.DIRT);
            helper.setBlock(outside, Blocks.CLAY);
            helper.setBlock(scheduled, Blocks.FARMLAND);
            helper.setBlock(coalOre, Blocks.COAL_ORE);
            helper.setBlock(copperOre, Blocks.COPPER_ORE);
            helper.setBlock(clay, Blocks.CLAY);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            helper.getLevel().scheduleTick(helper.absolutePos(scheduled), Blocks.FARMLAND, 20);
            helper.setBlock(water, Blocks.WATER);
            helper.setBlock(blockEntity, ModBlocks.INFUSER_FURNACE.get());

            boolean changed = InfestedBonemealItem.boneMealEffect(
                    helper.getLevel(), helper.absolutePos(ORIGIN), Direction.UP);

            helper.assertTrue(changed, Component.literal("the sphere did not report any converted block"));
            helper.assertTrue(helper.getBlockState(ORIGIN).is(ModBlocks.INFESTED_RUBBLE.get()),
                    Component.literal("stone at the clicked block was not infested"));
            helper.assertTrue(helper.getBlockState(edge).is(ModBlocks.INFESTED_STAIN.get()),
                    Component.literal("a block exactly three blocks away was outside the sphere"));
            helper.assertTrue(helper.getBlockState(outside).is(Blocks.CLAY),
                    Component.literal("a block outside radius squared 9 was converted"));
            helper.assertTrue(helper.getBlockState(coalOre).is(ModBlocks.INFESTED_COAL_ORE.get()),
                    Component.literal("coal ore did not keep its ore-specific infection mapping"));
            helper.assertTrue(helper.getBlockState(copperOre).is(ModBlocks.INFESTED_ORE.get()),
                    Component.literal("an unclassified ore did not use the generic infection mapping"));
            helper.assertTrue(helper.getBlockState(clay).is(ModBlocks.INFESTED_SAND.get()),
                    Component.literal("clay did not use the original sand mapping"));
            helper.assertTrue(helper.getBlockState(planks).is(ModBlocks.INFESTED_PLANKS.get()),
                    Component.literal("wooden planks did not use the plank infection mapping"));
            helper.assertTrue(helper.getBlockState(scheduled).is(Blocks.FARMLAND),
                    Component.literal("a block with a scheduled tick was converted"));
            helper.assertTrue(helper.getBlockState(water).is(Blocks.WATER),
                    Component.literal("a liquid block was converted"));
            helper.assertTrue(helper.getBlockEntity(blockEntity, InfuserFurnaceBlockEntity.class) != null,
                    Component.literal("the block entity in the sphere was removed"));

            BlockPos saplingPos = ORIGIN.offset(0, 0, 4);
            ParasiteSaplingBlock sapling = (ParasiteSaplingBlock) ModBlocks.legacyBlock("parasitesapling").get();
            helper.setBlock(saplingPos, sapling.defaultBlockState()
                    .setValue(ParasiteSaplingBlock.VARIANT, ParasiteSaplingBlock.Variant.TREETHIN));
            boolean advanced = sapling.tryGrowWithInfestedBonemeal(helper.getLevel(), helper.absolutePos(saplingPos),
                    helper.getBlockState(saplingPos), helper.getLevel().getRandom());
            helper.assertTrue(advanced
                            && helper.getBlockState(saplingPos).getValue(ParasiteSaplingBlock.STAGE) == 1,
                    Component.literal("infested bonemeal did not advance a stage-zero parasite sapling"));
            helper.assertFalse(sapling.tryGrowWithInfestedBonemeal(helper.getLevel(), helper.absolutePos(saplingPos),
                            helper.getBlockState(saplingPos), helper.getLevel().getRandom()),
                    Component.literal("a stage-one thin sapling incorrectly grew from infested bonemeal"));

            BlockPos failedTreePos = ORIGIN.offset(1, 0, 4);
            helper.setBlock(failedTreePos.below(), Blocks.DIRT);
            helper.setBlock(failedTreePos.above(), Blocks.STONE);
            helper.setBlock(failedTreePos, sapling.defaultBlockState()
                    .setValue(ParasiteSaplingBlock.VARIANT, ParasiteSaplingBlock.Variant.CONSUMED)
                    .setValue(ParasiteSaplingBlock.STAGE, 1));
            boolean generated = sapling.tryGrowWithInfestedBonemeal(helper.getLevel(), helper.absolutePos(failedTreePos),
                    helper.getBlockState(failedTreePos), helper.getLevel().getRandom());
            helper.assertFalse(generated,
                    Component.literal("a stage-one sapling generated without ground support"));
            helper.assertTrue(helper.getBlockState(failedTreePos).is(sapling)
                            && helper.getBlockState(failedTreePos).getValue(ParasiteSaplingBlock.VARIANT)
                                    == ParasiteSaplingBlock.Variant.CONSUMED
                            && helper.getBlockState(failedTreePos).getValue(ParasiteSaplingBlock.STAGE) == 1,
                    Component.literal("failed tree generation did not restore the original sapling state"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp infested bonemeal sphere and guards");
        }
    }
}
