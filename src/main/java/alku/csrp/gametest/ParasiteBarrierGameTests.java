package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.block.InfestedBushBlock;
import alku.csrp.block.ParasiteBushBlock;
import alku.csrp.block.entity.ParasiteBarrierBlockEntity;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 屏障范围/持久状态及两类寄生灌木攀爬规则的运行期验证。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteBarrierGameTests {
    private static final String MODID = Csrp.MODID;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final BlockPos BARRIER_POS = new BlockPos(1, 1, 1);
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;

    private ParasiteBarrierGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "parasite_barrier"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "parasite_barrier/radius_and_block_contract"),
                new BarrierContractTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "parasite_barrier/bush_climbing"),
                new BushClimbingTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "parasite_barrier/parasite_ejection"),
                new ParasiteEjectionTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class BarrierContractTest extends GameTestInstance {
        BarrierContractTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            helper.setBlock(BARRIER_POS, ModBlocks.legacyBlock("parasite_barrier").get());
            ParasiteBarrierBlockEntity barrier = helper.getBlockEntity(BARRIER_POS, ParasiteBarrierBlockEntity.class);
            helper.assertTrue(barrier != null, Component.literal("parasite barrier block entity was not registered"));

            BlockPos absolutePos = helper.absolutePos(BARRIER_POS);
            int chunkX = absolutePos.getX() >> 4;
            int chunkZ = absolutePos.getZ() >> 4;
            assertBounds(helper, barrier, chunkX, chunkZ, 1);
            helper.assertTrue(barrier.radiusChunks() == 1, "barrier radius should default to one chunk");

            barrier.setRadius(helper.getLevel(), 10);
            helper.assertTrue(barrier.radiusChunks() == 10, "barrier should accept the maximum radius");
            barrier.setRadius(helper.getLevel(), 100);
            helper.assertTrue(barrier.radiusChunks() == 10, "barrier radius should clamp at ten chunks");
            barrier.setRadius(helper.getLevel(), 0);
            helper.assertTrue(barrier.radiusChunks() == 1, "barrier radius should clamp at one chunk");
            barrier.cycleRadius(helper.getLevel());
            helper.assertTrue(barrier.radiusChunks() == 2, "barrier interaction should advance the radius");
            assertBounds(helper, barrier, chunkX, chunkZ, 2);
            CompoundTag saved = barrier.getUpdateTag(helper.getLevel().registryAccess());
            helper.assertTrue(saved.contains("centerChunkX") && saved.contains("centerChunkZ")
                            && saved.contains("radiusChunks"),
                    "barrier data should use the original lower-camel NBT keys");
            helper.assertTrue(saved.getIntOr("radiusChunks", 0) == 2,
                    "barrier NBT should persist its configured radius");

            BlockState state = helper.getLevel().getBlockState(absolutePos);
            helper.assertTrue(state.getPistonPushReaction() == PushReaction.IMMOVEABLE,
                    "parasite barrier must not move under piston force");
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolutePos, CollisionContext.empty()).isEmpty(),
                    "parasite barrier must have no collision shape");
            helper.succeed();
        }

        private static void assertBounds(GameTestHelper helper, ParasiteBarrierBlockEntity barrier,
                int centerChunkX, int centerChunkZ, int radius) {
            int[] bounds = barrier.currentWorldBounds();
            int minX = (centerChunkX - radius) * 16;
            int maxX = (centerChunkX + radius + 1) * 16 - 1;
            int minZ = (centerChunkZ - radius) * 16;
            int maxZ = (centerChunkZ + radius + 1) * 16 - 1;
            helper.assertTrue(bounds[0] == minX && bounds[1] == maxX
                            && bounds[2] == minZ && bounds[3] == maxZ,
                    "barrier bounds should span the configured chunk radius");
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp parasite barrier radius and block contract");
        }
    }

    private static final class ParasiteEjectionTest extends GameTestInstance {
        ParasiteEjectionTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            helper.setBlock(BARRIER_POS, ModBlocks.legacyBlock("parasite_barrier").get());
            ParasiteBarrierBlockEntity barrier = helper.getBlockEntity(
                    BARRIER_POS, ParasiteBarrierBlockEntity.class);
            Entity parasite = helper.spawn(ModEntities.BUGLIN.get(), BARRIER_POS.above());
            helper.assertTrue(barrier != null, "parasite barrier block entity should be present");
            helper.runAtTickTime(5, () -> {
                int[] bounds = barrier.currentWorldBounds();
                boolean outside = parasite.getX() < bounds[0] || parasite.getX() > bounds[1]
                        || parasite.getZ() < bounds[2] || parasite.getZ() > bounds[3];
                helper.assertTrue(outside, "parasite barrier should eject a parasite beyond its bounds");
                helper.succeed();
            });
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp parasite barrier parasite ejection contract");
        }
    }

    private static final class BushClimbingTest extends GameTestInstance {
        BushClimbingTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            InfestedBushBlock infested = (InfestedBushBlock) ModBlocks.legacyBlock("infestedbush").get();
            BlockState spine = infested.defaultBlockState().setValue(InfestedBushBlock.VARIANT,
                    InfestedBushBlock.Variant.SPINE);
            BlockState vine = infested.defaultBlockState().setValue(InfestedBushBlock.VARIANT,
                    InfestedBushBlock.Variant.VINE);
            helper.assertTrue(infested.isLadder(spine, helper.getLevel(), BlockPos.ZERO, player),
                    "infested spine should be climbable by players");
            helper.assertFalse(infested.isLadder(vine, helper.getLevel(), BlockPos.ZERO, player),
                    "infested vine should not be climbable by players");

            ParasiteBushBlock parasite = (ParasiteBushBlock) ModBlocks.legacyBlock("parasitebush").get();
            BlockState tendril = parasite.defaultBlockState().setValue(ParasiteBushBlock.VARIANT,
                    ParasiteBushBlock.Variant.TENDRIL);
            BlockState bine = parasite.defaultBlockState().setValue(ParasiteBushBlock.VARIANT,
                    ParasiteBushBlock.Variant.BINE);
            BlockState pop = parasite.defaultBlockState().setValue(ParasiteBushBlock.VARIANT,
                    ParasiteBushBlock.Variant.POP);
            helper.assertTrue(parasite.isLadder(tendril, helper.getLevel(), BlockPos.ZERO, player),
                    "parasite tendril should be climbable by players");
            helper.assertTrue(parasite.isLadder(bine, helper.getLevel(), BlockPos.ZERO, player),
                    "parasite bine should be climbable by players");
            helper.assertFalse(parasite.isLadder(pop, helper.getLevel(), BlockPos.ZERO, player),
                    "non-climbing parasite bushes should not be climbable");
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp parasite bush climbing contract");
        }
    }
}
