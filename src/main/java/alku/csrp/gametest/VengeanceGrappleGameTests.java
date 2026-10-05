package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.event.BookOfVengeanceEvents;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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

/**
 * Runtime checks for the Book of Vengeance's ported data.
 *
 * <p>The grapple itself drives a player's velocity, which a GameTest cannot exercise (mock players cannot
 * receive the payloads the rest of the mod sends on login), so what is checked here is the part that is
 * observable without one: the five {@code VENGEANCE_*} sound events exist in the registry — the ogg files
 * and {@code sounds.json} entries were present for a long time while no sound event was — and the
 * knockback burst's distance falloff still computes the original's numbers.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class VengeanceGrappleGameTests {
    private static final String MODID = Csrp.MODID;
    private static final double EPSILON = 1.0E-6D;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    /** {@code SRPSounds}: the five sounds the grapple and the burst play. */
    private static final List<String> VENGEANCE_SOUNDS = List.of(
            "vengeance.attack.impact",
            "vengeance.attack.paper",
            "vengeance.attack.chain",
            "vengeance.attack.whoosh",
            "vengeance.attack.rock");

    private VengeanceGrappleGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "vengeance_grapple"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "vengeance_grapple/sound_events"),
                new SoundEventsTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "vengeance_grapple/knockback_falloff"),
                new KnockbackFalloffTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class SoundEventsTest extends GameTestInstance {
        SoundEventsTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            for (String id : VENGEANCE_SOUNDS) {
                Identifier key = Identifier.fromNamespaceAndPath(MODID, id);
                helper.assertTrue(BuiltInRegistries.SOUND_EVENT.getValue(key) != null,
                        Component.literal("sound event " + key + " is not registered"));
            }
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp vengeance sound events registered");
        }
    }

    /** {@code doKnockbackBurst}: push {@code 2.2*(0.45+0.55*falloff)}, lift {@code 0.4+0.15*falloff}. */
    private static final class KnockbackFalloffTest extends GameTestInstance {
        KnockbackFalloffTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            // Full strength on top of the caster, 0.45 of it at the 5 block edge and beyond.
            assertClose(helper, BookOfVengeanceEvents.knockbackPush(0.0D), 2.2D, "push at distance 0");
            assertClose(helper, BookOfVengeanceEvents.knockbackPush(5.0D), 0.99D, "push at distance 5");
            assertClose(helper, BookOfVengeanceEvents.knockbackPush(9.0D), 0.99D, "push beyond the radius");
            assertClose(helper, BookOfVengeanceEvents.knockbackPush(2.5D), 1.595D, "push at half falloff");
            assertClose(helper, BookOfVengeanceEvents.knockbackLift(0.0D), 0.55D, "lift at distance 0");
            assertClose(helper, BookOfVengeanceEvents.knockbackLift(5.0D), 0.4D, "lift at distance 5");
            assertClose(helper, BookOfVengeanceEvents.knockbackLift(2.5D), 0.475D, "lift at half falloff");
            helper.succeed();
        }

        private static void assertClose(GameTestHelper helper, double actual, double expected, String label) {
            helper.assertTrue(Math.abs(actual - expected) < EPSILON,
                    Component.literal(label + ": expected " + expected + " but got " + actual));
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp vengeance knockback falloff");
        }
    }
}
