package alku.csrp.inventory;

import alku.csrp.block.entity.RelayTerminalBlockEntity;
import alku.csrp.item.RelayModuleItem;
import alku.csrp.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class RelayTerminalMenu extends AbstractContainerMenu {
    public static final int SCAN_BUTTON = 0;
    private static final int MODULE_SLOT_COUNT = 1;
    private static final int PLAYER_SLOT_END = 37;
    private final Container container;
    private final ContainerData data;

    public RelayTerminalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MODULE_SLOT_COUNT), new SimpleContainerData(3));
    }

    public RelayTerminalMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.RELAY_TERMINAL.get(), containerId);
        checkContainerSize(container, MODULE_SLOT_COUNT);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        addSlot(new Slot(container, 0, 80, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RelayModuleItem && !isScanning();
            }

            @Override
            public boolean mayPickup(Player player) {
                return !isScanning();
            }
        });
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
    public boolean clickMenuButton(Player player, int id) {
        if (id != SCAN_BUTTON || !(container instanceof RelayTerminalBlockEntity relay)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return relay.startScan(serverPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof RelayModuleItem && !isScanning()) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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

    public int cooldownTicks() {
        return data.get(0);
    }

    public int scanTicks() {
        return data.get(1);
    }

    public boolean isFormed() {
        return data.get(2) != 0;
    }

    public boolean isScanning() {
        return scanTicks() > 0;
    }
}
