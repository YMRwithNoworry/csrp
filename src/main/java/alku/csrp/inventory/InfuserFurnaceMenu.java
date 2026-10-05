package alku.csrp.inventory;

import alku.csrp.registry.ModMenus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Port of SRP 1.10.9 {@code container/ContainerInfuserFurnace}.
 *
 * <p>Slot order and coordinates are the original's, so they line up with the slot frames baked into
 * {@code textures/gui/infuser_furnace.png}: smelt input (56,17), fuel (56,53), smelt output (140,17),
 * infuse reagent (30,35), infused output (140,53) and bottle byproduct (116,35).</p>
 */
public final class InfuserFurnaceMenu extends AbstractContainerMenu {
    /** {@code TileEntityInfuserFurnace} slot count. */
    private static final int CONTAINER_SLOTS = 6;
    private static final int PLAYER_SLOTS_END = CONTAINER_SLOTS + 36;
    /** {@code getFieldCount()} of the original tile entity. */
    private static final int DATA_COUNT = 4;

    private final Container container;
    private final ContainerData data;

    public InfuserFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CONTAINER_SLOTS),
                new SimpleContainerData(DATA_COUNT));
    }

    public InfuserFurnaceMenu(int containerId, Inventory inventory, Container container,
            ContainerData data) {
        super(ModMenus.INFUSER_FURNACE.get(), containerId);
        checkContainerSize(container, CONTAINER_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        addSlot(new Slot(container, 0, 56, 17));
        addSlot(new FuelSlot(container, 1, 56, 53));
        addSlot(new Slot(container, 3, 30, 35));
        addSlot(new FurnaceResultSlot(inventory.player, container, 4, 140, 53));
        addSlot(new FurnaceResultSlot(inventory.player, container, 5, 116, 35));
        addSlot(new FurnaceResultSlot(inventory.player, container, 2, 140, 17));

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

    /** {@code isItemFuel}: the original validated the fuel slot with {@code TileEntityFurnace}. */
    static boolean isFuel(ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL);
    }

    /** {@code ContainerInfuserFurnace.transferStackInSlot}. */
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
        } else if (isFuel(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false) && !moveItemStackTo(stack, 3, 4, false)) {
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

    /** {@code TileEntityInfuserFurnace.isBurning()} as seen by the GUI. */
    public boolean isBurning() {
        return data.get(0) > 0;
    }

    /** {@code GuiInfuserFurnace.getBurnLeftScaled(13)}. */
    public int burnScaled() {
        int current = data.get(1);
        if (current <= 0) {
            current = 200;
        }
        return data.get(0) * 13 / current;
    }

    /** {@code GuiInfuserFurnace.getCookScaled(getField(2), 24)} — the vanilla smelting arrow. */
    public int smeltScaled() {
        int cook = data.get(2);
        return cook <= 0 ? 0 : cook * 24 / 200;
    }

    /** {@code GuiInfuserFurnace.getCookScaled(getField(3), 24)} — the infusion arrow. */
    public int infuseScaled() {
        int cook = data.get(3);
        return cook <= 0 ? 0 : cook * 24 / 200;
    }

    /** The fuel slot only accepts fuel, matching {@code ContainerInfuserFurnace}'s anonymous slot. */
    private static final class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFuel(stack);
        }
    }
}
