package alku.csrp.inventory;

import alku.csrp.block.entity.ParasiteLootBlockEntity;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModMenus;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ParasiteLootMenu extends AbstractContainerMenu {
    private static final int LOOT_SLOTS = 27;
    private static final int PLAYER_SLOTS_END = 63;
    private final Container container;
    private final ContainerData data;
    private boolean hazardQueued;
    private float queuedFullness;

    public ParasiteLootMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(LOOT_SLOTS), new SimpleContainerData(1));
    }

    public ParasiteLootMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.PARASITE_LOOT.get(), containerId);
        checkContainerSize(container, LOOT_SLOTS);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new ParasiteLootSlot(container, column + row * 9,
                        8 + column * 18, 30 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 88 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 146));
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
        if (index < LOOT_SLOTS) {
            ((ParasiteLootSlot) slot).captureFullness();
            if (!moveItemStackTo(stack, LOOT_SLOTS, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ParasiteLootBlockEntity.isValidParasiteLootItem(stack)) {
            if (!moveItemStackTo(stack, 0, LOOT_SLOTS, false)) {
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        ItemStack before = slotId >= 0 && slotId < LOOT_SLOTS
                ? container.getItem(slotId).copy() : ItemStack.EMPTY;
        hazardQueued = false;
        super.clicked(slotId, button, clickType, player);
        if (!hazardQueued) {
            return;
        }
        ItemStack after = slotId >= 0 && slotId < LOOT_SLOTS
                ? container.getItem(slotId) : ItemStack.EMPTY;
        boolean replaced = !before.isEmpty() && !after.isEmpty()
                && !ItemStack.isSameItemSameComponents(before, after);
        if (!replaced) {
            applyParasiteEffects(player, queuedFullness);
        }
        hazardQueued = false;
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

    public int fullnessScaled() {
        return data.get(0);
    }

    private void applyParasiteEffects(Player player, float fullness) {
        if (player.level().isClientSide) {
            return;
        }
        float clamped = Math.max(0.0F, Math.min(1.0F, fullness));
        player.hurt(player.damageSources().magic(), 0.5F + 7.5F * (1.0F - clamped));

        int corrosionAmplifier;
        int corrosionDuration;
        int viralAmplifier;
        int viralDuration;
        if (clamped < 0.25F) {
            corrosionAmplifier = 1;
            corrosionDuration = 400;
            viralAmplifier = 3;
            viralDuration = 600;
        } else if (clamped < 0.5F) {
            corrosionAmplifier = 1;
            corrosionDuration = 200;
            viralAmplifier = 2;
            viralDuration = 400;
        } else if (clamped < 0.75F) {
            corrosionAmplifier = 0;
            corrosionDuration = 200;
            viralAmplifier = 1;
            viralDuration = 200;
        } else {
            corrosionAmplifier = 0;
            corrosionDuration = 100;
            viralAmplifier = 1;
            viralDuration = 100;
        }
        player.addEffect(new MobEffectInstance(ModMobEffects.CORROSION.get(),
                corrosionDuration, corrosionAmplifier, false, false));
        EffectStacking.apply(player, ModMobEffects.VIRAL.get(), viralDuration, viralAmplifier);
    }

    private void queueParasiteEffects(float fullness) {
        hazardQueued = true;
        queuedFullness = fullness;
    }

    private final class ParasiteLootSlot extends Slot {
        private float fullnessBeforeTake = -1.0F;

        private ParasiteLootSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ParasiteLootBlockEntity.isValidParasiteLootItem(stack);
        }

        @Override
        public ItemStack remove(int amount) {
            captureFullness();
            return super.remove(amount);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            float fullness = fullnessBeforeTake >= 0.0F
                    ? fullnessBeforeTake : ParasiteLootMenu.this.fullnessFromContainer();
            fullnessBeforeTake = -1.0F;
            ParasiteLootMenu.this.queueParasiteEffects(fullness);
            super.onTake(player, stack);
        }

        private void captureFullness() {
            fullnessBeforeTake = ParasiteLootMenu.this.fullnessFromContainer();
        }
    }

    private float fullnessFromContainer() {
        if (container instanceof ParasiteLootBlockEntity loot) {
            return loot.fullness();
        }
        int used = 0;
        for (int slot = 0; slot < LOOT_SLOTS; slot++) {
            if (!container.getItem(slot).isEmpty()) {
                used++;
            }
        }
        return used / (float) LOOT_SLOTS;
    }
}
