package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.block.entity.DermoidCystBlockEntity;
import alku.csrp.registry.ModBlocks;
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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Runtime checks for the original 27-slot Dermoid Cyst container. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DermoidCystGameTests {
    private static final String MODID = Csrp.MODID;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final BlockPos CYST_POS = new BlockPos(1, 1, 1);

    private DermoidCystGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "dermoid_cyst"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "dermoid_cyst/container_contract"),
                new ContainerContractTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class ContainerContractTest extends GameTestInstance {
        ContainerContractTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            helper.setBlock(CYST_POS, ModBlocks.legacyBlock("dermoid_cyst").get());
            DermoidCystBlockEntity cyst = helper.getBlockEntity(CYST_POS, DermoidCystBlockEntity.class);
            helper.assertTrue(cyst != null, Component.literal("dermoid cyst has no block entity"));
            helper.assertTrue(cyst.getContainerSize() == 27,
                    Component.literal("dermoid cyst should have 27 slots"));
            helper.assertTrue(cyst.getMaxStackSize() == 64,
                    Component.literal("dermoid cyst max stack size should be 64"));

            for (int slot = 0; slot < cyst.getContainerSize(); slot++) {
                cyst.setItem(slot, new ItemStack(Items.STONE, cyst.getMaxStackSize()));
            }
            helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromContainer(cyst) == 15,
                    Component.literal("a full dermoid cyst should produce comparator signal 15"));
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp dermoid cyst container contract");
        }
    }
}
