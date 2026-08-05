package alku.csrp.block.entity;

import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Metallurgical Infuser Furnace: combines Dead Blood Fluid with Iron Ingots
 * using furnace fuel to create Semi-organic Ingots.
 */
public final class InfuserFurnaceBlockEntity extends BaseContainerBlockEntity {
    public static final int IRON_SLOT = 0;
    public static final int BLOOD_SLOT = 1;
    public static final int FUEL_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int CONTAINER_SIZE = 4;
    public static final int PROCESS_TIME = 200;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int burnTime;
    private int burnDuration;
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnDuration;
                case 2 -> progress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnDuration = value;
                case 2 -> progress = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public InfuserFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSER_FURNACE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(blockEntity instanceof InfuserFurnaceBlockEntity furnace) || level.isClientSide) {
            return;
        }
        furnace.tickFurnace();
    }

    private void tickFurnace() {
        if (burnTime > 0) {
            burnTime--;
        }
        if (canProcess() && burnTime <= 0 && hasFuel()) {
            consumeFuel();
        }
        if (canProcess() && burnTime > 0) {
            progress++;
            if (progress >= PROCESS_TIME) {
                process();
                progress = 0;
            }
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
        }
        setChanged();
    }

    private boolean canProcess() {
        ItemStack iron = items.get(IRON_SLOT);
        ItemStack blood = items.get(BLOOD_SLOT);
        ItemStack output = items.get(OUTPUT_SLOT);
        if (!blood.is(ModItems.DEADBLOOD_FLUID.get())
                || !(iron.is(Items.IRON_INGOT) || iron.is(net.minecraft.world.level.block.Blocks.SAND.asItem()))) {
            return false;
        }
        net.minecraft.world.item.Item result = resultItem(iron);
        return output.isEmpty() || (output.is(result)
                && output.getCount() < output.getMaxStackSize());
    }

    private static net.minecraft.world.item.Item resultItem(ItemStack input) {
        return input.is(Items.IRON_INGOT) ? ModItems.SEMIORGANIC_INGOT.get()
                : ModBlocks.INFESTED_GLASS.get().asItem();
    }

    private boolean hasFuel() {
        return fuelBurnTime(items.get(FUEL_SLOT)) > 0;
    }

    private void consumeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        burnDuration = fuelBurnTime(fuel);
        burnTime = burnDuration;
        if (fuel.is(Items.LAVA_BUCKET)) {
            items.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
        } else {
            fuel.shrink(1);
        }
    }

    private static int fuelBurnTime(ItemStack stack) {
        Map<net.minecraft.world.item.Item, Integer> fuels = AbstractFurnaceBlockEntity.getFuel();
        return fuels.getOrDefault(stack.getItem(), 0);
    }

    private void process() {
        ItemStack input = items.get(IRON_SLOT);
        net.minecraft.world.item.Item result = resultItem(input);
        input.shrink(1);
        items.get(BLOOD_SLOT).shrink(1);
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, new ItemStack(result));
        } else {
            output.grow(1);
        }
    }

    public ContainerData dataAccess() {
        return data;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.csrp.infuser_furnace");
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new alku.csrp.inventory.InfuserFurnaceMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        progress = tag.getInt("Progress");
    }
}
