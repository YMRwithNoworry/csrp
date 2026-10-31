package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.entity.AncientPodEntity;
import alku.csrp.entity.NexusParasiteEntity;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Runtime coverage for the original stage-IV Dispatcher pod attack. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DispatcherPodGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private DispatcherPodGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_pod"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY_STRUCTURE, 20, 1, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_pod/volley_and_target_loss"),
                new VolleyTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_pod/range_and_registration"),
                new RangeTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_pod/server_ai"),
                new ServerAiTest(data));
    }

    private static NexusParasiteEntity dispatcher(GameTestHelper helper, int y) {
        NexusParasiteEntity entity = helper.spawn(ModEntities.DISPATCHER_SIV.get(), new BlockPos(0, y, 0));
        entity.setNoGravity(true);
        entity.setNoAi(true);
        return entity;
    }

    private static Mob target(GameTestHelper helper, NexusParasiteEntity dispatcher) {
        Mob target = helper.spawn(EntityTypes.SHEEP, new BlockPos(0, 5, 0));
        target.setNoGravity(true);
        target.setNoAi(true);
        target.setPos(dispatcher.getX() + 5.0D, dispatcher.getY(), dispatcher.getZ());
        target.setOnGround(true);
        dispatcher.setTarget(target);
        return target;
    }

    private static AABB podArea(GameTestHelper helper, double x, double y, double z) {
        AABB area = new AABB(x - 10.0D, y - 1.0D, z - 10.0D, x + 10.0D, y + 3.0D, z + 10.0D);
        for (int dx : new int[] {-10, 10}) {
            for (int dz : new int[] {-10, 10}) {
                helper.getLevel().getChunkAt(BlockPos.containing(x + dx, y, z + dz));
            }
        }
        return area;
    }

    private static List<AncientPodEntity> pods(GameTestHelper helper, AABB area) {
        return helper.getLevel().getEntitiesOfClass(AncientPodEntity.class, area);
    }

    private static void tickSelector(NexusParasiteEntity dispatcher, int ticks) {
        for (int i = 0; i < ticks; i++) {
            dispatcher.goalSelector.tick();
        }
    }

    private static void clearPods(GameTestHelper helper, AABB area) {
        for (AncientPodEntity pod : pods(helper, area)) pod.discard();
    }

    private static final class VolleyTest extends GameTestInstance {
        VolleyTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity dispatcher = dispatcher(helper, 120);
            Mob target = target(helper, dispatcher);
            double x = target.getX();
            double y = dispatcher.getY() + 25.0D;
            double z = target.getZ();
            AABB area = podArea(helper, x, y, z);
            try {
                tickSelector(dispatcher, 119);
                helper.assertTrue(pods(helper, area).isEmpty(), "pods spawned before the 120-tick windup");
                tickSelector(dispatcher, 1);
                tickSelector(dispatcher, 1);
                target.setPos(x + 35.0D, target.getY(), z + 35.0D);
                dispatcher.setTarget(null);
                tickSelector(dispatcher, 37);
                helper.assertTrue(pods(helper, area).isEmpty(), "the first pod spawned before attack tick 40");
                tickSelector(dispatcher, 1);
                helper.assertTrue(pods(helper, area).size() == 1, "the first pod was not spawned at attack tick 40");
                for (int expected = 2; expected <= 4; expected++) {
                    tickSelector(dispatcher, 19);
                    helper.assertTrue(pods(helper, area).size() == expected - 1, "pod interval was shorter than 20 ticks");
                    tickSelector(dispatcher, 1);
                    helper.assertTrue(pods(helper, area).size() == expected, "pod interval or volley count is incorrect");
                }
                tickSelector(dispatcher, 80);
                helper.assertTrue(pods(helper, area).size() == 4, "volley continued after four pods or target loss");
                for (AncientPodEntity pod : pods(helper, area)) {
                    double dx = pod.getX() - x;
                    double dz = pod.getZ() - z;
                    helper.assertTrue(Math.abs(dx) <= 9.0D && Math.abs(dz) <= 9.0D
                                    && dx == Math.rint(dx) && dz == Math.rint(dz)
                                    && Math.abs(pod.getY() - y) < 0.0001D,
                            "pod did not use the original locked square and height");
                    TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, pod.registryAccess());
                    pod.saveWithoutId(output);
                    CompoundTag saved = output.buildResult();
                    helper.assertTrue(saved.getByteOr("pod_owner", (byte) -1) == 0,
                            "Dispatcher pod incorrectly inherited the Dreadnaut payload");
                    helper.assertTrue(pod.getTarget() == null && pod.getDeltaMovement().lengthSqr() == 0.0D,
                            "Dispatcher pod inherited a target or artificial launch velocity");
                }
                helper.succeed();
            } finally {
                clearPods(helper, area);
                target.discard();
                dispatcher.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Dispatcher pod volley timing and locked position");
        }
    }

    private static final class RangeTest extends GameTestInstance {
        RangeTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            List<EntityType<NexusParasiteEntity>> types = List.of(ModEntities.DISPATCHER_SI.get(),
                    ModEntities.DISPATCHER_SII.get(), ModEntities.DISPATCHER_SIII.get(), ModEntities.DISPATCHER_SIV.get(),
                    ModEntities.ROOTER_SIV.get(), ModEntities.BECKON_SIV.get());
            for (EntityType<NexusParasiteEntity> type : types) {
                NexusParasiteEntity entity = type.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
                helper.assertTrue(entity != null, "could not create Nexus entity");
                List<WrappedGoal> goals = entity.goalSelector.getAvailableGoals().stream()
                        .filter(goal -> goal.getGoal().getClass().getSimpleName().equals("DispatcherPodGoal")).toList();
                helper.assertTrue(goals.size() == (type == ModEntities.DISPATCHER_SIV.get() ? 1 : 0),
                        "pod goal registered on the wrong Nexus stage or family");
                if (!goals.isEmpty()) {
                    helper.assertTrue(goals.getFirst().requiresUpdateEveryTick(), "pod goal does not tick every server tick");
                }
                entity.discard();
            }

            NexusParasiteEntity dispatcher = dispatcher(helper, 170);
            Mob target = target(helper, dispatcher);
            AABB area = podArea(helper, dispatcher.getX() + 5.0D, dispatcher.getY() + 25.0D, dispatcher.getZ());
            try {
                target.setPos(dispatcher.getX() + 50.0D, dispatcher.getY(), dispatcher.getZ());
                tickSelector(dispatcher, 150);
                helper.assertTrue(pods(helper, area).isEmpty(), "a target exactly 50 blocks away started the attack");
                target.setPos(dispatcher.getX() + 5.0D, dispatcher.getY(), dispatcher.getZ());
                tickSelector(dispatcher, 119);
                helper.assertTrue(pods(helper, area).isEmpty(), "out-of-range time counted toward the windup");
                tickSelector(dispatcher, 40);
                helper.assertTrue(pods(helper, area).size() == 1, "valid target did not trigger the pod attack");
                helper.succeed();
            } finally {
                clearPods(helper, area);
                target.discard();
                dispatcher.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Dispatcher pod range and stage registration");
        }
    }

    private static final class ServerAiTest extends GameTestInstance {
        ServerAiTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity dispatcher = dispatcher(helper, 220);
            Mob target = target(helper, dispatcher);
            AABB area = podArea(helper, target.getX(), dispatcher.getY() + 25.0D, target.getZ());
            try {
                dispatcher.targetSelector.removeAllGoals(goal -> true);
                dispatcher.setNoAi(false);
                for (int i = 0; i < 165; i++) {
                    target.setOnGround(true);
                    dispatcher.tick();
                }
                helper.assertTrue(pods(helper, area).size() == 1,
                        "normal server AI did not run the pod goal at its original cadence");
                helper.succeed();
            } finally {
                clearPods(helper, area);
                target.discard();
                dispatcher.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Dispatcher pod goal under normal server AI ticking");
        }
    }
}
