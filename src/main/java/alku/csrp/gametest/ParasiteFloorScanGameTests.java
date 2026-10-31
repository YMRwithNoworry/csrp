package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.gen.ParasiteFloorScan;
import alku.csrp.world.gen.ParasiteGenContext;
import alku.csrp.world.gen.WorldGenParasiteMouth;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Runtime checks for the original floor scan and its world-generation callers. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteFloorScanGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private ParasiteFloorScanGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "parasite_floor_scan"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY_STRUCTURE, 20, 1, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "parasite_floor_scan/search_bounds"),
                new SearchBoundsTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "parasite_floor_scan/second_mouth"),
                new SecondMouthTest(data));
    }

    private static final class SearchBoundsTest extends GameTestInstance {
        SearchBoundsTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            BlockPos airStart = helper.absolutePos(new BlockPos(0, 15, 0));
            level.setBlock(airStart.below(3), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(ParasiteGenContext.floor(level, airStart, 2) == null,
                    "downward search exceeded its position limit");
            helper.assertTrue(airStart.below(2).equals(ParasiteGenContext.floor(level, airStart, 3)),
                    "downward search did not return air above the floor at the boundary");
            helper.assertTrue(airStart.below(2).equals(ParasiteFloorScan.getFloor(level, airStart, 3)),
                    "floor-scan compatibility entry point changed the result");
            helper.assertTrue(ParasiteGenContext.floor(level, airStart.below(2), 0) == null,
                    "zero-length search unexpectedly inspected a position");
            helper.assertTrue(airStart.below(2).equals(ParasiteGenContext.floor(level, airStart.below(2), 1)),
                    "one-position search did not find an adjacent floor");

            BlockPos solidStart = airStart.east(4);
            for (int y = 0; y < 3; y++) {
                level.setBlock(solidStart.above(y), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            helper.assertTrue(ParasiteGenContext.floor(level, solidStart, 3) == null,
                    "upward search exceeded its position limit");
            helper.assertTrue(solidStart.above(3).equals(ParasiteGenContext.floor(level, solidStart, 4)),
                    "solid starting position did not search upward to air");

            BlockPos waterStart = airStart.east(8);
            level.setBlock(waterStart, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(waterStart.above(), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(waterStart.above(2).equals(ParasiteGenContext.floor(level, waterStart, 3)),
                    "water starting position did not search upward to air");
            helper.assertTrue(ParasiteGenContext.floor(level, airStart.east(12), 5) == null,
                    "floor scan found a floor in an empty column");
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Floor scan direction, returned position and bounds");
        }
    }

    private static final class SecondMouthTest extends GameTestInstance {
        SecondMouthTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            BlockPos first = helper.absolutePos(new BlockPos(0, 60, 0));
            long seed = 1234L;
            RandomSource expectedRandom = RandomSource.create(seed);
            int offset = expectedRandom.nextInt(7) + 1;
            if (expectedRandom.nextBoolean()) offset *= -1;
            int x = first.getX() + offset;
            int z = first.getZ();
            if (x == 7) {
                int second = expectedRandom.nextInt(7) + 1;
                if (expectedRandom.nextBoolean()) second *= -1;
                z += second;
            } else {
                z += expectedRandom.nextBoolean() ? -7 : 7;
            }
            BlockPos second = new BlockPos(x, first.getY(), z);
            level.setBlock(first.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(second.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);

            WorldGenParasiteMouth.generate(level, RandomSource.create(seed), first);
            helper.assertTrue(level.getBlockState(first.north()).is(ModBlocks.PARASITE_MOUTH.get()),
                    "first mouth did not generate: block=" + level.getBlockState(first.north())
                            + ", origin=" + first + ", second=" + second
                            + ", loaded=" + level.isLoaded(first.north()));
            helper.assertTrue(level.getBlockState(second.north()).is(ModBlocks.PARASITE_MOUTH.get()),
                    "second mouth did not generate above its floor: block=" + level.getBlockState(second.north())
                            + ", origin=" + first + ", second=" + second
                            + ", loaded=" + level.isLoaded(second.north()));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Second parasite mouth generates above the detected floor");
        }
    }
}
