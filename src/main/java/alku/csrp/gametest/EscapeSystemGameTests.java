package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.feature.EscapeSystem;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
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
 * Runtime tests for the escape system's streak rule (1.10.9 {@code EscapeOnDeathHandler}).
 *
 * <p>The rule is driven directly through {@link EscapeSystem#registerParasiteDeath(UUID, long)} rather
 * than through a mock player: NeoForge refuses custom payloads to GameTest mock players (their fake
 * connection never negotiates channels) and logging one in fires the celestial sync, so a player-based
 * test cannot run.  The rule itself — more than five deaths inside the one-minute window — is exactly
 * what the original computed and what the death handler consumes.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class EscapeSystemGameTests {
    private static final String MODID = Csrp.MODID;
    /** {@code EscapeOnDeathHandler.THRESHOLD}. */
    private static final int THRESHOLD = 5;
    /** {@code EscapeOnDeathHandler.WINDOW_MS}. */
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private EscapeSystemGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "escape_system"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "escape_system/streak_offer"),
                new StreakOfferTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "escape_system/window_expiry"),
                new WindowExpiryTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    /** The offer only appears once the deaths exceed the threshold inside the window. */
    private static final class StreakOfferTest extends GameTestInstance {
        StreakOfferTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            UUID player = UUID.randomUUID();
            for (int death = 1; death <= THRESHOLD; death++) {
                boolean offer = EscapeSystem.registerParasiteDeath(player, 1_000L * death);
                helper.assertTrue(!offer,
                        Component.literal("offer raised after only " + death + " deaths (threshold "
                                + THRESHOLD + ")"));
            }
            boolean offer = EscapeSystem.registerParasiteDeath(player, 1_000L * (THRESHOLD + 1));
            helper.assertTrue(offer,
                    Component.literal("offer missing after " + (THRESHOLD + 1) + " deaths inside the window"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp escape offer after repeated deaths");
        }
    }

    /** Deaths older than the window are dropped, so the streak never reaches the threshold. */
    private static final class WindowExpiryTest extends GameTestInstance {
        WindowExpiryTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            UUID player = UUID.randomUUID();
            long step = WINDOW_MS + 1L;
            boolean offer = false;
            for (int death = 0; death <= THRESHOLD; death++) {
                offer = EscapeSystem.registerParasiteDeath(player, step * death);
            }
            helper.assertTrue(!offer,
                    Component.literal("deaths spread beyond the window still raised the offer"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp escape streak expires outside the window");
        }
    }
}
