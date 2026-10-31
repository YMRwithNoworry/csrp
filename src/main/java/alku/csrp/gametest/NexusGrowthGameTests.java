package alku.csrp.gametest;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.config.RuntimeToggles;
import alku.csrp.config.WorldConfig;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.ParasiteBiomeDecorator;
import alku.csrp.world.SrpWorldData;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Server-side checks for Nexus stage upgrades, gates, and protection rolls. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class NexusGrowthGameTests {
    private static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");

    private NexusGrowthGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_growth"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY, 30, 1, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_growth/replacements"),
                new ReplacementTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_growth/stage_four_gates"),
                new StageFourTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_growth/sounds_and_structures"),
                new SoundAndStructureTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_growth/no_ai"),
                new NoAiTest(data));
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Object invoke(NexusParasiteEntity entity, String name, Class<?>[] types, Object... arguments) {
        try {
            Method method = NexusParasiteEntity.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(entity, arguments);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(name, exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(name, exception);
        }
    }

    private static boolean evolve(NexusParasiteEntity entity) {
        return (boolean) invoke(entity, "evolve", new Class<?>[0]);
    }

    private static boolean stageFourUnlocked(NexusParasiteEntity entity) {
        return (boolean) invoke(entity, "stageFourUnlocked",
                new Class<?>[] {ServerLevel.class, NexusParasiteEntity.Kind.class},
                entity.level(), entity.getKind());
    }

    private static boolean protectionRoll(NexusParasiteEntity entity, RandomSource random) {
        return (boolean) invoke(entity, "shouldGenerateProtectionStructure",
                new Class<?>[] {NexusParasiteEntity.Kind.class, RandomSource.class}, entity.getKind(), random);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> adaptations(NexusParasiteEntity entity) {
        try {
            return (Map<String, Integer>) field(PrimitiveParasiteEntity.class, "damageAdaptations").get(entity);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void setInt(Object target, String name, int value) {
        try {
            field(target.getClass(), name).setInt(target, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int getInt(Object target, String name) {
        try {
            return field(target.getClass(), name).getInt(target);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<SrpWorldData.DislodgmentCode> codes(SrpWorldData data) {
        try {
            return (List<SrpWorldData.DislodgmentCode>) field(SrpWorldData.class, "dislodgmentCodes").get(data);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeColony(SrpWorldData data, BlockPos pos) {
        try {
            ((List<SrpWorldData.ColonyEntry>) field(SrpWorldData.class, "colonies").get(data))
                    .removeIf(entry -> entry.pos().equals(pos));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static NexusParasiteEntity spawn(GameTestHelper helper, EntityType<NexusParasiteEntity> type) {
        NexusParasiteEntity entity = helper.spawn(type, new BlockPos(0, 200, 0));
        entity.setNoAi(true);
        entity.setNoGravity(true);
        return entity;
    }

    private static final class ConfigScope implements AutoCloseable {
        private final List<Runnable> restore = new ArrayList<>();

        void setBoolean(Class<?> owner, String name, boolean value) {
            try {
                ModConfigSpec.BooleanValue setting = (ModConfigSpec.BooleanValue) field(owner, name).get(null);
                boolean previous = setting.get();
                restore.add(() -> setting.set(previous));
                setting.set(value);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }

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

        @Override
        public void close() {
            for (int i = restore.size() - 1; i >= 0; i--) restore.get(i).run();
        }
    }

    private abstract static class GrowthTest extends GameTestInstance {
        GrowthTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Nexus growth behavior");
        }
    }

    private record Transition(EntityType<NexusParasiteEntity> from, EntityType<NexusParasiteEntity> to,
            String sound, float volume) {
    }

    private static List<Transition> transitions() {
        return List.of(
                new Transition(ModEntities.BECKON_SI.get(), ModEntities.BECKON_SII.get(), "venkrol.sii", 10),
                new Transition(ModEntities.BECKON_SII.get(), ModEntities.BECKON_SIII.get(), "venkrol.siii", 9),
                new Transition(ModEntities.BECKON_SIII.get(), ModEntities.BECKON_SIV.get(), "venkrol.siv", 10),
                new Transition(ModEntities.DISPATCHER_SI.get(), ModEntities.DISPATCHER_SII.get(), "dod.sii", 10),
                new Transition(ModEntities.DISPATCHER_SII.get(), ModEntities.DISPATCHER_SIII.get(), "dod.siii", 9),
                new Transition(ModEntities.DISPATCHER_SIII.get(), ModEntities.DISPATCHER_SIV.get(), "dod.siv", 10),
                new Transition(ModEntities.ROOTER_SI.get(), ModEntities.ROOTER_SII.get(), "leem.sii", 10),
                new Transition(ModEntities.ROOTER_SII.get(), ModEntities.ROOTER_SIII.get(), "leem.siii", 9),
                new Transition(ModEntities.ROOTER_SIII.get(), ModEntities.ROOTER_SIV.get(), "leem.siv", 10));
    }

    private static final class ReplacementTest extends GrowthTest {
        ReplacementTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            List<NexusParasiteEntity> all = new ArrayList<>();
            NexusParasiteEntity[] parent = {null};
            NexusParasiteEntity[] replacement = {null};
            EntityType<?>[] expectedType = {null};
            boolean[] cancel = {false};
            int[] joins = {0};
            Consumer<EntityJoinLevelEvent> listener = event -> {
                if (event.getLevel() != level || parent[0] == null || event.getEntity().getType() != expectedType[0]) return;
                helper.assertTrue(parent[0].isRemoved(), "parent was still present at replacement join");
                replacement[0] = (NexusParasiteEntity) event.getEntity();
                helper.assertTrue(adaptations(replacement[0]).isEmpty(), "adaptations copied before join event");
                helper.assertFalse(replacement[0].isOnFire(), "fire inherited before join event");
                joins[0]++;
                if (cancel[0]) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, listener);
            boolean evolutionInitiallyEnabled = RuntimeToggles.mobEvolution();
            if (!evolutionInitiallyEnabled) RuntimeToggles.toggleMobEvolution();
            try (ConfigScope config = new ConfigScope()) {
                config.setBoolean(Config.class, "USE_EVOLUTION_PHASES", false);
                config.setBoolean(Config.class, "REINFORCEMENT_SOUNDS", false);
                config.setBoolean(Config.class, "NEXUS_STRUCTURES", false);
                for (Transition transition : transitions()) {
                    parent[0] = null;
                    NexusParasiteEntity source = spawn(helper, transition.from());
                    all.add(source);
                    parent[0] = source;
                    expectedType[0] = transition.to();
                    replacement[0] = null;
                    adaptations(source).put("test:damage", 3);
                    source.setCustomName(Component.literal("Nexus Test"));
                    source.setCustomNameVisible(true);
                    source.setPersistenceRequired();
                    source.igniteForSeconds(3);
                    helper.assertTrue(evolve(source), "normal upgrade failed: " + transition.sound());
                    NexusParasiteEntity next = replacement[0];
                    helper.assertTrue(next != null && next.getType() == transition.to(), "wrong replacement type");
                    all.add(next);
                    helper.assertTrue(adaptations(next).getOrDefault("test:damage", 0) == 3, "adaptation not inherited");
                    helper.assertTrue(next.hasCustomName() && next.isCustomNameVisible() && next.isPersistenceRequired(),
                            "name, visibility, or persistence not inherited");
                    helper.assertTrue(next.getRemainingFireTicks() == 160
                                    && Math.abs(next.getHealth() - next.getMaxHealth() * 0.5F) < 0.001F,
                            "burning upgrade did not inherit fire and half health");
                }
                parent[0] = null;
                NexusParasiteEntity source = spawn(helper, ModEntities.BECKON_SI.get());
                all.add(source);
                parent[0] = source;
                expectedType[0] = ModEntities.BECKON_SII.get();
                replacement[0] = null;
                adaptations(source).put("test:damage", 4);
                source.igniteForSeconds(3);
                cancel[0] = true;
                helper.assertTrue(evolve(source), "canceled join changed upgrade return semantics");
                helper.assertTrue(source.isRemoved() && replacement[0] != null, "canceled join restored parent");
                helper.assertTrue(adaptations(replacement[0]).getOrDefault("test:damage", 0) == 4
                                && replacement[0].getRemainingFireTicks() == 160,
                        "canceled join skipped post-join inheritance");
                helper.assertTrue(joins[0] == transitions().size() + 1, "unexpected join count");
                parent[0] = null;
                NexusParasiteEntity disabled = spawn(helper, ModEntities.BECKON_SI.get());
                all.add(disabled);
                parent[0] = disabled;
                RuntimeToggles.toggleMobEvolution();
                try {
                    helper.assertFalse(evolve(disabled), "runtime evolution toggle did not block upgrade");
                    helper.assertFalse(disabled.isRemoved(), "disabled evolution removed the parent");
                    helper.assertTrue(joins[0] == transitions().size() + 1, "disabled evolution attempted a join");
                } finally {
                    RuntimeToggles.toggleMobEvolution();
                }
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                if (!evolutionInitiallyEnabled) RuntimeToggles.toggleMobEvolution();
                for (NexusParasiteEntity entity : all) entity.discard();
            }
        }
    }

    private record BiomeCell(PalettedContainer<Holder<Biome>> palette, int x, int y, int z,
            Holder<Biome> previous) {
    }

    private static final class StageFourTest extends GrowthTest {
        StageFourTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        @SuppressWarnings("unchecked")
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            SrpWorldData data = SrpWorldData.get(level);
            SrpWorldData global = SrpWorldData.get(level.getServer().overworld());
            int oldPhase = getInt(data, "evolutionPhase");
            int oldDevelopment = getInt(global, "ubiquitousDevelopment");
            boolean oldEve = global.eveMode();
            List<NexusParasiteEntity> entities = new ArrayList<>();
            SrpWorldData.NodeEntry savedNode = null;
            SrpWorldData.ColonyEntry savedColony = null;
            BlockPos node = null;
            BlockPos colony = null;
            List<BiomeCell> changedBiomes = new ArrayList<>();
            try (ConfigScope config = new ConfigScope()) {
                config.setBoolean(Config.class, "USE_EVOLUTION_PHASES", true);
                config.setInt("EVOLUTION_NODE_UNLOCK", 6);
                config.setInt("EVOLUTION_COLONY_UNLOCK", 6);
                config.setInt("EVOLUTION_HIVES", 6);
                config.setInt("DEVE_NODES_USE", 4);
                config.setInt("DEVE_COLONIES_USE", 4);
                config.setInt("DEVE_HIVES_USE", 4);
                config.setBoolean(WorldConfig.class, "NODES_ACTIVATED", true);
                config.setBoolean(WorldConfig.class, "BIOME_REGISTERED", true);
                global.setEveMode(false);
                global.setUbiquitousDevelopment(3);
                setInt(data, "evolutionPhase", 5);
                entities.add(spawn(helper, ModEntities.BECKON_SIII.get()));
                entities.add(spawn(helper, ModEntities.DISPATCHER_SIII.get()));
                entities.add(spawn(helper, ModEntities.ROOTER_SIII.get()));
                for (NexusParasiteEntity entity : entities) {
                    helper.assertFalse(stageFourUnlocked(entity), "both unmet thresholds permitted stage IV");
                }
                setInt(data, "evolutionPhase", 6);
                for (NexusParasiteEntity entity : entities) {
                    helper.assertTrue(stageFourUnlocked(entity), "equal phase threshold did not unlock stage IV");
                }
                setInt(data, "evolutionPhase", 5);
                global.setUbiquitousDevelopment(4);
                for (NexusParasiteEntity entity : entities) {
                    helper.assertTrue(stageFourUnlocked(entity), "equal development threshold did not unlock stage IV");
                }
                config.setInt("EVOLUTION_NODE_UNLOCK", 5);
                config.setInt("EVOLUTION_COLONY_UNLOCK", 6);
                config.setInt("EVOLUTION_HIVES", 7);
                global.setUbiquitousDevelopment(3);
                setInt(data, "evolutionPhase", 6);
                helper.assertTrue(stageFourUnlocked(entities.get(0)), "Beckon did not use node phase threshold");
                helper.assertTrue(stageFourUnlocked(entities.get(1)), "Dispatcher did not use colony phase threshold");
                helper.assertFalse(stageFourUnlocked(entities.get(2)), "Rooter ignored hive phase threshold");
                setInt(data, "evolutionPhase", 0);
                config.setInt("DEVE_NODES_USE", 2);
                config.setInt("DEVE_COLONIES_USE", 3);
                config.setInt("DEVE_HIVES_USE", 4);
                global.setUbiquitousDevelopment(3);
                helper.assertTrue(stageFourUnlocked(entities.get(0)), "Beckon did not use node development threshold");
                helper.assertTrue(stageFourUnlocked(entities.get(1)), "Dispatcher did not use colony development threshold");
                helper.assertFalse(stageFourUnlocked(entities.get(2)), "Rooter ignored hive development threshold");
                config.setBoolean(Config.class, "USE_EVOLUTION_PHASES", false);
                global.setUbiquitousDevelopment(3);
                for (NexusParasiteEntity entity : entities) {
                    helper.assertTrue(stageFourUnlocked(entity), "disabled evolution still enforced thresholds");
                }
                node = entities.get(0).blockPosition();
                BlockPos nodePos = node;
                savedNode = data.nodes().stream().filter(entry -> entry.pos().equals(nodePos)).findFirst().orElse(null);
                data.setNode(node, 1, 1);
                helper.assertFalse(stageFourUnlocked(entities.get(0)), "node effect range did not block Beckon IV");
                helper.assertTrue(stageFourUnlocked(entities.get(2)), "node effect range blocked Rooter IV");
                config.setBoolean(WorldConfig.class, "BIOME_REGISTERED", false);
                helper.assertTrue(stageFourUnlocked(entities.get(0)), "disabled biome registration still blocked Beckon");
                config.setBoolean(WorldConfig.class, "BIOME_REGISTERED", true);
                data.removeNode(node);
                if (savedNode != null) data.setNode(savedNode.pos(), savedNode.age(), savedNode.type());
                savedNode = null;
                node = null;
                colony = entities.get(1).blockPosition();
                BlockPos colonyPos = colony;
                savedColony = data.colonies().stream().filter(entry -> entry.pos().equals(colonyPos)).findFirst().orElse(null);
                data.setColony(colony);
                helper.assertFalse(stageFourUnlocked(entities.get(1)), "colony effect range did not block Dispatcher IV");
                helper.assertTrue(stageFourUnlocked(entities.get(2)), "colony effect range blocked Rooter IV");
                removeColony(data, colony);
                if (savedColony != null) data.updateColony(savedColony.pos(), savedColony.points());
                savedColony = null;
                colony = null;
                BlockPos pos = entities.get(0).blockPosition();
                Holder<Biome> parasiteBiome = level.registryAccess().lookupOrThrow(Registries.BIOME)
                        .getOrThrow(ParasiteBiomeDecorator.HARLEQUIN_BIOME);
                int baseX = QuartPos.fromBlock(pos.getX() - 2);
                int baseY = QuartPos.fromBlock(pos.getY() - 2);
                int baseZ = QuartPos.fromBlock(pos.getZ() - 2);
                for (int x = 0; x <= 1; x++) {
                    for (int y = 0; y <= 1; y++) {
                        for (int z = 0; z <= 1; z++) {
                            int quartX = baseX + x;
                            int quartY = baseY + y;
                            int quartZ = baseZ + z;
                            LevelChunk chunk = level.getChunk(QuartPos.toSection(quartX), QuartPos.toSection(quartZ));
                            PalettedContainer<Holder<Biome>> palette = (PalettedContainer<Holder<Biome>>) chunk
                                    .getSection(chunk.getSectionIndex(QuartPos.toBlock(quartY))).getBiomes();
                            int localX = quartX & 3, localY = quartY & 3, localZ = quartZ & 3;
                            Holder<Biome> previous = palette.getAndSet(localX, localY, localZ, parasiteBiome);
                            changedBiomes.add(new BiomeCell(palette, localX, localY, localZ, previous));
                        }
                    }
                }
                for (NexusParasiteEntity entity : entities) {
                    helper.assertFalse(stageFourUnlocked(entity), "parasite biome permitted stage III growth");
                }
                helper.succeed();
            } finally {
                for (BiomeCell cell : changedBiomes) {
                    cell.palette().getAndSet(cell.x(), cell.y(), cell.z(), cell.previous());
                }
                if (node != null) {
                    data.removeNode(node);
                    if (savedNode != null) data.setNode(savedNode.pos(), savedNode.age(), savedNode.type());
                }
                if (colony != null) {
                    removeColony(data, colony);
                    if (savedColony != null) data.updateColony(savedColony.pos(), savedColony.points());
                }
                setInt(data, "evolutionPhase", oldPhase);
                global.setUbiquitousDevelopment(oldDevelopment);
                global.setEveMode(oldEve);
                for (NexusParasiteEntity entity : entities) entity.discard();
            }
        }
    }

    private static RandomSource fixedRoll(double value, int[] calls) {
        return (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
                new Class<?>[] {RandomSource.class}, (proxy, method, args) -> {
                    if (method.getName().equals("nextDouble")) {
                        calls[0]++;
                        return value;
                    }
                    throw new IllegalStateException("Unexpected random call: " + method.getName());
                });
    }

    private static final class SoundAndStructureTest extends GrowthTest {
        SoundAndStructureTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            List<NexusParasiteEntity> all = new ArrayList<>();
            List<SrpWorldData.DislodgmentCode> codes = codes(SrpWorldData.get(level));
            List<SrpWorldData.DislodgmentCode> saved = codes.stream().filter(code -> code.code() == 16).toList();
            codes.removeIf(code -> code.code() == 16);
            List<String> sounds = new ArrayList<>();
            Consumer<PlayLevelSoundEvent.AtPosition> watchSound = event -> {
                if (event.getLevel() != level || event.getSound() == null) return;
                for (Transition transition : transitions()) {
                    if (event.getSound().value() == ModSounds.get(transition.sound())) {
                        helper.assertTrue(event.getOriginalVolume() == transition.volume(), "incorrect upgrade sound volume");
                        sounds.add(transition.sound());
                    }
                }
            };
            NexusParasiteEntity[] currentParent = {null};
            EntityType<?>[] expectedReplacement = {null};
            boolean[] measureRandom = {false};
            Consumer<EntityJoinLevelEvent> cancelReplacement = event -> {
                if (event.getLevel() == level && currentParent[0] != null && currentParent[0].isRemoved()
                        && event.getEntity().getType() == expectedReplacement[0]) {
                    if (measureRandom[0]) level.getRandom().setSeed(0L);
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(PlayLevelSoundEvent.AtPosition.class, watchSound);
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, cancelReplacement);
            boolean evolutionInitiallyEnabled = RuntimeToggles.mobEvolution();
            if (!evolutionInitiallyEnabled) RuntimeToggles.toggleMobEvolution();
            try (ConfigScope config = new ConfigScope()) {
                config.setBoolean(Config.class, "USE_EVOLUTION_PHASES", false);
                config.setBoolean(Config.class, "NEXUS_STRUCTURES", true);
                config.setBoolean(Config.class, "REINFORCEMENT_SOUNDS", false);
                config.setBoolean(Config.class, "DISLO_WALK_NOISE", true);
                for (Transition transition : transitions()) {
                    NexusParasiteEntity entity = spawn(helper, transition.from());
                    all.add(entity);
                    int[] calls = {0};
                    double chance = transition.sound().startsWith("venkrol") ? 0.5D : 0.3D;
                    boolean firstStage = transition.sound().endsWith(".sii");
                    helper.assertTrue(protectionRoll(entity, fixedRoll(chance - 0.00001D, calls)) == firstStage,
                            "wrong structure probability or stage: " + transition.sound());
                    helper.assertTrue(calls[0] == (firstStage ? 1 : 0), "wrong structure random consumption");
                    calls[0] = 0;
                    helper.assertFalse(protectionRoll(entity, fixedRoll(chance, calls)), "probability boundary accepted");
                    helper.assertTrue(calls[0] == (firstStage ? 1 : 0), "boundary used unexpected random calls");
                }
                NexusParasiteEntity disabled = all.getFirst();
                config.setBoolean(Config.class, "NEXUS_STRUCTURES", false);
                int[] calls = {0};
                helper.assertFalse(protectionRoll(disabled, fixedRoll(0, calls)), "disabled structure generated");
                helper.assertTrue(calls[0] == 0, "disabled structure consumed random value");
                currentParent[0] = disabled;
                expectedReplacement[0] = ModEntities.BECKON_SII.get();
                helper.assertTrue(evolve(disabled), "sound-disabled upgrade failed");
                helper.assertTrue(sounds.isEmpty(), "disabled reinforcement sounds still played");
                config.setBoolean(Config.class, "REINFORCEMENT_SOUNDS", true);
                codes.add(new SrpWorldData.DislodgmentCode(16, 1, level.getGameTime() + 1200));
                currentParent[0] = null;
                NexusParasiteEntity silenced = spawn(helper, ModEntities.ROOTER_SI.get());
                all.add(silenced);
                currentParent[0] = silenced;
                expectedReplacement[0] = ModEntities.ROOTER_SII.get();
                helper.assertTrue(evolve(silenced), "dislodgment-suppressed upgrade failed");
                helper.assertTrue(sounds.isEmpty(), "walk-noise dislodgment did not silence upgrade");
                config.setBoolean(Config.class, "DISLO_WALK_NOISE", false);
                for (Transition transition : transitions()) {
                    currentParent[0] = null;
                    NexusParasiteEntity source = spawn(helper, transition.from());
                    all.add(source);
                    currentParent[0] = source;
                    expectedReplacement[0] = transition.to();
                    helper.assertTrue(evolve(source), "sound-enabled upgrade failed: " + transition.sound());
                    helper.assertTrue(sounds.getLast().equals(transition.sound()), "incorrect upgrade sound");
                }
                helper.assertTrue(sounds.size() == transitions().size(), "unexpected upgrade sound count");

                config.setBoolean(Config.class, "NEXUS_STRUCTURES", true);
                config.setBoolean(Config.class, "REINFORCEMENT_SOUNDS", false);
                assertWorldRoll(helper, all, currentParent, expectedReplacement, measureRandom,
                        ModEntities.BECKON_SI.get(), ModEntities.BECKON_SII.get(), false);
                config.setBoolean(Config.class, "REINFORCEMENT_SOUNDS", true);
                config.setBoolean(Config.class, "DISLO_WALK_NOISE", true);
                assertWorldRoll(helper, all, currentParent, expectedReplacement, measureRandom,
                        ModEntities.BECKON_SI.get(), ModEntities.BECKON_SII.get(), false);
                config.setBoolean(Config.class, "DISLO_WALK_NOISE", false);
                config.setBoolean(Config.class, "NEXUS_STRUCTURES", false);
                assertWorldRoll(helper, all, currentParent, expectedReplacement, measureRandom,
                        ModEntities.BECKON_SI.get(), ModEntities.BECKON_SII.get(), false);
                config.setBoolean(Config.class, "NEXUS_STRUCTURES", true);
                assertWorldRoll(helper, all, currentParent, expectedReplacement, measureRandom,
                        ModEntities.BECKON_SII.get(), ModEntities.BECKON_SIII.get(), false);
                assertWorldRoll(helper, all, currentParent, expectedReplacement, measureRandom,
                        ModEntities.BECKON_SI.get(), ModEntities.BECKON_SII.get(), true);
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(watchSound);
                NeoForge.EVENT_BUS.unregister(cancelReplacement);
                if (!evolutionInitiallyEnabled) RuntimeToggles.toggleMobEvolution();
                codes.removeIf(code -> code.code() == 16);
                codes.addAll(saved);
                for (NexusParasiteEntity entity : all) entity.discard();
            }
        }
        private void assertWorldRoll(GameTestHelper helper, List<NexusParasiteEntity> all,
                NexusParasiteEntity[] parent, EntityType<?>[] replacement, boolean[] measureRandom,
                EntityType<NexusParasiteEntity> from, EntityType<NexusParasiteEntity> to, boolean consumesRoll) {
            parent[0] = null;
            NexusParasiteEntity source = spawn(helper, from);
            all.add(source);
            parent[0] = source;
            replacement[0] = to;
            RandomSource expected = RandomSource.create(0L);
            if (consumesRoll) {
                helper.assertTrue(expected.nextDouble() >= 0.5D, "test seed unexpectedly generates a structure");
            }
            measureRandom[0] = true;
            try {
                helper.assertTrue(evolve(source), "upgrade failed while checking world random sequence");
                helper.assertTrue(helper.getLevel().getRandom().nextDouble() == expected.nextDouble(),
                        "upgrade consumed the wrong number of structure rolls");
            } finally {
                measureRandom[0] = false;
            }
        }
    }

    private static final class NoAiTest extends GrowthTest {
        NoAiTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity entity = spawn(helper, ModEntities.BECKON_SI.get());
            SrpWorldData data = SrpWorldData.get(helper.getLevel());
            int oldPhase = getInt(data, "evolutionPhase");
            try {
                setInt(data, "evolutionPhase", 10);
                setInt(entity, "growthDelayTicks", 1);
                int before = getInt(entity, "growthTicks");
                for (int i = 0; i < 24; i++) entity.tick();
                helper.assertTrue(getInt(entity, "growthTicks") == before && !entity.isRemoved(),
                        "NoAI Nexus accumulated growth or evolved");
                helper.succeed();
            } finally {
                setInt(data, "evolutionPhase", oldPhase);
                entity.discard();
            }
        }
    }
}
