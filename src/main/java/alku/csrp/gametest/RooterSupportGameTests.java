package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.config.MobsConfig;
import alku.csrp.entity.AbominationEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.entity.RupterEntity;
import alku.csrp.event.StatusEffectEvents;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Runtime coverage for the legacy Rooter support and Pivot contracts. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class RooterSupportGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private RooterSupportGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "rooter_support"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY_STRUCTURE, 20, 1, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "rooter_support/cycle_and_targets"),
                new CycleTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "rooter_support/pivot_and_persistence"),
                new PivotTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "rooter_support/gene_types"),
                new GeneTypesTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "rooter_support/cross_chunk"),
                new CrossChunkTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "rooter_support/cyst_capacity_and_dying"),
                new CystCapacityTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "rooter_support/canceled_cyst_join"),
                new CanceledCystJoinTest(data));
    }

    private static final class CycleTest extends GameTestInstance {
        CycleTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            int[] cystRanges = {16, 32, 48, 128};
            int[] effectRanges = {20, 30, 40, 56};
            int[] cooldowns = {12, 10, 8, 6};
            int[] limits = {3, 4, 5, 6};
            for (int stage = 1; stage <= 4; stage++) {
                int index = stage - 1;
                helper.assertTrue(MobsConfig.rooterCystRange(stage) == cystRanges[index], "cyst range mismatch");
                helper.assertTrue(MobsConfig.rooterEffectRange(stage) == effectRanges[index], "support range mismatch");
                helper.assertTrue(MobsConfig.rooterSupportCooldown(stage) == cooldowns[index], "cooldown mismatch");
                helper.assertTrue(MobsConfig.rooterCystLimit(stage) == limits[index], "cyst limit mismatch");
            }
            NexusParasiteEntity rooter = helper.spawn(ModEntities.ROOTER_SI.get(), new BlockPos(0, 5, 0));
            NexusParasiteEntity otherRooter = helper.spawn(ModEntities.ROOTER_SII.get(), new BlockPos(0, 9, 0));
            NexusParasiteEntity cyst = helper.spawn(ModEntities.ROOTERBALL.get(), new BlockPos(0, 13, 0));
            PrimitiveParasiteEntity ally = helper.spawn(ModEntities.PRI_LONGARMS.get(), new BlockPos(0, 16, 0));
            Mob animal = helper.spawn(EntityTypes.SHEEP, new BlockPos(0, 19, 0));
            for (Mob mob : List.of(rooter, otherRooter, cyst, ally, animal)) {
                mob.setNoGravity(true);
                mob.setNoAi(true);
            }
            for (int i = 0; i < 9; i++) rooter.tick();
            helper.assertFalse(ally.hasEffect(ModMobEffects.PIVOT), "support fired before cycle tick 10");
            rooter.tick();
            helper.assertTrue(ally.hasEffect(ModMobEffects.PIVOT) && ally.hasEffect(ModMobEffects.PARATE),
                    "support did not apply both effects at cycle tick 10: rooter=" + rooter.position()
                            + ", ally=" + ally.position() + ", alive=" + ally.isAlive()
                            + ", pivot=" + ally.hasEffect(ModMobEffects.PIVOT)
                            + ", parate=" + ally.hasEffect(ModMobEffects.PARATE));
            helper.assertTrue(ally.getEffect(ModMobEffects.PIVOT).getAmplifier() == 0, "wrong stage I amplifier");
            helper.assertFalse(otherRooter.hasEffect(ModMobEffects.PIVOT), "another Rooter received Pivot");
            helper.assertFalse(cyst.hasEffect(ModMobEffects.PIVOT), "a Rootmass Cyst received support");
            helper.assertFalse(animal.hasEffect(ModMobEffects.PIVOT), "a non-parasite received support");
            EvolutionSystem.GenerationProfile genes = EvolutionSystem.generationProfile(helper.getLevel(), ally);
            helper.assertTrue(genes.minimumDamage() && genes.damageCap() && genes.lookWalls()
                            && genes.sprinting() && genes.waterLeap() && genes.specialMoves()
                            && genes.poisonHealing() == 2.5F && genes.mobHealing() == 3.0F
                            && genes.attackSpeedMultiplier() == 0.5F,
                    "Rooter did not apply its original gene values");
            ally.removeEffect(ModMobEffects.PIVOT);
            ally.removeEffect(ModMobEffects.PARATE);
            for (int i = 0; i < 251; i++) rooter.tick();
            helper.assertFalse(ally.hasEffect(ModMobEffects.PIVOT), "support cooldown expired too early");
            rooter.tick();
            helper.assertTrue(ally.hasEffect(ModMobEffects.PIVOT), "support did not repeat after 12 cycles");

            cyst.addEffect(new MobEffectInstance(ModMobEffects.PIVOT, 300, 0));
            helper.assertTrue(cyst.hasEffect(ModMobEffects.PIVOT), "cyst incorrectly rejected Pivot immediately");
            for (int i = 0; i < 5; i++) cyst.tick();
            helper.assertFalse(cyst.hasEffect(ModMobEffects.PIVOT), "cyst did not clear Pivot at cycle tick 5");
            for (Mob mob : List.of(rooter, otherRooter, cyst, ally, animal)) mob.discard();
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Rooter support cycle and target filtering");
        }
    }

    private static final class CystCapacityTest extends GameTestInstance {
        CystCapacityTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity rooter = helper.spawn(ModEntities.ROOTER_SI.get(), new BlockPos(0, 260, 0));
            NexusParasiteEntity dying = helper.spawn(ModEntities.ROOTERBALL.get(), new BlockPos(3, 260, 0));
            NexusParasiteEntity loaded = null;
            rooter.setNoAi(true);
            rooter.setNoGravity(true);
            dying.setNoAi(true);
            dying.setNoGravity(true);
            try {
                dying.setHealth(0.0F);
                helper.assertFalse(dying.isAlive() || dying.isRemoved(), "test cyst is not dying in the world");
                float healthBefore = rooter.getHealth();
                rooter.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fellOutOfWorld(), 8.0F);
                helper.assertTrue(rooter.getHealth() == healthBefore,
                        "a dying but not yet removed Rootmass Cyst did not intercept the hit");
                dying.discard();

                for (int i = 0; i < 3; i++) {
                    NexusParasiteEntity cyst = helper.spawn(ModEntities.ROOTERBALL.get(), new BlockPos(i + 3, 260, 0));
                    rooter.reserveTrackedSummon(cyst.getUUID(), 1);
                    cyst.discard();
                }
                helper.assertTrue(rooter.getUsedSummonCapacity() == 3, "test cyst reservations were not recorded");
                rooter.damageCooldownTime = 0;
                rooter.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fellOutOfWorld(), 1.0F);
                helper.assertTrue(rooter.getUsedSummonCapacity() == 3,
                        "Rooter reclaimed removed cyst capacity before reloading");
                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, rooter.registryAccess());
                rooter.saveWithoutId(output);
                loaded = ModEntities.ROOTER_SI.get().create(helper.getLevel(), EntitySpawnReason.STRUCTURE);
                helper.assertTrue(loaded != null, "could not create a reloaded Rooter");
                loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), output.buildResult()));
                helper.assertTrue(loaded.getUsedSummonCapacity() == 0,
                        "Rooter retained its original transient cyst capacity after reload");
                helper.succeed();
            } finally {
                if (loaded != null) loaded.discard();
                dying.discard();
                rooter.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Rootmass Cyst dying and transient capacity behavior");
        }
    }

    private static final class CanceledCystJoinTest extends GameTestInstance {
        CanceledCystJoinTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity rooter = helper.spawn(ModEntities.ROOTER_SI.get(), new BlockPos(0, 260, 0));
            rooter.setNoAi(true);
            rooter.setNoGravity(true);
            BlockPos floor = rooter.blockPosition().below();
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    helper.getLevel().setBlockAndUpdate(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState());
                }
            }
            int[] canceled = {0};
            Consumer<EntityJoinLevelEvent> rejectCyst = event -> {
                if (event.getLevel() == helper.getLevel()
                        && event.getEntity().getType() == ModEntities.ROOTERBALL.get()
                        && event.getEntity().distanceToSqr(rooter) <= 50.0D) {
                    canceled[0]++;
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, rejectCyst);
            try {
                rooter.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fellOutOfWorld(), 1.0F);
                helper.assertTrue(canceled[0] > 0, "no Rootmass Cyst join was attempted");
                helper.assertTrue(rooter.getUsedSummonCapacity() == canceled[0],
                        "a canceled cyst join did not retain its original capacity reservation");
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(NexusParasiteEntity.class,
                                rooter.getBoundingBox().inflate(6.0D),
                                entity -> entity.getType() == ModEntities.ROOTERBALL.get()).isEmpty(),
                        "a canceled cyst was added to the world");
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(rejectCyst);
                rooter.discard();
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        helper.getLevel().setBlockAndUpdate(floor.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Rootmass Cyst capacity after a canceled entity join");
        }
    }

    private static final class CrossChunkTest extends GameTestInstance {
        CrossChunkTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            BlockPos base = helper.absolutePos(new BlockPos(0, 80, 0));
            int boundaryX = Math.floorDiv(base.getX(), 16) * 16 + 15;
            BlockPos sourcePos = new BlockPos(boundaryX, base.getY(), base.getZ());
            BlockPos targetPos = sourcePos.east(2);
            helper.getLevel().getChunkAt(sourcePos);
            helper.getLevel().getChunkAt(targetPos);
            NexusParasiteEntity rooter = helper.spawn(ModEntities.ROOTER_SI.get(), new BlockPos(0, 80, 0));
            PrimitiveParasiteEntity ally = helper.spawn(ModEntities.PRI_LONGARMS.get(), new BlockPos(0, 80, 0));
            try {
                rooter.setNoGravity(true);
                rooter.setNoAi(true);
                ally.setNoGravity(true);
                ally.setNoAi(true);
                rooter.setPos(sourcePos.getX() + 0.5D, sourcePos.getY(), sourcePos.getZ() + 0.5D);
                ally.setPos(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
                helper.assertTrue(helper.getLevel().isLoaded(sourcePos) && helper.getLevel().isLoaded(targetPos),
                        "cross-chunk support requires both chunks loaded");
                for (int i = 0; i < 10; i++) rooter.tick();
                helper.assertTrue(ally.hasEffect(ModMobEffects.PIVOT) && ally.hasEffect(ModMobEffects.PARATE),
                        "Rooter did not support an ally across a loaded chunk boundary");
                helper.succeed();
            } finally {
                rooter.discard();
                ally.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Rooter support across a loaded chunk boundary");
        }
    }

    private static final class GeneTypesTest extends GameTestInstance {
        GeneTypesTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            SrpWorldData world = SrpWorldData.get(helper.getLevel());
            int oldGeneration = world.generation();
            boolean oldEveMode = world.eveMode();
            PrimitiveParasiteEntity malleable = helper.spawn(ModEntities.PRI_LONGARMS.get(), new BlockPos(0, 5, 0));
            PrimitiveParasiteEntity control = helper.spawn(ModEntities.PRI_LONGARMS.get(), new BlockPos(4, 5, 0));
            PrimitiveParasiteEntity basic = helper.spawn(ModEntities.HEED.get(), new BlockPos(8, 5, 0));
            PrimitiveParasiteEntity worm = helper.spawn(ModEntities.CARRIER_WORM.get(), new BlockPos(12, 5, 0));
            RupterEntity rupter = helper.spawn(ModEntities.RUPTER.get(), new BlockPos(16, 5, 0));
            try {
                world.setEveMode(false);
                world.setGeneration(0);
                EvolutionSystem.GenerationProfile before = EvolutionSystem.generationProfile(helper.getLevel(), malleable);
                helper.assertFalse(before.adaptation() || before.blockSearch() || before.residue() || before.ordinaryOrb(),
                        "generation zero unexpectedly enabled malleable genes");
                EvolutionSystem.applyRooterGenes(malleable);
                EvolutionSystem.applyRooterGenes(basic);
                EvolutionSystem.applyRooterGenes(worm);
                EvolutionSystem.applyRooterGenes(rupter);
                EvolutionSystem.GenerationProfile full = EvolutionSystem.generationProfile(helper.getLevel(), malleable);
                EvolutionSystem.GenerationProfile base = EvolutionSystem.generationProfile(helper.getLevel(), basic);
                helper.assertTrue(full.adaptation() && full.blockSearch() && full.residue() && full.ordinaryOrb(),
                        "Rooter did not enable all four malleable genes");
                helper.assertTrue(base.minimumDamage() && base.damageCap() && base.lookWalls()
                                && base.sprinting() && base.waterLeap() && base.specialMoves(),
                        "Rooter did not enable the six basic genes");
                helper.assertFalse(base.adaptation() || base.blockSearch() || base.residue() || base.ordinaryOrb(),
                        "Rooter enabled malleable-only genes on a basic parasite");
                EvolutionSystem.GenerationProfile wormGenes = EvolutionSystem.generationProfile(helper.getLevel(), worm);
                helper.assertFalse(worm.supportsDamageAdaptation() || wormGenes.adaptation() || wormGenes.blockSearch()
                                || wormGenes.residue() || wormGenes.ordinaryOrb(),
                        "Worm Carrier incorrectly received malleable-only genes");
                helper.assertFalse(rupter.getPersistentData().contains("csrp_rooter_gene_override"),
                        "Rupter must retain the original no-op applyGene override");
                helper.assertTrue(full.cothChance() == before.cothChance(), "Rooter altered unrelated COTH chance");

                var source = helper.getLevel().damageSources().generic();
                String damageId = PrimitiveParasiteEntity.damageTypeId(source);
                malleable.seedGlobalAdaptation(damageId, 1);
                control.seedGlobalAdaptation(damageId, 1);
                malleable.hurtServer(helper.getLevel(), source, 1.0F);
                control.hurtServer(helper.getLevel(), source, 1.0F);
                helper.assertTrue(malleable.getAdaptationHitStatus() > 0,
                        "Rooter gene did not enable damage adaptation at generation zero");
                helper.assertTrue(control.getAdaptationHitStatus() == 0,
                        "an unsupported parasite adapted at generation zero");
                helper.succeed();
            } finally {
                malleable.discard();
                control.discard();
                basic.discard();
                worm.discard();
                rupter.discard();
                world.setGeneration(oldGeneration);
                world.setEveMode(oldEveMode);
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Rooter genes distinguish basic and malleable parasites");
        }
    }

    private static final class PivotTest extends GameTestInstance {
        PivotTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            AbominationEntity rooter = helper.spawn(ModEntities.ABO_BODIES.get(), new BlockPos(0, 5, 0));
            PrimitiveParasiteEntity ally = helper.spawn(ModEntities.PRI_LONGARMS.get(), new BlockPos(3, 5, 0));
            rooter.setNoGravity(true);
            rooter.setNoAi(true);
            ally.setNoGravity(true);
            ally.setNoAi(true);
            StatusEffectEvents.linkToRooter(ally, rooter);
            EvolutionSystem.applyRooterGenes(ally);
            ally.addEffect(new MobEffectInstance(ModMobEffects.PIVOT, 300, 0));
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ally.registryAccess());
            ally.saveWithoutId(output);
            CompoundTag saved = output.buildResult();
            PrimitiveParasiteEntity loaded = ModEntities.PRI_LONGARMS.get().create(
                    helper.getLevel(), EntitySpawnReason.STRUCTURE);
            helper.assertTrue(loaded != null, "could not create NBT round-trip entity");
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved));
            helper.assertTrue(loaded.getPersistentData().getStringOr("csrp_rooter_owner_uuid", "")
                            .equals(rooter.getUUID().toString()), "Rooter UUID was not saved");
            helper.assertTrue(loaded.getPersistentData().getStringOr("csrp_rooter_owner_dimension", "")
                            .equals(helper.getLevel().dimension().identifier().toString()), "Rooter dimension was not saved");
            helper.assertTrue(EvolutionSystem.generationProfile(helper.getLevel(), loaded).lookWalls(),
                    "Rooter genes were not saved");
            float healthBefore = rooter.getHealth();
            for (int amplifier = 0; amplifier < 4; amplifier++) {
                loaded.removeEffect(ModMobEffects.PIVOT);
                loaded.addEffect(new MobEffectInstance(ModMobEffects.PIVOT, 300, amplifier));
                LivingIncomingDamageEvent event = incoming(loaded, helper, false);
                StatusEffectEvents.modifyEffectDamage(event);
                float expected = 10.0F * (1.0F - Math.min(0.95F, (amplifier + 1) * 0.2375F));
                helper.assertTrue(Math.abs(event.getAmount() - expected) < 0.0001F,
                        "wrong Pivot damage remaining at amplifier " + amplifier + ": " + event.getAmount());
            }
            helper.assertTrue(rooter.getHealth() < healthBefore, "Pivot did not forward damage to its Rooter");
            LivingIncomingDamageEvent fire = incoming(loaded, helper, true);
            StatusEffectEvents.modifyEffectDamage(fire);
            helper.assertTrue(fire.getAmount() == 10.0F, "ordinary fire damage must not be redirected");
            loaded.getPersistentData().putString("csrp_rooter_owner_dimension", "minecraft:the_nether");
            LivingIncomingDamageEvent otherDimension = incoming(loaded, helper, false);
            StatusEffectEvents.modifyEffectDamage(otherDimension);
            helper.assertTrue(otherDimension.getAmount() == 10.0F, "Pivot linked across dimensions");
            helper.assertFalse(loaded.getPersistentData().contains("csrp_rooter_owner_uuid")
                            || loaded.getPersistentData().contains("csrp_rooter_owner_dimension"),
                    "a cross-dimensional Rooter link was not cleared");
            StatusEffectEvents.clearDeadRooterLinks(new LivingDeathEvent(rooter, helper.getLevel().damageSources().generic()));
            helper.assertFalse(ally.getPersistentData().contains("csrp_rooter_owner_uuid"),
                    "Rooter death did not clear loaded allies' links");
            loaded.discard();
            ally.discard();
            rooter.discard();
            helper.succeed();
        }

        private static LivingIncomingDamageEvent incoming(LivingEntity entity, GameTestHelper helper, boolean fire) {
            return new LivingIncomingDamageEvent(entity, new DamageContainer(fire
                    ? helper.getLevel().damageSources().inFire() : helper.getLevel().damageSources().generic(), 10.0F));
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Pivot damage split and persistent Rooter ownership");
        }
    }
}
