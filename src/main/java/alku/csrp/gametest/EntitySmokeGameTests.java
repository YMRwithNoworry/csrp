package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration + implementation of the {@code csrp} entity smoke GameTests.
 *
 * <p>MC 26.3 replaced the old annotation driven GameTest API ({@code @GameTest},
 * {@code @GameTestGenerator}, {@code @GameTestHolder}) with a data driven registry based one. Tests
 * are contributed through {@link RegisterGameTestsEvent} (a mod bus event fired from
 * {@code RegistryDataLoader} while the game test server/client loads) as {@link GameTestInstance}s.</p>
 *
 * <p>One test is generated per registered {@link EntityType} in {@link ModEntities}, named
 * {@code csrp:entity_smoke/<entity id>}, so a failure names the offending entity id directly. A single
 * additional test ({@code csrp:entity_nbt/round_trip}) drives the migrated
 * {@code addAdditionalSaveData(ValueOutput)} / {@code readAdditionalSaveData(ValueInput)} path for
 * every registered entity through the public {@code saveWithoutId} / {@code load} wrappers.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class EntitySmokeGameTests {
    private static final String MODID = Csrp.MODID;
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID + "/gametest");
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final int TICK_CHECK = 5;
    /** The vanilla structure template used by the built-in {@code minecraft:always_pass} test. */
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private EntitySmokeGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "entity_smoke"),
                new TestEnvironmentDefinition.AllOf(List.of()));

        // Resolve the REAL registered entity ids from the DeferredRegister instead of hard coding them,
        // so helper/loop registrations are all covered.
        List<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> entityTypes =
                List.copyOf(ModEntities.ENTITIES.getEntries());
        LOGGER.info("Registering {} entity spawn smoke tests + 1 NBT round-trip test", entityTypes.size());

        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> holder : entityTypes) {
            Identifier testId = Identifier.fromNamespaceAndPath(MODID, "entity_smoke/" + holder.getId().getPath());
            event.registerTest(testId, new EntitySpawnTest(testData(environment), holder.get()));
        }

        event.registerTest(
                Identifier.fromNamespaceAndPath(MODID, "entity_nbt/round_trip"),
                new EntityNbtRoundTripTest(testData(environment), entityTypes));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Entity spawnAtTestBlock(GameTestHelper helper, EntityType<?> type) {
        // One block above the test origin; spawn() throws a descriptive GameTestAssertException when
        // EntityType.create() returns null.
        return helper.spawn((EntityType) type, new BlockPos(0, 1, 0));
    }

    /** Spawns one entity type, asserts it exists, ticks it a few times and succeeds. */
    private static final class EntitySpawnTest extends GameTestInstance {
        private final EntityType<?> entityType;

        EntitySpawnTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData, EntityType<?> entityType) {
            super(testData);
            this.entityType = entityType;
        }

        @Override
        public void run(GameTestHelper helper) {
            String id = this.entityType.builtInRegistryHolder().getRegisteredName();
            Entity entity = spawnAtTestBlock(helper, this.entityType);
            // Assert the entity really exists right after creation. We deliberately do NOT assert it is
            // still alive after ticking: owner/target-requiring effect entities (waves, slashes,
            // projectiles, summoned mobs) legitimately remove themselves when spawned bare. The point
            // here is to execute construction + a few ticks and surface exceptions, not gameplay.
            helper.assertTrue(entity != null && entity.isAlive(),
                    Component.literal("Entity was not created/alive after spawn: " + id));

            helper.runAtTickTime(TICK_CHECK, helper::succeed);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            // Code-registered instances are never serialized; this only satisfies the abstract method.
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp entity spawn smoke test");
        }
    }

    /**
     * One test that creates every registered entity and round-trips its NBT through
     * {@code addAdditionalSaveData}/{@code readAdditionalSaveData}.
     */
    private static final class EntityNbtRoundTripTest extends GameTestInstance {
        private final List<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> entityTypes;

        EntityNbtRoundTripTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                List<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> entityTypes) {
            super(testData);
            this.entityTypes = entityTypes;
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            List<String> failures = new ArrayList<>();

            for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> holder : this.entityTypes) {
                String id = holder.getId().toString();
                Entity entity = null;
                try {
                    entity = holder.get().create(level, EntitySpawnReason.STRUCTURE);
                    if (entity == null) {
                        failures.add(id + " -> EntityType.create returned null");
                        continue;
                    }

                    TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
                    entity.saveWithoutId(output);
                    CompoundTag saved = output.buildResult();
                    entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), saved));
                } catch (Throwable t) {
                    failures.add(id + " -> " + t.getClass().getName() + ": " + t.getMessage());
                } finally {
                    if (entity != null) {
                        entity.discard();
                    }
                }
            }

            if (failures.isEmpty()) {
                LOGGER.info("NBT round-trip passed for all {} entity types", this.entityTypes.size());
                helper.succeed();
            } else {
                helper.fail(Component.literal(
                        "NBT round-trip failed for " + failures.size() + "/" + this.entityTypes.size()
                                + " entities:\n" + String.join("\n", failures)));
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            // Code-registered instances are never serialized; this only satisfies the abstract method.
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp entity NBT round-trip test");
        }
    }
}
