package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.entity.DeterrentParasiteEntity;
import alku.csrp.entity.GoreEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.ParasiteBlockInventory;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;

/** Runtime contracts for Dispatcher tentacle payloads and natural despawn. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DispatcherLifecycleGameTests {
    private static final BlockPos ORIGIN = new BlockPos(0, 150, 0);
    private static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");

    private DispatcherLifecycleGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_lifecycle"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY, 120, 1, true, Rotation.NONE);
        event.registerTest(id("payload_effects"), new PayloadEffectsTest(data));
        event.registerTest(id("death_and_failed_join"), new DeathAndFailureTest(data));
        event.registerTest(id("transient_on_reload"), new ReloadTest(data));
        event.registerTest(id("despawn_order_and_owner"), new DespawnTest(data));
        event.registerTest(id("teleport_and_retreat"), new TeleportTest(data));
        event.registerTest(id("distance_despawn"), new DistanceDespawnTest(data));
        event.registerTest(id("server_tick_deployment"), new ServerTickTest(data));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Csrp.MODID, "dispatcher_lifecycle/" + name);
    }

    private static void tickTentacle(DeterrentParasiteEntity tentacle, int count) {
        try {
            Method method = DeterrentParasiteEntity.class.getDeclaredMethod("tickDispatcherTentacle");
            method.setAccessible(true);
            for (int i = 0; i < count; i++) method.invoke(tentacle);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException(exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Object field(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> inventory(NexusParasiteEntity parent) {
        return (List<String>) field(parent, "storedParasiteIds");
    }

    private static CompoundTag save(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final List<Entity> entities = new ArrayList<>();
        private final BlockPos origin;
        private final BlockState[] floor = new BlockState[3];

        Scene(GameTestHelper helper) {
            this.helper = helper;
            origin = helper.absolutePos(ORIGIN);
            for (int depth = 1; depth <= 3; depth++) {
                BlockPos pos = origin.below(depth);
                floor[depth - 1] = helper.getLevel().getBlockState(pos);
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

        DeterrentParasiteEntity tentacle(NexusParasiteEntity parent, int stage, Identifier payload) {
            DeterrentParasiteEntity tentacle = spawn(ModEntities.DISPATCHERTEN.get());
            tentacle.setDispatchEntity(payload);
            tentacle.setDispatcherStage(stage);
            tentacle.setDispatcherOwner(parent);
            return tentacle;
        }

        @Override
        public void close() {
            for (Entity entity : entities) entity.discard();
            for (int depth = 1; depth <= 3; depth++) {
                helper.getLevel().setBlockAndUpdate(origin.below(depth), floor[depth - 1]);
            }
        }
    }

    private abstract static class Test extends GameTestInstance {
        Test(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }
        @Override public MapCodec<? extends GameTestInstance> codec() { return FunctionGameTestInstance.CODEC; }
        @Override protected MutableComponent typeDescription() { return Component.literal("Dispatcher lifecycle"); }
    }

    private static final class PayloadEffectsTest extends Test {
        PayloadEffectsTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            List<Mob> joined = new ArrayList<>();
            Consumer<EntityJoinLevelEvent> listener = event -> {
                if (event.getLevel() == helper.getLevel() && event.getEntity().getType() == ModEntities.RUPTER.get()) {
                    Mob mob = (Mob) event.getEntity();
                    helper.assertFalse(mob.hasEffect(MobEffects.SPEED), "effects applied before join event");
                    joined.add(mob);
                }
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, listener);
            try (Scene scene = new Scene(helper)) {
                List<EntityType<NexusParasiteEntity>> stages = List.of(ModEntities.DISPATCHER_SI.get(),
                        ModEntities.DISPATCHER_SII.get(), ModEntities.DISPATCHER_SIII.get(), ModEntities.DISPATCHER_SIV.get());
                for (int stage = 1; stage <= 4; stage++) {
                    NexusParasiteEntity parent = scene.spawn(stages.get(stage - 1));
                    DeterrentParasiteEntity tentacle = scene.tentacle(parent, stage, Identifier.parse("csrp:rupter"));
                    if (stage == 4) tentacle.igniteForSeconds(8.0F);
                    tickTentacle(tentacle, 60);
                    helper.assertTrue(joined.size() == stage - 1, "payload released at or before tick 60");
                    tickTentacle(tentacle, 1);
                    helper.assertTrue(joined.size() == stage, "payload not released at tick 61");
                    Mob payload = joined.get(stage - 1);
                    scene.entities.add(payload);
                    helper.assertTrue(tentacle.isAlive() && field(tentacle, "dispatchEntityId") == null,
                            "successful deployment discarded tentacle or retained payload");
                    helper.assertTrue(payload.hasEffect(MobEffects.SPEED)
                                    && payload.getEffect(MobEffects.SPEED).getDuration() == stage * 100
                                    && payload.getEffect(MobEffects.SPEED).getAmplifier() == stage - 1,
                            "wrong stage speed effect " + stage);
                    helper.assertTrue(payload.hasEffect(ModMobEffects.LINK) && payload.hasEffect(ModMobEffects.DEBAR)
                                    && payload.hasEffect(ModMobEffects.SENSES), "missing Dispatcher effects at stage " + stage);
                    if (stage == 4) {
                        helper.assertTrue(payload.isOnFire() && payload.getHealth() == payload.getMaxHealth() * 0.5F,
                                "burning tentacle did not transfer fire and half health");
                    }
                    tickTentacle(tentacle, 1);
                    helper.assertTrue(joined.size() == stage, "payload deployed more than once");
                    tentacle.die(helper.getLevel().damageSources().generic());
                    helper.assertTrue(inventory(parent).isEmpty(), "released payload returned on tentacle death");
                }
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                for (Mob mob : joined) mob.discard();
            }
        }
    }

    private static final class DeathAndFailureTest extends Test {
        DeathAndFailureTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            List<Mob> canceled = new ArrayList<>();
            Consumer<EntityJoinLevelEvent> listener = event -> {
                if (event.getLevel() == helper.getLevel() && event.getEntity().getType() == ModEntities.RUPTER.get()) {
                    canceled.add((Mob) event.getEntity());
                    event.setCanceled(true);
                }
            };
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity parent = scene.spawn(ModEntities.DISPATCHER_SII.get());
                NexusParasiteEntity other = scene.spawn(ModEntities.DISPATCHER_SI.get());
                DeterrentParasiteEntity killed = scene.tentacle(parent, 2, Identifier.parse("csrp:sim_cow"));
                killed.die(helper.getLevel().damageSources().generic());
                killed.die(helper.getLevel().damageSources().generic());
                helper.assertTrue(inventory(parent).equals(List.of("csrp:sim_cow")) && inventory(other).isEmpty(),
                        "tentacle death did not return the pending payload exactly once to its parent");

                DeterrentParasiteEntity orphan = scene.tentacle(other, 1, Identifier.parse("csrp:rupter"));
                other.discard();
                orphan.die(helper.getLevel().damageSources().generic());
                helper.assertTrue(inventory(other).isEmpty() && inventory(parent).size() == 1,
                        "dead parent or unrelated Dispatcher received payload");

                DeterrentParasiteEntity invalid = scene.tentacle(parent, 2, Identifier.parse("csrp:not_registered"));
                tickTentacle(invalid, 65);
                helper.assertTrue(invalid.isAlive() && field(invalid, "dispatchEntityId") != null,
                        "invalid payload was consumed or discarded");
                invalid.die(helper.getLevel().damageSources().generic());
                helper.assertTrue(inventory(parent).contains("csrp:not_registered"), "failed payload was not returned");

                NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, listener);
                DeterrentParasiteEntity tentacle = scene.tentacle(parent, 2, Identifier.parse("csrp:rupter"));
                tickTentacle(tentacle, 61);
                helper.assertTrue(canceled.size() == 1 && canceled.get(0).hasEffect(MobEffects.SPEED)
                                && field(tentacle, "dispatchEntityId") == null && tentacle.isAlive(),
                        "canceled join did not preserve original post-join effects and consumption order");
                int before = inventory(parent).size();
                tentacle.die(helper.getLevel().damageSources().generic());
                helper.assertTrue(inventory(parent).size() == before, "canceled join caused a second payload return");
                DeterrentParasiteEntity protectedTentacle = scene.tentacle(parent, 2, Identifier.parse("csrp:rupter"));
                Consumer<LivingDeathEvent> cancelDeath = event -> {
                    if (event.getEntity() == protectedTentacle) event.setCanceled(true);
                };
                NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, cancelDeath);
                try {
                    protectedTentacle.die(helper.getLevel().damageSources().generic());
                    helper.assertTrue(inventory(parent).size() == before && field(protectedTentacle, "dispatchEntityId") != null,
                            "canceled death returned the payload");
                } finally {
                    NeoForge.EVENT_BUS.unregister(cancelDeath);
                }
                protectedTentacle.die(helper.getLevel().damageSources().generic());
                helper.assertTrue(inventory(parent).size() == before + 1,
                        "later uncanceled death did not return the payload");
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                for (Mob mob : canceled) mob.discard();
            }
        }
    }

    private static final class ReloadTest extends Test {
        ReloadTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity parent = scene.spawn(ModEntities.DISPATCHER_SII.get());
                DeterrentParasiteEntity original = scene.tentacle(parent, 2, Identifier.parse("csrp:rupter"));
                tickTentacle(original, 25);
                CompoundTag tag = save(original);
                DeterrentParasiteEntity restored = ModEntities.DISPATCHERTEN.get().create(
                        helper.getLevel(), EntitySpawnReason.LOAD);
                helper.assertTrue(restored != null, "could not create reloaded tentacle");
                restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
                helper.assertTrue(field(restored, "dispatchEntityId") == null && field(restored, "dispatchOwner") == null
                                && (boolean) field(restored, "dispatchPeek") && (int) field(restored, "lifetimeTicks") == 0,
                        "reload did not retain peek while resetting age and clearing transient payload and parent");
                restored.die(helper.getLevel().damageSources().generic());
                helper.assertTrue(inventory(parent).isEmpty(), "reloaded tentacle returned a transient payload");
                restored.discard();
                helper.succeed();
            }
        }
    }

    private static final class DespawnTest extends Test {
        DespawnTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            List<Mob> payloads = new ArrayList<>();
            List<GoreEntity> gore = new ArrayList<>();
            Mob[] current = {null};
            MobDespawnEvent.Result[] result = {MobDespawnEvent.Result.DENY};
            NexusParasiteEntity[] owner = {null};
            Consumer<EntityJoinLevelEvent> join = event -> {
                if (event.getLevel() != helper.getLevel()) return;
                if (event.getEntity().getType() == ModEntities.BUGLIN.get()) {
                    payloads.add((Mob) event.getEntity());
                } else if (event.getEntity() instanceof GoreEntity cyst && current[0] != null) {
                    helper.assertTrue(current[0].isAlive() && inventory(owner[0]).isEmpty(),
                            "cyst was not spawned before parent storage and removal");
                    gore.add(cyst);
                }
            };
            Consumer<MobDespawnEvent> despawn = event -> {
                if (event.getEntity() == current[0]) event.setResult(result[0]);
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, join);
            NeoForge.EVENT_BUS.addListener(MobDespawnEvent.class, despawn);
            try (Scene scene = new Scene(helper)) {
                owner[0] = scene.spawn(ModEntities.DISPATCHER_SI.get());
                NexusParasiteEntity nearby = scene.spawn(ModEntities.DISPATCHER_SI.get());
                DeterrentParasiteEntity tentacle = scene.tentacle(owner[0], 1, Identifier.parse("csrp:buglin"));
                tickTentacle(tentacle, 61);
                helper.assertTrue(payloads.size() == 1, "test payload was not deployed");
                Mob mob = payloads.get(0);
                current[0] = mob;
                BlockPos collected = scene.origin.offset(4, 0, 0);
                BlockState previous = helper.getLevel().getBlockState(collected);
                try {
                    helper.getLevel().setBlockAndUpdate(collected, Blocks.DIRT.defaultBlockState());
                    helper.assertTrue(ParasiteBlockInventory.collect(helper.getLevel(), collected, mob), "could not collect block");
                    mob.removeWhenFarAway(100_000.0D);
                    mob.checkDespawn();
                    helper.assertTrue(mob.isAlive() && gore.isEmpty() && inventory(owner[0]).isEmpty()
                                    && mob.getPersistentData().contains(ParasiteBlockInventory.TAG),
                            "query or denied despawn produced side effects");
                    result[0] = MobDespawnEvent.Result.ALLOW;
                    mob.checkDespawn();
                    helper.assertTrue(mob.isRemoved() && gore.size() == 1 && gore.get(0).storedItemStacks() == 1,
                            "allowed despawn did not launch the stored block inventory");
                    helper.assertTrue(inventory(owner[0]).equals(List.of("csrp:rupter")) && inventory(nearby).isEmpty(),
                            "despawn did not use original parent's stage conversion");
                } finally {
                    helper.getLevel().setBlockAndUpdate(collected, previous);
                }
                current[0] = null;
                Mob unrelated = scene.spawn(ModEntities.RUPTER.get());
                current[0] = unrelated;
                result[0] = MobDespawnEvent.Result.ALLOW;
                unrelated.checkDespawn();
                helper.assertTrue(unrelated.isRemoved() && inventory(nearby).isEmpty()
                                && inventory(owner[0]).equals(List.of("csrp:rupter")) && gore.size() == 1,
                        "unowned empty parasite was stored nearby or spawned a cyst");
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(join);
                NeoForge.EVENT_BUS.unregister(despawn);
                for (Mob mob : payloads) mob.discard();
                for (GoreEntity entity : gore) entity.discard();
            }
        }
    }

    private static final class DistanceDespawnTest extends Test {
        DistanceDespawnTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            List<Mob> payloads = new ArrayList<>();
            Consumer<EntityJoinLevelEvent> join = event -> {
                if (event.getLevel() == helper.getLevel() && event.getEntity().getType() == ModEntities.BUGLIN.get()) {
                    payloads.add((Mob) event.getEntity());
                }
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, join);
            ServerPlayer player = null;
            try (Scene scene = new Scene(helper)) {
                NexusParasiteEntity parent = scene.spawn(ModEntities.DISPATCHER_SI.get());
                DeterrentParasiteEntity first = scene.tentacle(parent, 1, Identifier.parse("csrp:buglin"));
                DeterrentParasiteEntity second = scene.tentacle(parent, 1, Identifier.parse("csrp:buglin"));
                tickTentacle(first, 61);
                tickTentacle(second, 61);
                helper.assertTrue(payloads.size() == 2, "could not deploy distance test parasites");
                player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
                new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND),
                        player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                    @Override public void send(Packet<?> packet) { }
                };
                player.snapTo(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);
                helper.getLevel().addNewPlayer(player);
                helper.assertTrue(helper.getLevel().getNearestPlayer(parent, -1.0D) == player,
                        "mock player was not visible to distance checks");

                Mob distant = payloads.get(0);
                distant.setPos(player.getX() + 200.0D, player.getY(), player.getZ());
                distant.checkDespawn();
                helper.assertTrue(distant.isRemoved() && inventory(parent).equals(List.of("csrp:rupter")),
                        "instant distance despawn did not return to the deploying parent");

                Mob idle = payloads.get(1);
                idle.setPos(player.getX() + 64.0D, player.getY(), player.getZ());
                idle.setNoActionTime(601);
                long seed = 0;
                while (RandomSource.create(seed).nextInt(800) != 0) seed++;
                idle.getRandom().setSeed(seed);
                idle.checkDespawn();
                helper.assertTrue(idle.isRemoved() && inventory(parent).equals(List.of("csrp:rupter", "csrp:rupter")),
                        "random idle distance despawn did not return to the deploying parent");
                helper.succeed();
            } finally {
                if (player != null) helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                NeoForge.EVENT_BUS.unregister(join);
                for (Mob mob : payloads) mob.discard();
            }
        }
    }

    private static final class ServerTickTest extends Test {
        ServerTickTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            Scene scene = new Scene(helper);
            NexusParasiteEntity parent = scene.spawn(ModEntities.DISPATCHER_SI.get());
            DeterrentParasiteEntity tentacle = scene.tentacle(parent, 1, Identifier.parse("csrp:buglin"));
            List<Mob> payloads = new ArrayList<>();
            Consumer<EntityJoinLevelEvent> join = event -> {
                if (event.getLevel() == helper.getLevel() && event.getEntity().getType() == ModEntities.BUGLIN.get()
                        && event.getEntity().distanceToSqr(tentacle) < 4.0D) {
                    payloads.add((Mob) event.getEntity());
                }
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, join);
            boolean[] premature = {false};
            helper.runAtTickTime(55, () -> premature[0] = !payloads.isEmpty());
            helper.runAtTickTime(75, () -> {
                try {
                    helper.assertTrue(!premature[0] && payloads.size() == 1 && tentacle.isAlive()
                                    && payloads.get(0).hasEffect(MobEffects.SPEED),
                            "normal server ticks did not deploy one buffed payload while keeping the tentacle alive");
                    helper.succeed();
                } finally {
                    NeoForge.EVENT_BUS.unregister(join);
                    for (Mob mob : payloads) mob.discard();
                    scene.close();
                }
            });
        }
    }

    private static final class TeleportTest extends Test {
        TeleportTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

        @Override
        public void run(GameTestHelper helper) {
            try (Scene scene = new Scene(helper)) {
                Mob payload = scene.spawn(ModEntities.RUPTER.get());
                payload.setPos(payload.getX() + 3.0D, payload.getY(), payload.getZ());
                payload.setDeltaMovement(new Vec3(0.2D, 0.1D, 0.3D));
                DeterrentParasiteEntity tentacle = scene.spawn(ModEntities.DISPATCHERTEN.get());
                tentacle.setYRot(45.0F);
                tentacle.setXRot(12.0F);
                tentacle.setDispatchTarget(payload);
                tentacle.igniteForSeconds(8.0F);
                tickTentacle(tentacle, 60);
                helper.assertTrue(payload.distanceToSqr(tentacle) > 1.0D, "teleport occurred too early");
                tickTentacle(tentacle, 1);
                helper.assertTrue(payload.distanceToSqr(tentacle) < 0.001D && tentacle.isAlive()
                                && !payload.isOnFire() && payload.getYRot() == 45.0F && payload.getXRot() == 12.0F
                                && payload.getDeltaMovement().equals(new Vec3(0.2D, 0.1D, 0.3D)),
                        "live teleport changed payload state or discarded tentacle");
                tickTentacle(tentacle, 59);
                helper.assertTrue(tentacle.isAlive() && !(boolean) field(tentacle, "dispatchRetreating"),
                        "empty tentacle retreated at or before tick 120");
                tickTentacle(tentacle, 1);
                helper.assertTrue((boolean) field(tentacle, "dispatchRetreating") && tentacle.isAlive(),
                        "empty tentacle did not begin retreat after tick 120");
                tickTentacle(tentacle, 52);
                helper.assertTrue(tentacle.isRemoved(), "tentacle did not finish retreat");
                helper.succeed();
            }
        }
    }
}
