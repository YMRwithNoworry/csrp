package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 验证源液与流动死血均可装瓶。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DeadBloodBottleGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 20;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private DeadBloodBottleGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "dead_blood_bottles"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "dead_blood_bottles/source_flowing_and_creative"),
                new BottleCollectionTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class BottleCollectionTest extends GameTestInstance {
        BottleCollectionTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            BlockPos sourcePos = new BlockPos(1, 1, 1);
            BlockPos flowingPos = new BlockPos(3, 1, 1);
            BlockPos creativePos = new BlockPos(5, 1, 1);
            BlockPos fullInventoryPos = new BlockPos(7, 1, 1);
            helper.setBlock(sourcePos, ModBlocks.DEAD_BLOOD.get().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, 0));
            helper.setBlock(flowingPos, ModBlocks.DEAD_BLOOD.get().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, 1));
            helper.setBlock(creativePos, ModBlocks.DEAD_BLOOD.get().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, 0));
            helper.setBlock(fullInventoryPos, ModBlocks.DEAD_BLOOD.get().defaultBlockState()
                    .setValue(LiquidBlock.LEVEL, 0));

            helper.assertTrue(helper.getBlockState(sourcePos).getFluidState().isSource(),
                    Component.literal("the source bottle test did not create source Dead Blood"));
            helper.assertTrue(!helper.getBlockState(flowingPos).getFluidState().isSource()
                            && helper.getBlockState(flowingPos).getFluidState().getAmount() > 0,
                    Component.literal("the flowing bottle test did not create flowing Dead Blood"));

            Player survivalSource = helper.makeMockServerPlayer(GameType.SURVIVAL);
            setHeldStack(helper, survivalSource, sourcePos, new ItemStack(Items.GLASS_BOTTLE, 2));
            helper.useBlock(sourcePos, survivalSource);
            helper.assertTrue(survivalSource.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                    Component.literal("source collection did not consume exactly one glass bottle"));
            helper.assertTrue(survivalSource.getInventory().contains(new ItemStack(ModItems.DEADBLOOD_FLUID.get())),
                    Component.literal("source collection did not produce a Dead Blood bottle"));

            Player survivalFlowing = helper.makeMockServerPlayer(GameType.SURVIVAL);
            setHeldStack(helper, survivalFlowing, flowingPos, new ItemStack(Items.GLASS_BOTTLE));
            helper.useBlock(flowingPos, survivalFlowing);
            helper.assertTrue(survivalFlowing.getItemInHand(InteractionHand.MAIN_HAND)
                            .is(ModItems.DEADBLOOD_FLUID.get()),
                    Component.literal("flowing Dead Blood could not replace the consumed glass bottle"));

            Player creative = helper.makeMockServerPlayer(GameType.CREATIVE);
            setHeldStack(helper, creative, creativePos, new ItemStack(Items.GLASS_BOTTLE));
            helper.useBlock(creativePos, creative);
            helper.assertTrue(creative.getItemInHand(InteractionHand.MAIN_HAND).is(Items.GLASS_BOTTLE),
                    Component.literal("creative collection consumed the glass bottle"));
            helper.assertTrue(creative.getInventory().contains(new ItemStack(ModItems.DEADBLOOD_FLUID.get())),
                    Component.literal("creative collection did not produce a Dead Blood bottle"));

            Player fullInventory = helper.makeMockServerPlayer(GameType.SURVIVAL);
            setHeldStack(helper, fullInventory, fullInventoryPos, new ItemStack(Items.GLASS_BOTTLE, 2));
            for (int slot = 0; slot < fullInventory.getInventory().getContainerSize(); slot++) {
                fullInventory.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            }
            fullInventory.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
            helper.useBlock(fullInventoryPos, fullInventory);
            helper.assertTrue(fullInventory.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                    Component.literal("full-inventory collection did not consume exactly one glass bottle"));
            helper.assertItemEntityPresent(ModItems.DEADBLOOD_FLUID.get(), fullInventoryPos, 2.0D);

            helper.succeed();
        }

        private static void setHeldStack(GameTestHelper helper, Player player, BlockPos pos, ItemStack stack) {
            BlockPos absolute = helper.absolutePos(pos);
            player.setPos(Vec3.atCenterOf(absolute));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp Dead Blood source and flowing bottle collection");
        }
    }
}
