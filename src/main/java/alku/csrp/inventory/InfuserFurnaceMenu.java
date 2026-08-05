package alku.csrp.inventory;

import alku.csrp.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class InfuserFurnaceMenu extends AbstractContainerMenu {
    private static final int CONTAINER_SLOTS = 4;
    private static final int PLAYER_SLOTS_END = 31;
    private final Container container;
    private final ContainerData data;

    public InfuserFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CONTAINER_SLOTS),
                new SimpleContainerData(3));
    }

    public InfuserFurnaceMenu(int containerId, Inventory inventory, Container container,
            ContainerData data) {
        super(ModMenus.INFUSER_FURNACE.get(), containerId);
        checkContainerSize(container, CONTAINER_SLOTS);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        addSlot(new Slot(container, 0, 44, 17));
        addSlot(new Slot(container, 1, 62, 17));
        addSlot(new Slot(container, 2, 56, 53));
        addSlot(new Slot(container, 3, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, CONTAINER_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    public int burnScaled() {
        int duration = data.get(1);
        return duration <= 0 ? 0 : data.get(0) * 14 / duration;
    }

    public int progressScaled() {
        return data.get(2) * 24 / 200;
    }
}
