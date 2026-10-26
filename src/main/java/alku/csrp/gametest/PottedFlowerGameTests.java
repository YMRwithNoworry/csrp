package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 验证原版异化花朵放入花盆的交互行为。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class PottedFlowerGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 20;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final BlockPos SURVIVAL_POT = new BlockPos(1, 1, 1);
    private static final BlockPos CREATIVE_POT = new BlockPos(3, 1, 1);
    private static final BlockPos TOP_POT = new BlockPos(5, 1, 1);
    private static final BlockPos FULL_POT = new BlockPos(7, 1, 1);

    private PottedFlowerGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "potted_flower"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "potted_flower/assimilated_blossom_interaction"),
                new AssimilatedBlossomPotTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class AssimilatedBlossomPotTest extends GameTestInstance {
        AssimilatedBlossomPotTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            helper.setBlock(SURVIVAL_POT, Blocks.FLOWER_POT);
            helper.setBlock(CREATIVE_POT, Blocks.FLOWER_POT);
            helper.setBlock(TOP_POT, Blocks.FLOWER_POT);
            helper.setBlock(FULL_POT, Blocks.POTTED_POPPY);

            Player survival = helper.makeMockServerPlayer(GameType.SURVIVAL);
            ItemStack survivalBlossoms = new ItemStack(ModItems.ASSIMILATED_BLOSSOM.get(), 2);
            survival.setItemInHand(InteractionHand.MAIN_HAND, survivalBlossoms);
            PlayerInteractEvent.RightClickBlock survivalClick = click(helper, survival, SURVIVAL_POT, Direction.NORTH);
            NeoForge.EVENT_BUS.post(survivalClick);
            helper.assertTrue(survivalClick.getCancellationResult() == InteractionResult.CONSUME,
                    Component.literal("the valid survival pot interaction was not consumed"));
            helper.assertTrue(helper.getBlockState(SURVIVAL_POT)
                            .is(ModBlocks.legacyBlock("potted_assimilated_blossom").get()),
                    Component.literal("the empty flower pot did not receive the assimilated blossom"));
            helper.assertTrue(survivalBlossoms.getCount() == 1,
                    Component.literal("survival potting did not consume exactly one blossom"));

            Player creative = helper.makeMockServerPlayer(GameType.CREATIVE);
            ItemStack creativeBlossom = new ItemStack(ModItems.ASSIMILATED_BLOSSOM.get());
            creative.setItemInHand(InteractionHand.MAIN_HAND, creativeBlossom);
            PlayerInteractEvent.RightClickBlock creativeClick = click(helper, creative, CREATIVE_POT, Direction.WEST);
            NeoForge.EVENT_BUS.post(creativeClick);
            helper.assertTrue(creativeClick.getCancellationResult() == InteractionResult.CONSUME,
                    Component.literal("the valid creative pot interaction was not consumed"));
            helper.assertTrue(helper.getBlockState(CREATIVE_POT)
                            .is(ModBlocks.legacyBlock("potted_assimilated_blossom").get()),
                    Component.literal("creative potting did not place the assimilated blossom"));
            helper.assertTrue(creativeBlossom.getCount() == 1,
                    Component.literal("creative potting consumed the blossom"));

            Player topClicker = helper.makeMockServerPlayer(GameType.SURVIVAL);
            ItemStack topBlossom = new ItemStack(ModItems.ASSIMILATED_BLOSSOM.get());
            topClicker.setItemInHand(InteractionHand.MAIN_HAND, topBlossom);
            PlayerInteractEvent.RightClickBlock topClick = click(helper, topClicker, TOP_POT, Direction.UP);
            NeoForge.EVENT_BUS.post(topClick);
            helper.assertTrue(helper.getBlockState(TOP_POT).is(Blocks.FLOWER_POT)
                            && topBlossom.getCount() == 1,
                    Component.literal("clicking the top of an empty pot incorrectly planted a blossom"));

            Player fullPotClicker = helper.makeMockServerPlayer(GameType.SURVIVAL);
            ItemStack fullPotBlossom = new ItemStack(ModItems.ASSIMILATED_BLOSSOM.get());
            fullPotClicker.setItemInHand(InteractionHand.MAIN_HAND, fullPotBlossom);
            PlayerInteractEvent.RightClickBlock fullPotClick = click(
                    helper, fullPotClicker, FULL_POT, Direction.NORTH);
            NeoForge.EVENT_BUS.post(fullPotClick);
            helper.assertTrue(helper.getBlockState(FULL_POT).is(Blocks.POTTED_POPPY)
                            && fullPotBlossom.getCount() == 1,
                    Component.literal("a non-empty flower pot was replaced by the assimilated blossom"));

            helper.succeed();
        }

        private static PlayerInteractEvent.RightClickBlock click(
                GameTestHelper helper, Player player, BlockPos pos, Direction face) {
            BlockPos absolute = helper.absolutePos(pos);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), face, absolute, false);
            return new PlayerInteractEvent.RightClickBlock(
                    player, InteractionHand.MAIN_HAND, absolute, hit);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp assimilated blossom flower-pot interaction");
        }
    }
}
