package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.command.HarlequinCommand;
import alku.csrp.command.HarlequinConvertCommand;
import alku.csrp.command.HarlequinScatterCommand;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.ParasiteBiomeDecorator;
import alku.csrp.world.gen.HarlequinBlockConverter;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
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
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "harlequin_command/convert_palette"),
                new ConvertPaletteTest(testData(environment)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "harlequin_command/scatter_contract"),
                new ScatterContractTest(testData(environment)));
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

            int chunkX = origin.getX() >> 4;
            int chunkZ = origin.getZ() >> 4;
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            int changed = HarlequinCommand.applyHarlequinBiome(level, chunkX, chunkZ, 0, harlequin);
            helper.assertTrue(changed == 1,
                    Component.literal("expected one rewritten chunk but got " + changed));
            // Read the biome back through the same chunk the command wrote to: a gametest level keeps
            // generating chunks while a test runs, so `level.getBiome` can land on a chunk that was
            // (re)generated after the write and lose it.
            helper.assertTrue(chunk.getNoiseBiome(QuartPos.fromBlock(origin.getX()),
                            QuartPos.fromBlock(origin.getY()), QuartPos.fromBlock(origin.getZ()))
                            .is(ParasiteBiomeDecorator.HARLEQUIN_BIOME),
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

    /**
     * Drives {@link HarlequinBlockConverter} — the {@code /harlequin_convert} kernel — over the
     * original's block families.  The sand and grass cases pass {@code blotchHere=true} so the result
     * does not depend on whether the test column is open to the sky.
     */
    private static final class ConvertPaletteTest extends GameTestInstance {
        ConvertPaletteTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            HarlequinBlockConverter.Config config = HarlequinConvertCommand.config();
            RandomSource random = RandomSource.create(1L);

            BlockPos planks = new BlockPos(1, 1, 1);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            Block planksResult = HarlequinBlockConverter.convert(level, helper.absolutePos(planks),
                    Blocks.OAK_PLANKS.defaultBlockState(), false, random, config);
            helper.assertTrue(planksResult == config.harleskinn(),
                    Component.literal("oak planks converted to " + planksResult));
            helper.assertTrue(helper.getBlockState(planks).is(config.harleskinn()),
                    Component.literal("oak planks were not written as harleskinn"));

            BlockPos leaves = new BlockPos(2, 1, 1);
            helper.setBlock(leaves, Blocks.OAK_LEAVES);
            Block leavesResult = HarlequinBlockConverter.convert(level, helper.absolutePos(leaves),
                    Blocks.OAK_LEAVES.defaultBlockState(), false, random, config);
            helper.assertTrue(leavesResult == config.alveoli(),
                    Component.literal("oak leaves converted to " + leavesResult));
            helper.assertTrue(helper.getBlockState(leaves).is(config.alveoli()),
                    Component.literal("oak leaves were not written as alveoli"));

            BlockPos grass = new BlockPos(3, 1, 1);
            helper.setBlock(grass, Blocks.GRASS_BLOCK);
            Block grassResult = HarlequinBlockConverter.convert(level, helper.absolutePos(grass),
                    Blocks.GRASS_BLOCK.defaultBlockState(), false, random, config);
            helper.assertTrue(grassResult == config.harlequinnGrass(),
                    Component.literal("grass converted to " + grassResult));
            // 1.10.9's maybePlaceHair is gated on Block#canSustainPlant, whose Forge default is false
            // and which 1.10.9 overrides nowhere: converted grass never grows hair in the original.
            helper.assertTrue(helper.getBlockState(grass.above()).isAir(),
                    Component.literal("converted grass grew " + helper.getBlockState(grass.above())));

            BlockPos blotch = new BlockPos(4, 1, 1);
            helper.setBlock(blotch, Blocks.GRASS_BLOCK);
            Block blotchResult = HarlequinBlockConverter.convert(level, helper.absolutePos(blotch),
                    Blocks.GRASS_BLOCK.defaultBlockState(), true, random, config);
            helper.assertTrue(blotchResult == config.harleskinn(),
                    Component.literal("blotched grass converted to " + blotchResult));

            BlockPos sand = new BlockPos(5, 1, 1);
            helper.setBlock(sand, Blocks.SAND);
            Block sandResult = HarlequinBlockConverter.convert(level, helper.absolutePos(sand),
                    Blocks.SAND.defaultBlockState(), true, random, config);
            helper.assertTrue(sandResult == config.harleskinn(),
                    Component.literal("blotched sand converted to " + sandResult));

            BlockPos water = new BlockPos(6, 1, 1);
            helper.setBlock(water, Blocks.WATER);
            Block waterResult = HarlequinBlockConverter.convert(level, helper.absolutePos(water),
                    Blocks.WATER.defaultBlockState(), true, random, config);
            helper.assertTrue(waterResult == null, Component.literal("water converted to " + waterResult));
            helper.assertTrue(helper.getBlockState(water).is(Blocks.WATER),
                    Component.literal("water was replaced by " + helper.getBlockState(water)));

            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp harlequin convert palette");
        }
    }

    /**
     * The parts of {@code /harlequin_scatter} that need no player: the three dispatcher entries, the
     * ground/top rules (which follow the original's {@code SAND} / {@code SANDSTONE} /
     * {@code RED_SANDSTONE} / {@code GRASS} / {@code DIRT} set rather than a red-sand/gravel pair), and
     * one real template placement.
     */
    private static final class ScatterContractTest extends GameTestInstance {
        ScatterContractTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
            for (String name : new String[] {HarlequinCommand.HERE, HarlequinConvertCommand.NAME,
                    HarlequinScatterCommand.NAME}) {
                helper.assertTrue(root.getChild(name) != null,
                        Component.literal("/" + name + " is not registered"));
            }

            ServerLevel level = helper.getLevel();
            BlockPos ground = new BlockPos(1, 1, 3);
            for (Block good : new Block[] {Blocks.SAND, Blocks.SANDSTONE, Blocks.RED_SANDSTONE,
                    Blocks.GRASS_BLOCK, Blocks.DIRT, ModBlocks.HARLESKINN_BLOCK.get()}) {
                helper.setBlock(ground, good);
                helper.assertTrue(HarlequinScatterCommand.isGoodGround(level, helper.absolutePos(ground)),
                        Component.literal(good + " is no longer accepted ground"));
            }
            for (Block bad : new Block[] {Blocks.STONE, Blocks.RED_SAND, Blocks.GRAVEL}) {
                helper.setBlock(ground, bad);
                helper.assertTrue(!HarlequinScatterCommand.isGoodGround(level, helper.absolutePos(ground)),
                        Component.literal(bad + " was accepted as ground but is not in the original set"));
            }

            BlockPos top = new BlockPos(2, 1, 3);
            for (Block bad : new Block[] {Blocks.WATER, Blocks.LAVA, Blocks.OAK_LEAVES}) {
                helper.setBlock(top, bad);
                helper.assertTrue(HarlequinScatterCommand.isBadTop(level, helper.absolutePos(top)),
                        Component.literal(bad + " is no longer a bad top block"));
            }
            for (Block fine : new Block[] {Blocks.STONE, Blocks.ICE}) {
                helper.setBlock(top, fine);
                helper.assertTrue(!HarlequinScatterCommand.isBadTop(level, helper.absolutePos(top)),
                        Component.literal(fine + " is not a bad top block in the original"));
            }

            BlockPos at = helper.absolutePos(new BlockPos(6, 1, 6));
            BlockPos min = at.offset(-4, -2, -4);
            BlockPos max = at.offset(4, 4, 4);
            int before = countNonAir(level, min, max);
            helper.assertTrue(
                    HarlequinScatterCommand.placeTemplate(level, at, "harlequin_rock_01",
                            RandomSource.create(7L)),
                    Component.literal("harlequin_rock_01 is not packaged"));
            helper.assertTrue(countNonAir(level, min, max) > before,
                    Component.literal("harlequin_rock_01 placed no blocks"));

            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp harlequin scatter contract");
        }
    }

    private static int countNonAir(ServerLevel level, BlockPos min, BlockPos max) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(pos).isAir()) {
                count++;
            }
        }
        return count;
    }
}
