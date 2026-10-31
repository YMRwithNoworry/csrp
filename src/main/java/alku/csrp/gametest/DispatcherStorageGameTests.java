package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.config.MobsConfig;
import alku.csrp.entity.DispatcherStorageCooldown;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Server-side checks for Dispatcher storage conversions and persistence. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DispatcherStorageGameTests {
    private static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");
    private static final BlockPos ORIGIN = new BlockPos(0, 150, 0);

    private DispatcherStorageGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_storage"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY, 30, 1, true, Rotation.NONE);
        event.registerTest(id("stage_and_ground"), new StageAndGroundTest(data));
        event.registerTest(id("aggregation"), new AggregationTest(data));
        event.registerTest(id("config"), new ConfigTest(data));
        event.registerTest(id("persistence"), new PersistenceTest(data));
        event.registerTest(id("scan_and_no_ai"), new ScanTest(data));
        event.registerTest(id("movement_and_damage_cooldown"), new CooldownTest(data));
        event.registerTest(id("candidate_categories"), new CategoryTest(data));
        event.registerTest(id("legacy_storage_cadence"), new StorageCadenceTest(data));
        event.registerTest(id("server_ai_cadence"), new ServerAiCadenceTest(data));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_storage/" + name);
    }

    private static Object invoke(NexusParasiteEntity entity, String name, Class<?>[] types, Object... args) {
        try {
            Method method = NexusParasiteEntity.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(entity, args);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(name, exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(name, exception);
        }
    }

    private static int counter(NexusParasiteEntity entity, String name) {
        try {
            Field field = NexusParasiteEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(entity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(name, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> inventory(NexusParasiteEntity entity) {
        try {
            Field field = NexusParasiteEntity.class.getDeclaredField("storedParasiteIds");
            field.setAccessible(true);
            return (List<String>) field.get(entity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean store(NexusParasiteEntity dispatcher, Mob candidate) {
        return (boolean) invoke(dispatcher, "storeDispatcherParasite", new Class<?>[]{Mob.class}, candidate);
    }

    private static String take(NexusParasiteEntity dispatcher, int index) {
        return (String) invoke(dispatcher, "takeStoredParasite", new Class<?>[]{int.class}, index);
    }

    private static CompoundTag save(NexusParasiteEntity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final List<Entity> entities = new ArrayList<>();
        private final BlockState[] oldFloor = new BlockState[3];
        private final BlockPos floor;

        Scene(GameTestHelper helper) {
            this.helper = helper;
            floor = helper.absolutePos(ORIGIN);
            for (int depth = 1; depth <= 3; depth++) {
                BlockPos pos = floor.below(depth);
                oldFloor[depth - 1] = helper.getLevel().getBlockState(pos);
                helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            }
        }

        <T extends Mob> T spawn(EntityType<T> type) {
            T entity = helper.spawn(type, ORIGIN);
            entity.setNoAi(true);
            entity.setNoGravity(true);
            entities.add(entity);
            return entity;
        }

        NexusParasiteEntity dispatcher(EntityType<NexusParasiteEntity> type) {
            return spawn(type);
        }

        @Override
        public void close() {
            for (Entity entity : entities) entity.discard();
            for (int depth = 1; depth <= 3; depth++) {
                helper.getLevel().setBlockAndUpdate(floor.below(depth), oldFloor[depth - 1]);
            }
        }
    }

    private abstract static class Test extends GameTestInstance {
        Test(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Dispatcher storage");
        }
    }

    private static final class StageAndGroundTest extends Test {
        StageAndGroundTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                List<EntityType<NexusParasiteEntity>> stages = List.of(ModEntities.DISPATCHER_SI.get(),
                        ModEntities.DISPATCHER_SII.get(), ModEntities.DISPATCHER_SIII.get(), ModEntities.DISPATCHER_SIV.get());
                for (int stage = 1; stage <= 4; stage++) {
                    NexusParasiteEntity dispatcher = scene.dispatcher(stages.get(stage - 1));
                    Mob buglin = scene.spawn(ModEntities.BUGLIN.get());
                    helper.assertTrue(store(dispatcher, buglin) && buglin.isRemoved()
                                    && inventory(dispatcher).contains("csrp:rupter"), "Buglin conversion failed at stage " + stage);
                    Mob rupter = scene.spawn(ModEntities.RUPTER.get());
                    helper.assertTrue(store(dispatcher, rupter), "Rupter was not stored");
                    helper.assertTrue(inventory(dispatcher).contains(stage >= 3 ? "csrp:mangler" : "csrp:rupter"),
                            "Rupter conversion did not respect stage " + stage);
                    Mob worker = scene.spawn(ModEntities.WORKER.get());
                    helper.assertTrue(!store(dispatcher, worker) && !worker.isRemoved(), "Worker was stored");
                    dispatcher.discard();
                }
                NexusParasiteEntity dispatcher = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                Mob candidate = scene.spawn(ModEntities.BUGLIN.get());
                helper.getLevel().setBlockAndUpdate(scene.floor.below(2), Blocks.AIR.defaultBlockState());
                helper.assertTrue(!store(dispatcher, candidate) && !candidate.isRemoved(), "Air did not block storage");
                helper.getLevel().setBlockAndUpdate(scene.floor.below(2), Blocks.BEDROCK.defaultBlockState());
                helper.assertTrue(!store(dispatcher, candidate), "Unbreakable ground did not block storage");
                helper.getLevel().setBlockAndUpdate(scene.floor.below(2), Blocks.OBSIDIAN.defaultBlockState());
                helper.assertTrue(!store(dispatcher, candidate), "Hard ground did not block storage");
                helper.getLevel().setBlockAndUpdate(scene.floor.below(1), Blocks.IRON_BLOCK.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(scene.floor.below(2), Blocks.DEEPSLATE.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(scene.floor.below(3), Blocks.BRICKS.defaultBlockState());
                helper.assertTrue(!store(dispatcher, candidate), "Ground hardness exactly 10 did not block storage");
                for (int depth = 1; depth <= 3; depth++) {
                    helper.getLevel().setBlockAndUpdate(scene.floor.below(depth), Blocks.DIRT.defaultBlockState());
                }
                helper.assertTrue(store(dispatcher, candidate), "Soft ground did not allow storage");
                helper.succeed();
            }
        }
    }

    private static final class AggregationTest extends Test {
        AggregationTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity first = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                for (int i = 0; i < 4; i++) store(first, scene.spawn(ModEntities.SIM_COW.get()));
                helper.assertTrue(inventory(first).size() == 4 && counter(first, "dispatcherInfected") == 0,
                        "Stage I unexpectedly converted infected parasites");
                store(first, scene.spawn(ModEntities.MOVINGFLESH.get()));
                helper.assertTrue(counter(first, "dispatcherFlesh") == 1, "First flesh was not counted");
                store(first, scene.spawn(ModEntities.MOVINGFLESH.get()));
                helper.assertTrue(counter(first, "dispatcherFlesh") == 0 && inventory(first).size() == 5
                                && inventory(first).getLast().startsWith("csrp:pri_"), "Two flesh did not form a primitive");

                NexusParasiteEntity second = scene.dispatcher(ModEntities.DISPATCHER_SII.get());
                for (int i = 0; i < 4; i++) store(second, scene.spawn(ModEntities.SIM_COW.get()));
                helper.assertTrue(counter(second, "dispatcherInfected") == 0 && inventory(second).size() == 1
                                && inventory(second).getFirst().startsWith("csrp:pri_"), "Four infected did not form a primitive");
                inventory(second).clear();
                store(second, scene.spawn(ModEntities.MAR_COW.get()));
                for (int i = 0; i < 3; i++) store(second, scene.spawn(ModEntities.SIM_COW.get()));
                helper.assertTrue(counter(second, "dispatcherInfected") == 0
                                && inventory(second).equals(List.of("csrp:mar_cow")),
                        "Marauderized infected did not count while remaining outside the sim_ removal loop");
                inventory(second).clear();
                for (int i = 0; i < 6; i++) store(second, scene.spawn(ModEntities.INCOMPLETEFORM_SMALL.get()));
                helper.assertTrue(counter(second, "dispatcherCrude") == 0
                                && inventory(second).equals(List.of("csrp:heed")), "Six small crude forms did not form Heed");
                inventory(second).clear();
                for (int i = 0; i < 3; i++) store(second, scene.spawn(ModEntities.INCOMPLETEFORM_MEDIUM.get()));
                helper.assertTrue(counter(second, "dispatcherCrude") == 0 && inventory(second).isEmpty(),
                        "Three medium crude forms must consume three entries without creating Heed");
                helper.succeed();
            }
        }
    }

    private static final class ConfigTest extends Test {
        private static final String[] SETTINGS = {"DISPATCHER_PRIMITIVE_YELLOWEYE", "DISPATCHER_PRIMITIVE_SUMMONER",
                "DISPATCHER_PRIMITIVE_BOLSTER", "DISPATCHER_PRIMITIVE_LONGARMS", "DISPATCHER_PRIMITIVE_ARACHNIDA",
                "DISPATCHER_PRIMITIVE_REEKER", "DISPATCHER_PRIMITIVE_MANDUCATER"};

        ConfigTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            List<ModConfigSpec.BooleanValue> settings = new ArrayList<>();
            List<Boolean> previous = new ArrayList<>();
            try (Scene scene = new Scene(helper)) {
                for (String name : SETTINGS) {
                    Field field = MobsConfig.class.getDeclaredField(name);
                    field.setAccessible(true);
                    ModConfigSpec.BooleanValue setting = (ModConfigSpec.BooleanValue) field.get(null);
                    settings.add(setting);
                    previous.add(setting.get());
                    setting.set(false);
                }
                NexusParasiteEntity dispatcher = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                store(dispatcher, scene.spawn(ModEntities.MOVINGFLESH.get()));
                store(dispatcher, scene.spawn(ModEntities.MOVINGFLESH.get()));
                helper.assertTrue(inventory(dispatcher).isEmpty() && counter(dispatcher, "dispatcherFlesh") == 0,
                        "Disabled primitives still created inventory or retained flesh");
                settings.get(5).set(true);
                for (int i = 0; i < 20; i++) {
                    invoke(dispatcher, "addDispatcherPrimitive", new Class<?>[]{});
                }
                helper.assertTrue(inventory(dispatcher).size() == 20
                                && inventory(dispatcher).stream().allMatch("csrp:pri_reeker"::equals),
                        "Primitive selection failed to wrap to the only enabled option");
                helper.succeed();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            } finally {
                for (int i = 0; i < settings.size(); i++) settings.get(i).set(previous.get(i));
            }
        }
    }

    private static final class PersistenceTest extends Test {
        PersistenceTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity source = scene.dispatcher(ModEntities.DISPATCHER_SIII.get());
                store(source, scene.spawn(ModEntities.INCOMPLETEFORM_MEDIUM.get()));
                store(source, scene.spawn(ModEntities.SIM_COW.get()));
                store(source, scene.spawn(ModEntities.MOVINGFLESH.get()));
                store(source, scene.spawn(ModEntities.MANGLER.get()));
                CompoundTag saved = save(source);
                NexusParasiteEntity loaded = scene.dispatcher(ModEntities.DISPATCHER_SIII.get());
                loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved));
                helper.assertTrue(inventory(loaded).equals(inventory(source)), "Inventory was not preserved");
                helper.assertTrue(counter(loaded, "dispatcherCrude") == 2 && counter(loaded, "dispatcherInfected") == 1
                                && counter(loaded, "dispatcherFlesh") == 1 && counter(loaded, "dispatcherMangler") == 0,
                        "Counters did not match the original save semantics");
                helper.assertTrue(take(loaded, inventory(loaded).indexOf("csrp:incompleteform_medium"))
                                .equals("csrp:incompleteform_medium") && counter(loaded, "dispatcherCrude") == 0,
                        "Taking a medium crude form did not decrement by two");
                take(loaded, inventory(loaded).indexOf("csrp:sim_cow"));
                helper.assertTrue(counter(loaded, "dispatcherInfected") == 0, "Infected take did not decrement count");
                loaded.recallParasite("csrp:sim_cow");
                take(loaded, inventory(loaded).indexOf("csrp:sim_cow"));
                helper.assertTrue(counter(loaded, "dispatcherInfected") == -1,
                        "Returning a deployed infected unexpectedly incremented its count");
                for (int i = 0; i < 20; i++) loaded.recallParasite("csrp:rupter");
                helper.assertTrue(inventory(loaded).size() > 12, "Artificial twelve-mob inventory cap remains");

                saved.remove("nexus_dispatcher_crude");
                saved.remove("nexus_dispatcher_infected");
                saved.remove("nexus_dispatcher_flesh");
                NexusParasiteEntity old = scene.dispatcher(ModEntities.DISPATCHER_SIII.get());
                old.load(TagValueInput.create(ProblemReporter.DISCARDING, old.registryAccess(), saved));
                helper.assertTrue(counter(old, "dispatcherCrude") == 2 && counter(old, "dispatcherInfected") == 1,
                        "Old 26.3 inventory counters were not reconstructed");
                helper.succeed();
            }
        }
    }

    private static final class CooldownTest extends Test {
        CooldownTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity dispatcher = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                Mob candidate = scene.spawn(ModEntities.BUGLIN.get());
                candidate.tickCount = 101;
                candidate.setOnGround(true);
                helper.assertTrue(DispatcherStorageCooldown.canStore(candidate), "Fresh parasite had a storage cooldown");
                candidate.setNoAi(false);
                candidate.setDeltaMovement(0.25D, 0.0D, 0.0D);
                helper.getLevel().tickNonPassenger(candidate);
                candidate.setNoAi(true);
                candidate.setDeltaMovement(Vec3.ZERO);
                candidate.setOnGround(true);
                helper.assertTrue(!DispatcherStorageCooldown.canStore(candidate), "Movement did not start the storage cooldown");
                invoke(dispatcher, "storeNearbyParasite", new Class<?>[]{});
                helper.assertTrue(!candidate.isRemoved(), "A recently moved parasite was stored");
                for (int i = 0; i < 53; i++) helper.getLevel().tickNonPassenger(candidate);
                helper.assertTrue(!DispatcherStorageCooldown.canStore(candidate), "Movement cooldown ended too early");
                helper.getLevel().tickNonPassenger(candidate);
                helper.assertTrue(DispatcherStorageCooldown.canStore(candidate), "Movement cooldown did not expire");

                candidate.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 0.25F);
                helper.assertTrue(!DispatcherStorageCooldown.canStore(candidate), "Incoming damage did not reset cooldown");
                for (int i = 0; i < 54; i++) helper.getLevel().tickNonPassenger(candidate);
                helper.assertTrue(!DispatcherStorageCooldown.canStore(candidate), "Damage cooldown ended too early");
                helper.getLevel().tickNonPassenger(candidate);
                helper.assertTrue(DispatcherStorageCooldown.canStore(candidate), "Damage cooldown did not expire");
                Mob target = scene.spawn(EntityTypes.SHEEP);
                target.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(candidate), 0.25F);
                helper.assertTrue(!DispatcherStorageCooldown.canStore(candidate), "Melee hit did not reset attacker cooldown");
                helper.succeed();
            }
        }
    }

    private static void prepareCandidate(Mob candidate) {
        candidate.tickCount = 101;
        candidate.setOnGround(true);
    }

    private static boolean isStorable(NexusParasiteEntity dispatcher, Mob candidate) {
        return (boolean) invoke(dispatcher, "isStorableDispatcherParasite", new Class<?>[]{Mob.class}, candidate);
    }

    private static void tickStorageGoal(NexusParasiteEntity dispatcher, int ticks) {
        for (int i = 0; i < ticks; i++) {
            invoke(dispatcher, "tickDispatcherStorageGoal", new Class<?>[]{});
        }
    }

    private static final class CategoryTest extends Test {
        CategoryTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity dispatcher = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                List<Mob> excluded = new ArrayList<>();
                for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(
                        ModEntities.CARRIER_WORM.get(), ModEntities.PRI_BURROWER.get(), ModEntities.PRI_TOZOON.get(),
                        ModEntities.ADA_BURROWER.get(), ModEntities.ADA_TOZOON.get(), ModEntities.BECKON_SI.get(),
                        ModEntities.ROOTER_SI.get(), ModEntities.ROOTERBALL.get(), ModEntities.DISPATCHERTEN.get(),
                        ModEntities.KYPHOSIS.get(), ModEntities.SEIZER.get(), ModEntities.SENTRY.get(),
                        ModEntities.WORM.get(), ModEntities.BOMBER_HEAVY.get(), ModEntities.BOGLE.get(),
                        ModEntities.CARRIER_COLONY.get(), ModEntities.HAUNTER.get(), ModEntities.WRAITH.get(),
                        ModEntities.SUCCOR.get(), ModEntities.ARCHITECT.get(), ModEntities.KIRIN.get(),
                        ModEntities.DRACONITE.get(), ModEntities.MARAUDER_TENDRIL.get(), EntityTypes.SHEEP)) {
                    Mob candidate = scene.spawn(type);
                    prepareCandidate(candidate);
                    helper.assertTrue(!isStorable(dispatcher, candidate), "Excluded storage candidate: " + type);
                    excluded.add(candidate);
                }
                List<Mob> allowed = new ArrayList<>();
                for (EntityType<? extends Mob> type : List.<EntityType<? extends Mob>>of(
                        ModEntities.PRI_BOLSTER.get(), ModEntities.ADA_BOLSTER.get(),
                        ModEntities.ANC_DREADNAUT.get(), ModEntities.ANC_OVERLORD.get(),
                        ModEntities.ANC_DREADNAUT_TEN.get(), ModEntities.ANC_POD.get(),
                        ModEntities.ABO_HEAD.get(), ModEntities.ABO_BODIES.get(), ModEntities.FER_COW.get(),
                        ModEntities.TENDRIL.get(), ModEntities.BIOMASS.get())) {
                    Mob candidate = scene.spawn(type);
                    prepareCandidate(candidate);
                    helper.assertTrue(isStorable(dispatcher, candidate), "Allowed storage candidate was rejected: " + type);
                    allowed.add(candidate);
                }
                Mob mount = scene.spawn(EntityTypes.SHEEP);
                Mob passenger = scene.spawn(ModEntities.BUGLIN.get());
                prepareCandidate(passenger);
                helper.assertTrue(passenger.startRiding(mount, true, false), "Could not set up riding parasite");
                helper.assertTrue(isStorable(dispatcher, passenger), "Riding incorrectly prevents storage");
                Mob vehicle = scene.spawn(ModEntities.BUGLIN.get());
                Mob rider = scene.spawn(EntityTypes.SHEEP);
                prepareCandidate(vehicle);
                helper.assertTrue(rider.startRiding(vehicle, true, false), "Could not set up parasite vehicle");
                helper.assertTrue(isStorable(dispatcher, vehicle), "Carrying a passenger incorrectly prevents storage");
                invoke(dispatcher, "storeNearbyParasite", new Class<?>[]{});
                for (Mob candidate : allowed) {
                    helper.assertTrue(candidate.isRemoved(), "Allowed candidate was not stored: " + candidate.getType());
                }
                for (Mob candidate : excluded) {
                    helper.assertTrue(!candidate.isRemoved(), "Excluded candidate was stored: " + candidate.getType());
                }
                helper.assertTrue(passenger.isRemoved() && vehicle.isRemoved(), "Riding candidates were not stored");
                helper.succeed();
            }
        }
    }

    private static final class StorageCadenceTest extends Test {
        StorageCadenceTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity dispatcher = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                Mob first = scene.spawn(ModEntities.BUGLIN.get());
                prepareCandidate(first);
                tickStorageGoal(dispatcher, 60);
                helper.assertTrue(!first.isRemoved(), "Initial storage scan ran before AI tick 61");
                tickStorageGoal(dispatcher, 1);
                helper.assertTrue(first.isRemoved(), "Initial storage scan did not run on AI tick 61");
                Mob idle = scene.spawn(ModEntities.BUGLIN.get());
                prepareCandidate(idle);
                tickStorageGoal(dispatcher, 239);
                helper.assertTrue(!idle.isRemoved(), "Idle storage scan ran before the 80-check interval");
                tickStorageGoal(dispatcher, 1);
                helper.assertTrue(idle.isRemoved(), "Idle storage scan did not run after 240 AI ticks");
                dispatcher.discard();

                NexusParasiteEntity combat = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                Mob target = scene.spawn(EntityTypes.SHEEP);
                combat.setTarget(target);
                Mob combatFirst = scene.spawn(ModEntities.BUGLIN.get());
                prepareCandidate(combatFirst);
                tickStorageGoal(combat, 61);
                helper.assertTrue(combatFirst.isRemoved(), "Initial combat storage scan did not run");
                Mob combatNext = scene.spawn(ModEntities.BUGLIN.get());
                prepareCandidate(combatNext);
                tickStorageGoal(combat, 29);
                helper.assertTrue(!combatNext.isRemoved(), "Combat storage scan ran before the 10-check interval");
                tickStorageGoal(combat, 1);
                helper.assertTrue(combatNext.isRemoved(), "Combat storage scan did not run after 30 AI ticks");

                CompoundTag saved = save(combat);
                tickStorageGoal(combat, 10);
                combat.load(TagValueInput.create(ProblemReporter.DISCARDING, combat.registryAccess(), saved));
                helper.assertTrue(counter(combat, "dispatcherStorageChecks") == 20
                                && counter(combat, "dispatcherSelectorTick") == 0,
                        "Transient storage goal state was not reset on reload");
                helper.succeed();
            }
        }
    }

    private static final class ServerAiCadenceTest extends Test {
        ServerAiCadenceTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity dispatcher = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                Mob candidate = scene.spawn(ModEntities.BUGLIN.get());
                candidate.setPos(candidate.getX() + 3.0D, candidate.getY(), candidate.getZ());
                prepareCandidate(candidate);
                for (int i = 0; i < 70; i++) helper.getLevel().tickNonPassenger(dispatcher);
                helper.assertTrue(!candidate.isRemoved() && counter(dispatcher, "dispatcherStorageChecks") == 20,
                        "NoAI advanced the Dispatcher storage goal");
                dispatcher.setNoAi(false);
                for (int i = 0; i < 60; i++) helper.getLevel().tickNonPassenger(dispatcher);
                helper.assertTrue(!candidate.isRemoved(), "Server AI stored the candidate too early");
                helper.getLevel().tickNonPassenger(dispatcher);
                helper.assertTrue(candidate.isRemoved(), "Server AI did not perform the initial storage scan");
                helper.succeed();
            }
        }
    }

    private static final class ScanTest extends Test {
        ScanTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity dispatcher = scene.dispatcher(ModEntities.DISPATCHER_SI.get());
                Mob first = scene.spawn(ModEntities.BUGLIN.get());
                Mob second = scene.spawn(ModEntities.BUGLIN.get());
                Mob young = scene.spawn(ModEntities.BUGLIN.get());
                Mob targeted = scene.spawn(ModEntities.BUGLIN.get());
                Mob target = scene.spawn(EntityTypes.SHEEP);
                for (Mob mob : List.of(first, second, young, targeted)) {
                    mob.setOnGround(true);
                    mob.tickCount = 101;
                }
                young.tickCount = 100;
                targeted.setTarget(target);
                dispatcher.tickCount = 39;
                dispatcher.tick();
                helper.assertTrue(!first.isRemoved() && !second.isRemoved(), "NoAI Dispatcher stored parasites");
                invoke(dispatcher, "storeNearbyParasite", new Class<?>[]{});
                helper.assertTrue(first.isRemoved() && second.isRemoved(), "A storage scan did not process all eligible mobs");
                helper.assertTrue(!young.isRemoved() && !targeted.isRemoved(), "Scan stored a young or targeted mob");
                helper.succeed();
            }
        }
    }
}
