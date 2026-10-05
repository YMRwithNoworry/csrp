package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.command.HarlequinCommand;
import alku.csrp.world.ParasiteBiomeDecorator;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Runtime checks for the ported {@code /harlequin_here} (1.10.9 {@code CommandHarlequinHere}).
 *
 * <p>The command itself needs a player, so the two things it depends on are checked directly: the
 * Harlequin biome exists in the registry, and the biome-fill path the command uses
 * ({@link FillBiomeCommand#fill}, vanilla's own {@code /fillbiome} entry point) really rewrites the
 * terrain's biomes — the 26.3 replacement for the original's hand-rolled
 * {@code Chunk#getBiomeArray} write plus chunk-biome packet.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class HarlequinCommandGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private HarlequinCommandGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "harlequin_command"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "harlequin_command/registered"),
                new RegisteredTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "harlequin_command/biome_fill"),
                new BiomeFillTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class RegisteredTest extends GameTestInstance {
        RegisteredTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
            helper.assertTrue(root.getChild(HarlequinCommand.HERE) != null,
                    Component.literal("/" + HarlequinCommand.HERE + " is not registered"));
            helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME)
                            .getValue(ParasiteBiomeDecorator.HARLEQUIN_BIOME) != null,
                    Component.literal("the srp_harlequinn biome is not registered"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp harlequin_here registered");
        }
    }

    /** Drives the command's own biome write and asserts the terrain biome actually changed. */
    private static final class BiomeFillTest extends GameTestInstance {
        BiomeFillTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            Holder<Biome> harlequin = HarlequinCommand.harlequinBiome(level);
            BlockPos origin = helper.absolutePos(BlockPos.ZERO);

            int changed = HarlequinCommand.applyHarlequinBiome(level, origin.getX() >> 4,
                    origin.getZ() >> 4, 0, harlequin);
            helper.assertTrue(changed == 1,
                    Component.literal("expected one rewritten chunk but got " + changed));
            helper.assertTrue(level.getBiome(origin).is(ParasiteBiomeDecorator.HARLEQUIN_BIOME),
                    Component.literal("the biome at the test origin was not changed to Harlequin"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp harlequin biome fill");
        }
    }
}
