package alku.csrp.block.entity;

import alku.csrp.inventory.RelayTerminalMenu;
import alku.csrp.item.RelayModuleItem;
import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModSounds;
import alku.csrp.relay.RelayScanReportFactory;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RelayTerminalBlockEntity extends BaseContainerBlockEntity {
    public static final int CONTAINER_SIZE = 1;
    public static final int SCAN_TICKS = 110;
    public static final int COOLDOWN_TICKS = 400;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private long nextScanTick;
    private int scanTicks;
    private UUID scanPlayer;
    private RelayModuleItem.Kind scanKind;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cooldownRemaining();
                case 1 -> scanTicks;
                case 2 -> isFormed() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public RelayTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RELAY_TERMINAL.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.csrp.relay_terminal");
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public void clearContent() {
        items.clear();
        items.addAll(NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY));
        setChanged();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.size() || amount <= 0) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty() && result.getCount() > amount) {
            ItemStack remainder = result.split(amount);
            items.set(slot, result);
            return remainder;
        }
        setChanged();
        return result;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof RelayModuleItem && scanTicks <= 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RelayTerminalMenu(containerId, inventory, this, data);
    }

    public boolean isFormed() {
        return level != null && level.getBlockState(worldPosition).is(ModBlocks.RELAY_BASE.get())
                && level.getBlockState(worldPosition.above()).is(ModBlocks.RELAY_MIDDLE.get())
                && level.getBlockState(worldPosition.above(2)).is(ModBlocks.RELAY_ROOF.get());
    }

    public boolean startScan(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!isFormed()) {
            player.sendSystemMessage(Component.translatable("message.csrp.relay.not_formed"));
            return false;
        }
        if (scanTicks > 0 || scanPlayer != null) {
            player.sendSystemMessage(Component.translatable("message.csrp.relay.busy"));
            return false;
        }
        int cooldown = cooldownRemaining();
        if (cooldown > 0) {
            player.sendSystemMessage(Component.translatable("message.csrp.relay.cooldown",
                    (cooldown + 19) / 20));
            return false;
        }
        ItemStack module = getItem(0);
        if (!(module.getItem() instanceof RelayModuleItem relayModule)) {
            player.sendSystemMessage(Component.translatable("message.csrp.relay.insert_module"));
            return false;
        }
        if (!RelayScanReportFactory.hasProfile(relayModule.kind())) {
            player.sendSystemMessage(Component.translatable("message.csrp.relay.no_profile"));
            return false;
        }
        scanTicks = SCAN_TICKS;
        scanPlayer = player.getUUID();
        scanKind = relayModule.kind();
        nextScanTick = serverLevel.getGameTime() + COOLDOWN_TICKS;
        serverLevel.playSound(null, worldPosition, ModSounds.get("relay.scan.activate"),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.csrp.relay.started"));
        setChanged();
        return true;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel) || scanPlayer == null || scanKind == null) {
            return;
        }
        if (scanTicks > 0) {
            scanTicks--;
            if (scanTicks > 0) {
                return;
            }
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(scanPlayer);
        if (player == null) {
            return;
        }
        for (ItemStack report : RelayScanReportFactory.createReports(serverLevel, worldPosition, scanKind)) {
            if (!player.getInventory().add(report)) {
                player.drop(report, false);
            }
        }
        serverLevel.playSound(null, worldPosition, ModSounds.get("relay.paper.output"),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.csrp.relay.complete"));
        scanPlayer = null;
        scanKind = null;
        scanTicks = 0;
        setChanged();
    }

    public int cooldownRemaining() {
        if (level == null) {
            return 0;
        }
        return (int) Math.max(0L, nextScanTick - level.getGameTime());
    }

    public boolean isScanning() {
        return scanPlayer != null;
    }

    public ContainerData dataAccess() {
        return data;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putLong("NextScanTick", nextScanTick);
        tag.putInt("ScanTicks", scanTicks);
        if (scanPlayer != null) {
            tag.putUUID("ScanPlayer", scanPlayer);
        }
        if (scanKind != null) {
            tag.putString("ScanKind", scanKind.name());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        nextScanTick = tag.getLong("NextScanTick");
        scanTicks = Math.max(0, tag.getInt("ScanTicks"));
        scanPlayer = tag.hasUUID("ScanPlayer") ? tag.getUUID("ScanPlayer") : null;
        if (tag.contains("ScanKind")) {
            try {
                scanKind = RelayModuleItem.Kind.valueOf(tag.getString("ScanKind"));
            } catch (IllegalArgumentException ignored) {
                scanKind = null;
                scanPlayer = null;
                scanTicks = 0;
            }
        }
    }
}
