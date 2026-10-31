package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 原版 Nexus 跟随距离及 Dispatcher 倍率的运行时验证。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class NexusFollowRangeGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 20;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private NexusFollowRangeGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "nexus_follow_ranges"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "nexus_follow_ranges/stage_defaults"),
                new StageDefaultsTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class StageDefaultsTest extends GameTestInstance {
        StageDefaultsTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            assertRangeAndGrowth(helper, ModEntities.BECKON_SI.get(), 16.0D, 4_800, 6_000, "Beckon stage I");
            assertRangeAndGrowth(helper, ModEntities.BECKON_SII.get(), 32.0D, 6_000, 7_200, "Beckon stage II");
            assertRangeAndGrowth(helper, ModEntities.BECKON_SIII.get(), 48.0D, 12_000, 24_000, "Beckon stage III");
            assertRangeAndGrowth(helper, ModEntities.BECKON_SIV.get(), 128.0D, -1, -1, "Beckon stage IV");
            assertRangeAndGrowth(helper, ModEntities.DISPATCHER_SI.get(), 32.0D, 4_800, 6_000, "Dispatcher stage I");
            assertRangeAndGrowth(helper, ModEntities.DISPATCHER_SII.get(), 64.0D, 6_000, 7_200, "Dispatcher stage II");
            assertRangeAndGrowth(helper, ModEntities.DISPATCHER_SIII.get(), 96.0D, 12_000, 24_000, "Dispatcher stage III");
            assertRangeAndGrowth(helper, ModEntities.DISPATCHER_SIV.get(), 256.0D, -1, -1, "Dispatcher stage IV");
            assertRangeAndGrowth(helper, ModEntities.ROOTER_SI.get(), 16.0D, 4_800, 6_000, "Rooter stage I");
            assertRangeAndGrowth(helper, ModEntities.ROOTER_SII.get(), 32.0D, 4_800, 6_000, "Rooter stage II");
            assertRangeAndGrowth(helper, ModEntities.ROOTER_SIII.get(), 48.0D, 4_800, 6_000, "Rooter stage III");
            assertRangeAndGrowth(helper, ModEntities.ROOTER_SIV.get(), 128.0D, -1, -1, "Rooter stage IV");
            assertRangeAndGrowth(helper, ModEntities.ROOTERBALL.get(), 16.0D, -1, -1, "Rooterball");
            assertRooterAttributes(helper, ModEntities.ROOTER_SI.get(), 25.0D, 4.0D, 2.5D, "Rooter stage I");
            assertRooterAttributes(helper, ModEntities.ROOTER_SII.get(), 60.0D, 8.0D, 36.0D, "Rooter stage II");
            assertRooterAttributes(helper, ModEntities.ROOTER_SIII.get(), 110.0D, 16.0D, 13.0D, "Rooter stage III");
            assertRooterAttributes(helper, ModEntities.ROOTER_SIV.get(), 220.0D, 25.0D, 20.0D, "Rooter stage IV");
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("SRP Nexus stage follow-range defaults");
        }
    }

    private static void assertRangeAndGrowth(GameTestHelper helper, EntityType<NexusParasiteEntity> type,
            double expectedRange, int minimumGrowth, int maximumGrowth, String description) {
        NexusParasiteEntity entity = type.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(entity != null, Component.literal(description + " could not be created"));
        AttributeInstance followRange = entity.getAttribute(Attributes.FOLLOW_RANGE);
        helper.assertTrue(followRange != null && Math.abs(followRange.getBaseValue() - expectedRange) < 0.0001D,
                Component.literal(description + " follow range was "
                        + (followRange == null ? "missing" : followRange.getBaseValue())
                        + ", expected " + expectedRange));
        int delay = entity.getGrowthDelayTicks();
        helper.assertTrue(minimumGrowth < 0 ? delay == minimumGrowth
                        : delay >= minimumGrowth && delay <= maximumGrowth,
                Component.literal(description + " growth delay was " + delay + ", expected "
                        + (minimumGrowth < 0 ? "disabled" : minimumGrowth + ".." + maximumGrowth)));
        entity.discard();
    }

    private static void assertRooterAttributes(GameTestHelper helper, EntityType<NexusParasiteEntity> type,
            double expectedHealth, double expectedArmor, double expectedDamage, String description) {
        NexusParasiteEntity entity = type.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(entity != null, Component.literal(description + " could not be created"));
        assertAttribute(helper, entity, Attributes.MAX_HEALTH, expectedHealth, description + " health");
        assertAttribute(helper, entity, Attributes.ARMOR, expectedArmor, description + " armor");
        assertAttribute(helper, entity, Attributes.ATTACK_DAMAGE, expectedDamage, description + " attack damage");
        entity.discard();
    }

    private static void assertAttribute(GameTestHelper helper, NexusParasiteEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double expected, String description) {
        AttributeInstance instance = entity.getAttribute(attribute);
        helper.assertTrue(instance != null && Math.abs(instance.getBaseValue() - expected) < 0.0001D,
                Component.literal(description + " was "
                        + (instance == null ? "missing" : instance.getBaseValue()) + ", expected " + expected));
    }
}
