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
            assertRange(helper, ModEntities.BECKON_SI.get(), 16.0D, "Beckon stage I");
            assertRange(helper, ModEntities.BECKON_SII.get(), 32.0D, "Beckon stage II");
            assertRange(helper, ModEntities.BECKON_SIII.get(), 48.0D, "Beckon stage III");
            assertRange(helper, ModEntities.BECKON_SIV.get(), 128.0D, "Beckon stage IV");
            assertRange(helper, ModEntities.DISPATCHER_SI.get(), 32.0D, "Dispatcher stage I");
            assertRange(helper, ModEntities.DISPATCHER_SII.get(), 64.0D, "Dispatcher stage II");
            assertRange(helper, ModEntities.DISPATCHER_SIII.get(), 96.0D, "Dispatcher stage III");
            assertRange(helper, ModEntities.DISPATCHER_SIV.get(), 256.0D, "Dispatcher stage IV");
            assertRange(helper, ModEntities.ROOTER_SI.get(), 16.0D, "Rooter stage I");
            assertRange(helper, ModEntities.ROOTER_SII.get(), 32.0D, "Rooter stage II");
            assertRange(helper, ModEntities.ROOTER_SIII.get(), 48.0D, "Rooter stage III");
            assertRange(helper, ModEntities.ROOTER_SIV.get(), 128.0D, "Rooter stage IV");
            assertRange(helper, ModEntities.ROOTERBALL.get(), 24.0D, "Rooterball");
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

    private static void assertRange(GameTestHelper helper, EntityType<NexusParasiteEntity> type,
            double expected, String description) {
        NexusParasiteEntity entity = type.create(helper.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(entity != null, Component.literal(description + " could not be created"));
        AttributeInstance followRange = entity.getAttribute(Attributes.FOLLOW_RANGE);
        helper.assertTrue(followRange != null && Math.abs(followRange.getBaseValue() - expected) < 0.0001D,
                Component.literal(description + " follow range was "
                        + (followRange == null ? "missing" : followRange.getBaseValue())
                        + ", expected " + expected));
        entity.discard();
    }
}
