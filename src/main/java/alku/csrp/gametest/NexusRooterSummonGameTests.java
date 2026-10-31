package alku.csrp.gametest;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Server-side checks for the 1.10.9 Nexus Rooter summon path. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class NexusRooterSummonGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final long CANDIDATE_SEED = 28L;

    private NexusRooterSummonGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_rooter_summon"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY_STRUCTURE, 20, 1, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_rooter_summon/snapshot_and_gates"),
                new SnapshotTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_rooter_summon/placement_and_sky"),
                new PlacementTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_rooter_summon/cap_distance_and_events"),
                new CapAndEventTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_rooter_summon/cadence_and_ai"),
                new CadenceTest(data));
    }

    private static NexusParasiteEntity parent(GameTestHelper helper) {
        NexusParasiteEntity entity = helper.spawn(ModEntities.BECKON_SII.get(), new BlockPos(0, 200, 0));
        entity.setNoGravity(true);
        entity.setNoAi(true);
        entity.setPos(entity.getX() + 0.2D, entity.getY() + 0.25D, entity.getZ() + 0.1D);
        return entity;
    }

    private static CompoundTag save(NexusParasiteEntity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static boolean attempt(NexusParasiteEntity entity, ServerLevel level) {
        try {
            Method method = NexusParasiteEntity.class.getDeclaredMethod("trySpawnRooterSi",
                    ServerLevel.class, RandomSource.class, Random.class);
            method.setAccessible(true);
            return (boolean) method.invoke(entity, level, passingRoll(), new Random(CANDIDATE_SEED));
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Rooter summon failed", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke the Rooter summon", exception);
        }
    }

    private static RandomSource passingRoll() {
        for (long seed = 0; seed < 100_000; seed++) {
            if (RandomSource.create(seed).nextDouble() < 0.02D) return RandomSource.create(seed);
        }
        throw new IllegalStateException("No passing roll seed found");
    }

    private static List<Vec3> candidates(NexusParasiteEntity entity) {
        Random random = new Random(CANDIDATE_SEED);
        random.nextInt(9);
        random.nextInt(9);
        random.nextInt(1);
        List<Vec3> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            random.nextDouble();
            int x = 12 + random.nextInt(9);
            int z = 12 + random.nextInt(9);
            if (random.nextBoolean()) x = -x;
            if (random.nextBoolean()) z = -z;
            result.add(new Vec3(entity.getX() + x, entity.getY(), entity.getZ() + z));
        }
        return result;
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Missing test field " + name, exception);
        }
    }

    private static int intField(Object object, String name) {
        try {
            return field(object.getClass(), name).getInt(object);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void setIntField(Object object, String name, int value) {
        try {
            field(object.getClass(), name).setInt(object, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<SrpWorldData.DislodgmentCode> dislodgmentCodes(SrpWorldData data) {
        try {
            return (List<SrpWorldData.DislodgmentCode>) field(SrpWorldData.class, "dislodgmentCodes").get(data);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class ConfigScope implements AutoCloseable {
        private final List<Runnable> restore = new ArrayList<>();

        void setInt(String name, int value) {
            try {
                ModConfigSpec.IntValue setting = (ModConfigSpec.IntValue) field(Config.class, name).get(null);
                int previous = setting.get();
                restore.add(() -> setting.set(previous));
                setting.set(value);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }

        void setBoolean(String name, boolean value) {
            try {
                ModConfigSpec.BooleanValue setting = (ModConfigSpec.BooleanValue) field(Config.class, name).get(null);
                boolean previous = setting.get();
                restore.add(() -> setting.set(previous));
                setting.set(value);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public void close() {
            for (int i = restore.size() - 1; i >= 0; i--) restore.get(i).run();
        }
    }

    private static final class BlockScope implements AutoCloseable {
        private final ServerLevel level;
        private final Map<BlockPos, BlockState> previous = new LinkedHashMap<>();

        BlockScope(ServerLevel level) {
            this.level = level;
        }

        void set(BlockPos pos, BlockState state) {
            level.getChunkAt(pos);
            previous.putIfAbsent(pos, level.getBlockState(pos));
            level.setBlockAndUpdate(pos, state);
        }

        void floor(Vec3 position, BlockState state) {
            set(BlockPos.containing(position.x, position.y - 1.0D, position.z), state);
        }

        @Override
        public void close() {
            previous.forEach(level::setBlockAndUpdate);
        }
    }

    private abstract static class RooterTest extends GameTestInstance {
        RooterTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Nexus Rooter summon behavior");
        }
    }

    private static final class SnapshotTest extends RooterTest {
        SnapshotTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            SrpWorldData dimension = SrpWorldData.get(level);
            SrpWorldData global = SrpWorldData.get(level.getServer().overworld());
            int oldPhase = intField(dimension, "evolutionPhase");
            int oldDevelopment = intField(global, "ubiquitousDevelopment");
            NexusParasiteEntity original = null;
            NexusParasiteEntity loaded = null;
            NexusParasiteEntity oldSave = null;
            int[] joins = {0};
            Consumer<EntityJoinLevelEvent> cancelRooter = event -> {
                if (event.getLevel() == level && event.getEntity().getType() == ModEntities.ROOTER_SI.get()) {
                    joins[0]++;
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, cancelRooter);
            try (ConfigScope config = new ConfigScope(); BlockScope blocks = new BlockScope(level)) {
                config.setInt("EVOLUTION_NESTS", 5);
                config.setInt("DEVE_NESTS_USE", 3);
                config.setInt("NEXUS_ROOTER_CAP", 50);
                config.setInt("NEXUS_ROOTER_DISTANCE", 0);
                config.setBoolean("REINFORCEMENT_SKY", false);
                config.setBoolean("REINFORCEMENT_SOUNDS", false);
                setIntField(dimension, "evolutionPhase", 8);
                setIntField(global, "ubiquitousDevelopment", 4);
                original = parent(helper);
                CompoundTag saved = save(original);
                helper.assertTrue(saved.getBooleanOr("nexus_creation_captured", false)
                                && saved.getIntOr("nexus_creation_phase", -99) == 8
                                && saved.getIntOr("nexus_creation_development", -99) == 4,
                        "first world entry did not capture the phase and global development");
                setIntField(dimension, "evolutionPhase", 0);
                setIntField(global, "ubiquitousDevelopment", 0);
                original.discard();
                loaded = ModEntities.BECKON_SII.get().create(level, EntitySpawnReason.STRUCTURE);
                helper.assertTrue(loaded != null, "could not create the reloaded Nexus");
                loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved));
                helper.assertTrue(level.addFreshEntity(loaded), "could not add the reloaded Nexus");
                CompoundTag reloaded = save(loaded);
                helper.assertTrue(reloaded.getIntOr("nexus_creation_phase", -99) == 8
                                && reloaded.getIntOr("nexus_creation_development", -99) == 4,
                        "world re-entry replaced the saved creation snapshot");
                for (Vec3 position : candidates(loaded)) blocks.floor(position, Blocks.STONE.defaultBlockState());
                helper.assertFalse(attempt(loaded, level), "new world progress replaced the saved gate");
                setIntField(loaded, "creationEvolutionPhase", 5);
                helper.assertTrue(attempt(loaded, level), "equal creation phase did not pass the original gate");
                setIntField(loaded, "creationEvolutionPhase", 8);
                setIntField(loaded, "creationDevelopmentLevel", 3);
                helper.assertTrue(attempt(loaded, level), "equal development did not pass the original OR gate");
                helper.assertTrue(joins[0] == 2, "unexpected number of successful gate attempts");

                CompoundTag legacy = saved.copy();
                legacy.remove("nexus_creation_captured");
                legacy.remove("nexus_creation_phase");
                legacy.remove("nexus_creation_development");
                loaded.discard();
                oldSave = ModEntities.BECKON_SII.get().create(level, EntitySpawnReason.STRUCTURE);
                helper.assertTrue(oldSave != null, "could not create a pre-snapshot Nexus");
                oldSave.load(TagValueInput.create(ProblemReporter.DISCARDING, oldSave.registryAccess(), legacy));
                int currentGlobal = EvolutionSystem.ubiquitousDevelopment(level.getServer());
                helper.assertTrue(level.addFreshEntity(oldSave), "could not add a pre-snapshot Nexus");
                CompoundTag upgraded = save(oldSave);
                helper.assertTrue(upgraded.getIntOr("nexus_creation_phase", -99) == 0
                                && upgraded.getIntOr("nexus_creation_development", -99) == currentGlobal,
                        "old saves did not capture progress when first re-entering the world");
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(cancelRooter);
                setIntField(dimension, "evolutionPhase", oldPhase);
                setIntField(global, "ubiquitousDevelopment", oldDevelopment);
                if (original != null) original.discard();
                if (loaded != null) loaded.discard();
                if (oldSave != null) oldSave.discard();
            }
        }
    }

    private static final class PlacementTest extends RooterTest {
        PlacementTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            NexusParasiteEntity parent = parent(helper);
            parent.setCustomName(Component.literal("parent name"));
            List<Vec3> candidates = candidates(parent);
            Vec3 first = candidates.get(0);
            Vec3 second = candidates.get(1);
            BlockPos firstPos = BlockPos.containing(first);
            BlockPos secondPos = BlockPos.containing(second);
            List<NexusParasiteEntity> spawned = new ArrayList<>();
            Consumer<EntityJoinLevelEvent> watch = event -> {
                if (event.getLevel() == level && event.getEntity() instanceof NexusParasiteEntity rooter
                        && rooter.getType() == ModEntities.ROOTER_SI.get() && rooter.distanceToSqr(parent) < 1000.0D) {
                    spawned.add(rooter);
                }
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, watch);
            Mob obstruction = null;
            NexusParasiteEntity dispatcher = null;
            try (ConfigScope config = new ConfigScope(); BlockScope blocks = new BlockScope(level)) {
                config.setInt("EVOLUTION_NESTS", 11);
                config.setInt("NEXUS_ROOTER_CAP", 50);
                config.setInt("NEXUS_ROOTER_DISTANCE", 0);
                config.setBoolean("REINFORCEMENT_SKY", false);
                config.setBoolean("REINFORCEMENT_SOUNDS", false);
                helper.assertTrue(first.distanceToSqr(second) > 16.0D, "test candidates overlap");
                blocks.floor(first, Blocks.STONE.defaultBlockState());
                blocks.floor(second, Blocks.STONE.defaultBlockState());
                blocks.set(firstPos, Blocks.STONE.defaultBlockState());
                blocks.set(secondPos, Blocks.AIR.defaultBlockState());
                helper.assertTrue(attempt(parent, level), "second candidate was not attempted after blocked air");
                assertLastSpawn(helper, spawned, second);
                helper.assertTrue(spawned.getLast().getCustomName() == null, "summoned Rooter inherited the parent name");
                spawned.getLast().discard();

                blocks.set(firstPos, Blocks.AIR.defaultBlockState());
                obstruction = helper.spawn(EntityTypes.SHEEP, new BlockPos(0, 200, 0));
                obstruction.setNoAi(true);
                obstruction.setNoGravity(true);
                obstruction.setPos(first.x, first.y, first.z);
                helper.assertTrue(attempt(parent, level), "second candidate was not attempted after entity collision");
                assertLastSpawn(helper, spawned, second);
                spawned.getLast().discard();
                obstruction.discard();

                config.setBoolean("REINFORCEMENT_SKY", true);
                blocks.set(firstPos.above(10), Blocks.STONE.defaultBlockState());
                blocks.set(secondPos.above(10), Blocks.STONE.defaultBlockState());
                helper.assertFalse(attempt(parent, level), "covered candidates ignored the sky requirement");
                blocks.set(secondPos.above(10), Blocks.AIR.defaultBlockState());
                helper.assertTrue(attempt(parent, level), "open sky at the second candidate was rejected");
                assertLastSpawn(helper, spawned, second);
                spawned.getLast().discard();

                config.setBoolean("REINFORCEMENT_SKY", false);
                blocks.floor(first, Blocks.WATER.defaultBlockState());
                blocks.floor(second, Blocks.AIR.defaultBlockState());
                helper.assertTrue(attempt(parent, level), "a non-air liquid floor was incorrectly rejected");
                assertLastSpawn(helper, spawned, first);
                spawned.getLast().discard();
                blocks.set(firstPos, Blocks.CAVE_AIR.defaultBlockState());
                blocks.floor(second, Blocks.STONE.defaultBlockState());
                helper.assertTrue(attempt(parent, level), "second candidate was not used after non-standard air");
                assertLastSpawn(helper, spawned, second);
                spawned.getLast().discard();

                dispatcher = helper.spawn(ModEntities.DISPATCHER_SIII.get(), new BlockPos(0, 200, 0));
                dispatcher.setNoAi(true);
                dispatcher.setNoGravity(true);
                dispatcher.setPos(parent.position());
                helper.assertTrue(attempt(dispatcher, level), "stage III Dispatcher could not call a Rooter");
                assertLastSpawn(helper, spawned, second);
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(watch);
                if (obstruction != null) obstruction.discard();
                if (dispatcher != null) dispatcher.discard();
                for (NexusParasiteEntity entity : spawned) entity.discard();
                parent.discard();
            }
        }
    }

    private static void assertLastSpawn(GameTestHelper helper, List<NexusParasiteEntity> spawned, Vec3 expected) {
        helper.assertFalse(spawned.isEmpty(), "no Rooter join event was observed");
        NexusParasiteEntity rooter = spawned.getLast();
        helper.assertTrue(rooter.position().distanceToSqr(expected) < 0.000001D,
                "Rooter did not preserve the selected position and fractional coordinates");
    }

    private static final class CapAndEventTest extends RooterTest {
        CapAndEventTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            NexusParasiteEntity parent = parent(helper);
            List<NexusParasiteEntity> existing = new ArrayList<>();
            List<SrpWorldData.DislodgmentCode> codes = dislodgmentCodes(SrpWorldData.get(level));
            List<SrpWorldData.DislodgmentCode> savedNoiseCodes = codes.stream()
                    .filter(code -> code.code() == 15).toList();
            codes.removeIf(code -> code.code() == 15);
            int[] joins = {0};
            int[] sounds = {0};
            Consumer<EntityJoinLevelEvent> cancelRooter = event -> {
                if (event.getLevel() == level && event.getEntity().getType() == ModEntities.ROOTER_SI.get()
                        && event.getEntity().distanceToSqr(parent) < 1000.0D) {
                    joins[0]++;
                    event.setCanceled(true);
                }
            };
            Consumer<PlayLevelSoundEvent.AtPosition> watchSound = event -> {
                if (event.getLevel() == level && event.getSound() != null
                        && event.getSound().value() == ModSounds.get("leem.si")) sounds[0]++;
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, cancelRooter);
            NeoForge.EVENT_BUS.addListener(PlayLevelSoundEvent.AtPosition.class, watchSound);
            try (ConfigScope config = new ConfigScope(); BlockScope blocks = new BlockScope(level)) {
                config.setInt("EVOLUTION_NESTS", 11);
                config.setInt("NEXUS_ROOTER_CAP", 5);
                config.setInt("NEXUS_ROOTER_DISTANCE", 32);
                config.setBoolean("REINFORCEMENT_SKY", false);
                config.setBoolean("REINFORCEMENT_SOUNDS", true);
                config.setBoolean("DISLO_GROWL_NOISE", false);
                for (Vec3 position : candidates(parent)) blocks.floor(position, Blocks.STONE.defaultBlockState());
                for (int i = 0; i < 5; i++) {
                    NexusParasiteEntity rooter = helper.spawn(ModEntities.ROOTER_SII.get(), new BlockPos(0, 200, 0));
                    rooter.setNoAi(true);
                    rooter.setNoGravity(true);
                    level.getChunkAt(BlockPos.containing(parent.getX() + 32.0D, parent.getY(), parent.getZ() + i * 3.0D));
                    rooter.setPos(parent.getX() + 32.0D, parent.getY(), parent.getZ() + i * 3.0D);
                    existing.add(rooter);
                }
                NexusParasiteEntity cyst = helper.spawn(ModEntities.ROOTERBALL.get(), new BlockPos(0, 200, 0));
                cyst.setNoAi(true);
                cyst.setNoGravity(true);
                cyst.setPos(parent.getX() + 1.0D, parent.getY(), parent.getZ());
                existing.add(cyst);
                existing.getFirst().setPos(parent.getX() + 31.999D, parent.getY(), parent.getZ());
                helper.assertFalse(attempt(parent, level), "a Rooter inside the minimum distance was ignored");
                existing.getFirst().setPos(parent.getX() + 32.0D, parent.getY(), parent.getZ());
                helper.assertTrue(attempt(parent, level), "equal cap or exact distance was incorrectly rejected");
                helper.assertTrue(joins[0] == 1 && sounds[0] == 1,
                        "a canceled join did not terminate the attempt and play its sound");
                NexusParasiteEntity sixth = helper.spawn(ModEntities.ROOTER_SII.get(), new BlockPos(0, 200, 0));
                sixth.setNoAi(true);
                sixth.setNoGravity(true);
                sixth.setPos(parent.getX() + 32.0D, parent.getY(), parent.getZ() + 15.0D);
                existing.add(sixth);
                helper.assertFalse(attempt(parent, level), "more than the configured Rooter cap was accepted");
                helper.assertTrue(joins[0] == 1, "cap rejection reached the entity join event");
                config.setInt("NEXUS_ROOTER_CAP", 6);
                helper.assertTrue(attempt(parent, level), "the configured Rooter cap was ignored");
                helper.assertTrue(joins[0] == 2 && sounds[0] == 2, "the second canceled join was retried or silent");
                config.setBoolean("REINFORCEMENT_SOUNDS", false);
                helper.assertTrue(attempt(parent, level), "disabling sound disabled the summon");
                helper.assertTrue(joins[0] == 3 && sounds[0] == 2, "reinforcement sound toggle was ignored");
                config.setBoolean("REINFORCEMENT_SOUNDS", true);
                config.setBoolean("DISLO_GROWL_NOISE", true);
                codes.add(new SrpWorldData.DislodgmentCode(15, 1, level.getGameTime() + 1_200));
                helper.assertTrue(attempt(parent, level), "the growl dislodgment prevented summoning");
                helper.assertTrue(joins[0] == 4 && sounds[0] == 2, "active dislodgment did not silence the sound");
                config.setBoolean("DISLO_GROWL_NOISE", false);
                helper.assertTrue(attempt(parent, level), "disabled growl suppression prevented summoning");
                helper.assertTrue(joins[0] == 5 && sounds[0] == 3,
                        "disabled growl suppression still silenced the sound");
                config.setBoolean("REINFORCEMENT_SOUNDS", false);
                UUID full = UUID.randomUUID();
                UUID excess = UUID.randomUUID();
                int capacity = parent.getSummonCapacity();
                helper.assertTrue(capacity > 0, "test Beckon has no summon capacity");
                parent.reserveTrackedSummon(full, capacity);
                helper.assertTrue(attempt(parent, level), "zero-cost Rooter was rejected at full capacity");
                parent.reserveTrackedSummon(excess, 1);
                helper.assertFalse(attempt(parent, level), "zero-cost Rooter ignored overfilled capacity");
                helper.assertTrue(joins[0] == 6, "overfilled capacity reached the join event");
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(cancelRooter);
                NeoForge.EVENT_BUS.unregister(watchSound);
                codes.removeIf(code -> code.code() == 15);
                codes.addAll(savedNoiseCodes);
                for (NexusParasiteEntity entity : existing) entity.discard();
                parent.discard();
            }
        }
    }

    private static final class CadenceTest extends RooterTest {
        CadenceTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity parent = parent(helper);
            try {
                for (int i = 0; i < 30; i++) parent.tick();
                helper.assertTrue(intField(parent, "rooterSummonCooldown") == 20,
                        "disabled AI advanced the Rooter summon counter");
                parent.setNoAi(false);
                for (int i = 0; i < 20; i++) parent.tick();
                helper.assertTrue(intField(parent, "rooterSummonCooldown") == 0,
                        "the 20-counter reset before reaching zero");
                parent.tick();
                helper.assertTrue(intField(parent, "rooterSummonCooldown") == 20,
                        "the counter did not reset after decrementing below zero");
                helper.succeed();
            } finally {
                parent.discard();
            }
        }
    }
}
