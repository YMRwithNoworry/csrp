package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.entity.BombEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Runtime checks for the 1.10.9 stationary architect retaliation bombs. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class NexusRetaliationGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private NexusRetaliationGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY_STRUCTURE, 20, 1, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation/families_and_stages"),
                new FamiliesTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation/gates_and_cooldown"),
                new GatesTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation/reload_cooldown"),
                new ReloadTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation/bomb_targets"),
                new BombTargetsTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation/rooter_cyst_split"),
                new CystSplitTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation/status_damage"),
                new StatusDamageTest(data));
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "nexus_retaliation/bomb_motion"),
                new BombMotionTest(data));
    }

    private static List<BombEntity> bombsFrom(GameTestHelper helper, NexusParasiteEntity owner) {
        return helper.getLevel().getEntitiesOfClass(BombEntity.class, owner.getBoundingBox().inflate(10.0D), bomb -> {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, bomb.registryAccess());
            bomb.saveWithoutId(output);
            return output.buildResult().getIntArray("owner")
                    .map(UUIDUtil::uuidFromIntArray).filter(owner.getUUID()::equals).isPresent();
        });
    }

    private static void clearBombs(GameTestHelper helper, NexusParasiteEntity owner) {
        for (BombEntity bomb : bombsFrom(helper, owner)) bomb.discard();
    }

    private static NexusParasiteEntity spawnNexus(GameTestHelper helper, EntityType<NexusParasiteEntity> type) {
        NexusParasiteEntity entity = helper.spawn(type, new BlockPos(0, 10, 0));
        entity.setNoAi(true);
        entity.setNoGravity(true);
        return entity;
    }

    private static Mob spawnAttacker(GameTestHelper helper, NexusParasiteEntity target, double distance) {
        Mob attacker = helper.spawn(EntityTypes.SHEEP, new BlockPos(0, 10, 0));
        attacker.setNoAi(true);
        attacker.setNoGravity(true);
        attacker.setPos(target.getX() + distance, target.getY(), target.getZ());
        return attacker;
    }

    private static final class FamiliesTest extends GameTestInstance {
        FamiliesTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            List<List<EntityType<NexusParasiteEntity>>> families = List.of(
                    List.of(ModEntities.BECKON_SI.get(), ModEntities.BECKON_SII.get(),
                            ModEntities.BECKON_SIII.get(), ModEntities.BECKON_SIV.get()),
                    List.of(ModEntities.DISPATCHER_SI.get(), ModEntities.DISPATCHER_SII.get(),
                            ModEntities.DISPATCHER_SIII.get(), ModEntities.DISPATCHER_SIV.get()),
                    List.of(ModEntities.ROOTER_SI.get(), ModEntities.ROOTER_SII.get(),
                            ModEntities.ROOTER_SIII.get(), ModEntities.ROOTER_SIV.get()));
            float[][] damages = {{2.5F, 36.0F, 13.0F, 20.0F}, {3.0F, 7.0F, 14.0F, 22.0F},
                    {2.5F, 2.5F, 2.5F, 2.5F}};
            float[][] heights = {{1.4F, 2.7F, 4.9F, 8.1F}, {2.3F, 3.4F, 4.6F, 2.5F},
                    {1.4F, 1.4F, 1.4F, 1.4F}};
            for (int family = 0; family < families.size(); family++) {
                for (int index = 0; index < 4; index++) {
                    int stage = index + 1;
                    NexusParasiteEntity nexus = spawnNexus(helper, families.get(family).get(index));
                    Mob attacker = spawnAttacker(helper, nexus, 3.0D);
                    String label = "family " + family + " stage " + stage;
                    try {
                        boolean hit = nexus.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F);
                        helper.assertTrue(hit, label + " did not take the test hit");
                        List<BombEntity> bombs = bombsFrom(helper, nexus);
                        helper.assertTrue(bombs.size() == stage * 2,
                                label + " created " + bombs.size() + " bombs instead of " + stage * 2);
                        for (BombEntity bomb : bombs) {
                            helper.assertTrue(bomb.getFuse() == 60 && bomb.getStrength() == 0.0F
                                            && bomb.getSkin() == 1 && !bomb.canGrief(),
                                    label + " bomb has incorrect fuse, strength, skin or grief flag");
                            helper.assertTrue(bomb.getDamage() == damages[family][index]
                                            && bomb.getRangeRadius() == stage,
                                    label + " bomb has incorrect damage or radius");
                            helper.assertTrue(Math.abs(bomb.getY() - (nexus.getY() + heights[family][index] - 0.1F)) < 0.0001D,
                                    label + " bomb spawned at the wrong height");
                            helper.assertTrue(Double.isFinite(bomb.getDeltaMovement().x)
                                            && Double.isFinite(bomb.getDeltaMovement().y)
                                            && Double.isFinite(bomb.getDeltaMovement().z)
                                            && Math.abs(bomb.getDeltaMovement().x) <= 8.251D
                                            && Math.abs(bomb.getDeltaMovement().z) <= 8.251D
                                            && bomb.getDeltaMovement().y >= -stage * 24.751D
                                            && bomb.getDeltaMovement().y <= stage * 0.2D + 0.000001D,
                                    label + " bomb velocity is outside the original launch bounds");
                        }
                    } finally {
                        clearBombs(helper, nexus);
                        attacker.discard();
                        nexus.discard();
                    }
                }
            }
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Nexus retaliation bomb parameters across families and stages");
        }
    }

    private static final class GatesTest extends GameTestInstance {
        GatesTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity nexus = spawnNexus(helper, ModEntities.DISPATCHER_SIV.get());
            Mob attacker = spawnAttacker(helper, nexus, 6.001D);
            try {
                helper.assertTrue(nexus.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F),
                        "environmental damage did not reach the Nexus");
                helper.assertTrue(bombsFrom(helper, nexus).isEmpty(), "environmental damage triggered bombs");
                nexus.damageCooldownTime = 0;
                helper.assertTrue(nexus.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F),
                        "distant attacker did not damage the Nexus");
                helper.assertTrue(bombsFrom(helper, nexus).isEmpty(), "an attacker beyond six blocks triggered bombs");
                attacker.setPos(nexus.getX() + 6.0D, nexus.getY(), nexus.getZ());
                nexus.damageCooldownTime = 0;
                helper.assertTrue(nexus.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F),
                        "attacker at the six-block boundary did not damage the Nexus");
                helper.assertTrue(bombsFrom(helper, nexus).size() == 8, "six-block boundary did not trigger eight bombs");
                clearBombs(helper, nexus);
                for (int tick = 0; tick < 79; tick++) nexus.tick();
                nexus.damageCooldownTime = 0;
                helper.assertTrue(nexus.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F),
                        "cooldown test hit did not reach the Nexus");
                helper.assertTrue(bombsFrom(helper, nexus).isEmpty(), "bomb cooldown expired before 80 ticks");
                nexus.tick();
                nexus.damageCooldownTime = 0;
                helper.assertTrue(nexus.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F),
                        "post-cooldown hit did not reach the Nexus");
                helper.assertTrue(bombsFrom(helper, nexus).size() == 8, "bomb cooldown did not expire at 80 ticks");
                helper.succeed();
            } finally {
                clearBombs(helper, nexus);
                attacker.discard();
                nexus.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Nexus retaliation damage, distance and cooldown gates");
        }
    }

    private static final class ReloadTest extends GameTestInstance {
        ReloadTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity nexus = spawnNexus(helper, ModEntities.DISPATCHER_SIII.get());
            Mob attacker = spawnAttacker(helper, nexus, 3.0D);
            NexusParasiteEntity loaded = null;
            try {
                helper.assertTrue(nexus.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F),
                        "initial hit did not reach the Nexus");
                helper.assertTrue(bombsFrom(helper, nexus).size() == 6, "initial retaliation did not fire");
                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, nexus.registryAccess());
                nexus.saveWithoutId(output);
                CompoundTag saved = output.buildResult();
                helper.assertFalse(saved.contains("nexus_bomb_cooldown"), "retaliation cooldown was saved");
                loaded = ModEntities.DISPATCHER_SIII.get().create(helper.getLevel(), EntitySpawnReason.STRUCTURE);
                helper.assertTrue(loaded != null, "could not create saved Nexus");
                loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), saved));
                loaded.setUUID(UUID.randomUUID());
                loaded.damageCooldownTime = 0;
                helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "could not add saved Nexus");
                helper.assertTrue(loaded.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(attacker), 2.0F),
                        "hit on reloaded Nexus failed");
                helper.assertTrue(bombsFrom(helper, loaded).size() == 6,
                        "retaliation cooldown did not reset after reload");
                helper.succeed();
            } finally {
                clearBombs(helper, nexus);
                if (loaded != null) {
                    clearBombs(helper, loaded);
                    loaded.discard();
                }
                attacker.discard();
                nexus.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Nexus retaliation cooldown resets on reload");
        }
    }

    private static final class BombTargetsTest extends GameTestInstance {
        BombTargetsTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity nexus = spawnNexus(helper, ModEntities.DISPATCHER_SI.get());
            Mob marked = spawnAttacker(helper, nexus, 1.5D);
            PrimitiveParasiteEntity parasite = helper.spawn(ModEntities.PRI_LONGARMS.get(), new BlockPos(0, 10, 0));
            parasite.setNoAi(true);
            parasite.setNoGravity(true);
            parasite.setPos(nexus.getX() - 1.5D, nexus.getY(), nexus.getZ());
            BombEntity bomb = helper.spawn(ModEntities.BOMB.get(), new BlockPos(0, 10, 0));
            bomb.configure(nexus, 1, 0.0F, 4.0F, 2, 1, false);
            bomb.setNoGravity(true);
            bomb.setPos(nexus.getX(), nexus.getY() + 1.0D, nexus.getZ());
            marked.addEffect(new MobEffectInstance(ModMobEffects.THE_SIGN, 200));
            marked.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 310, 1));
            try {
                float markedHealth = marked.getHealth();
                float parasiteHealth = parasite.getHealth();
                bomb.tick();
                helper.assertTrue(marked.getHealth() < markedHealth && marked.hasEffect(ModMobEffects.VIRAL),
                        "bomb did not damage and infect the marked non-parasite");
                helper.assertTrue(marked.getEffect(ModMobEffects.VIRAL).getAmplifier() == 2
                                && marked.getEffect(ModMobEffects.VIRAL).getDuration() == 320,
                        "bomb did not stack an existing Viral effect");
                helper.assertTrue(marked.getLastDamageSource() != null
                                && marked.getLastDamageSource().getDirectEntity() == bomb
                                && marked.getLastDamageSource().getEntity() == nexus,
                        "bomb damage did not preserve the projectile and owner attribution");
                helper.assertTrue(parasite.getHealth() == parasiteHealth && !parasite.hasEffect(ModMobEffects.VIRAL),
                        "bomb affected another parasite");
                helper.succeed();
            } finally {
                bomb.discard();
                marked.discard();
                parasite.discard();
                nexus.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Original bomb target filtering and projectile attribution");
        }
    }

    private static final class BombMotionTest extends GameTestInstance {
        BombMotionTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            BombEntity bomb = ModEntities.BOMB.get().create(helper.getLevel(), EntitySpawnReason.STRUCTURE);
            helper.assertTrue(bomb != null, "could not create bomb");
            try {
                for (boolean negative : new boolean[]{false, true}) {
                    long seed = 1234L;
                    RandomSource expectedRandom = RandomSource.create(seed);
                    double xFactor = expectedRandom.nextDouble() * 2.0D - 1.0D;
                    double zFactor = expectedRandom.nextDouble() * 2.0D - 1.0D;
                    bomb.getRandom().setSeed(seed);
                    bomb.setBeckonMotion(negative ? -2.0D : 2.0D, negative ? -3.0D : 3.0D,
                            negative ? -4.0D : 4.0D, 0.35D, 0.2D);
                    helper.assertTrue(Math.abs(bomb.getDeltaMovement().x - (negative ? -2.0D : 0.35D) * xFactor) < 1.0E-9D
                                    && Math.abs(bomb.getDeltaMovement().y - (negative ? -3.0D : 0.2D)) < 1.0E-9D
                                    && Math.abs(bomb.getDeltaMovement().z - (negative ? -4.0D : 0.35D) * zFactor) < 1.0E-9D,
                            "bomb velocity did not preserve the original one-sided caps");
                }
                helper.succeed();
            } finally {
                bomb.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Original one-sided bomb motion caps");
        }
    }

    private static final class StatusDamageTest extends GameTestInstance {
        StatusDamageTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            List<EntityType<NexusParasiteEntity>> types = List.of(
                    ModEntities.BECKON_SI.get(), ModEntities.BECKON_SIV.get(),
                    ModEntities.DISPATCHER_SI.get(), ModEntities.DISPATCHER_SIV.get(),
                    ModEntities.ROOTER_SI.get(), ModEntities.ROOTERBALL.get());
            for (EntityType<NexusParasiteEntity> type : types) {
                NexusParasiteEntity nexus = spawnNexus(helper, type);
                try {
                    nexus.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
                    for (int status : new int[]{0, 1, 3}) {
                        for (boolean voidDamage : new boolean[]{false, true}) {
                            nexus.setParasiteStatus(status);
                            nexus.setHealth(nexus.getMaxHealth());
                            nexus.damageCooldownTime = 0;
                            float healthBefore = nexus.getHealth();
                            var source = voidDamage ? helper.getLevel().damageSources().fellOutOfWorld()
                                    : helper.getLevel().damageSources().inWall();
                            helper.assertTrue(nexus.hurtServer(helper.getLevel(), source, 10.0F),
                                    type + " status " + status + " did not take damage");
                            float expected = type != ModEntities.ROOTERBALL.get() && status == 0 ? 4.0F : 10.0F;
                            float actual = healthBefore - nexus.getHealth();
                            helper.assertTrue(Math.abs(actual - expected) < 0.0001F,
                                    type + " status " + status + " damage " + actual + " != " + expected);
                        }
                    }
                } finally {
                    nexus.discard();
                }
            }
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Nexus status damage reduction and Rootmass Cyst exception");
        }
    }

    private static final class CystSplitTest extends GameTestInstance {
        CystSplitTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            NexusParasiteEntity rooter = spawnNexus(helper, ModEntities.ROOTER_SI.get());
            NexusParasiteEntity first = spawnNexus(helper, ModEntities.ROOTERBALL.get());
            NexusParasiteEntity second = spawnNexus(helper, ModEntities.ROOTERBALL.get());
            first.setPos(rooter.getX() + 3.0D, rooter.getY(), rooter.getZ());
            second.setPos(rooter.getX() - 3.0D, rooter.getY(), rooter.getZ());
            try {
                float rooterHealth = rooter.getHealth();
                float firstHealth = first.getHealth();
                float secondHealth = second.getHealth();
                rooter.setParasiteStatus(0);
                rooter.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fellOutOfWorld(), 8.0F);
                helper.assertTrue(rooter.getHealth() == rooterHealth, "protected Rooter lost health");
                float firstLoss = firstHealth - first.getHealth();
                float secondLoss = secondHealth - second.getHealth();
                helper.assertTrue(Math.abs(firstLoss - 4.0F) < 0.0001F && Math.abs(secondLoss - 4.0F) < 0.0001F,
                        "Rootmass Cysts did not each receive half the unreduced incoming damage");
                helper.succeed();
            } finally {
                clearBombs(helper, rooter);
                first.discard();
                second.discard();
                rooter.discard();
            }
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Rooter damage is divided among nearby Rootmass Cysts");
        }
    }
}
