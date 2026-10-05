package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.command.GuiDistortionCommand;
import com.mojang.serialization.MapCodec;
import java.util.List;
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
 * Runtime tests for {@code /srpguidistortion} (1.10.9 {@code SRPCommandGuiDistortion}).
 *
 * <p>The flag transition is driven through {@link GuiDistortionCommand#computeFlags} rather than through
 * a mock player: NeoForge rejects custom payloads to GameTest mock players, and the command syncs its
 * state with one.  The rule itself — including the creative/spectator default and the override — is what
 * the command and the client both apply, and it is checked here in full.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class GuiDistortionGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private GuiDistortionGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "gui_distortion"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "gui_distortion/flag_transitions"),
                new FlagTransitionsTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "gui_distortion/command_registered"),
                new CommandRegisteredTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class FlagTransitionsTest extends GameTestInstance {
        FlagTransitionsTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            // A normal-mode player: "off" disables, "on" enables, "toggle" flips the effective state.
            assertFlags(helper, "off", false, false, false, true, false, "normal off");
            assertFlags(helper, "on", true, false, false, false, false, "normal on");
            assertFlags(helper, "toggle", true, false, false, false, false, "normal toggle from disabled");
            assertFlags(helper, "toggle", false, false, false, true, false, "normal toggle from enabled");

            // A creative player starts out effectively disabled, so "toggle" turns it on and records the
            // override, and "off" clears the override again.
            assertFlags(helper, "toggle", false, false, true, false, true, "creative toggle from default");
            assertFlags(helper, "on", false, false, true, false, true, "creative on");
            assertFlags(helper, "off", false, true, true, true, false, "creative off");

            // effectivelyDisabled = disabled || (creativeOrSpectator && !creativeOverride)
            helper.assertTrue(GuiDistortionCommand.isEffectivelyDisabled(false, false, true),
                    Component.literal("a creative player without the override must be disabled"));
            helper.assertTrue(!GuiDistortionCommand.isEffectivelyDisabled(false, true, true),
                    Component.literal("the creative override must keep the effect on"));
            helper.assertTrue(GuiDistortionCommand.isEffectivelyDisabled(true, true, false),
                    Component.literal("the raw disable flag must win over the override"));
            helper.succeed();
        }

        private static void assertFlags(GameTestHelper helper, String mode, boolean disabled,
                boolean override, boolean creative, boolean expectedDisabled, boolean expectedOverride,
                String label) {
            boolean[] flags = GuiDistortionCommand.computeFlags(mode, disabled, override, creative);
            helper.assertTrue(flags[0] == expectedDisabled && flags[1] == expectedOverride,
                    Component.literal(label + ": expected disabled=" + expectedDisabled + " override="
                            + expectedOverride + " but got disabled=" + flags[0] + " override=" + flags[1]));
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp gui distortion flag transitions");
        }
    }

    /** The command and both of its aliases must be on the dispatcher. */
    private static final class CommandRegisteredTest extends GameTestInstance {
        CommandRegisteredTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
            helper.assertTrue(root.getChild(GuiDistortionCommand.NAME) != null,
                    Component.literal("/" + GuiDistortionCommand.NAME + " is not registered"));
            for (String alias : GuiDistortionCommand.ALIASES) {
                helper.assertTrue(root.getChild(alias) != null,
                        Component.literal("/" + alias + " (alias) is not registered"));
            }
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp gui distortion command registered");
        }
    }
}
