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
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
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
                player.drop(report, false, Prediction.SERVER_ONLY);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("NextScanTick", nextScanTick);
        output.putInt("ScanTicks", scanTicks);
        if (scanPlayer != null) {
            output.store("ScanPlayer", UUIDUtil.CODEC, scanPlayer);
        }
        if (scanKind != null) {
            output.putString("ScanKind", scanKind.name());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        nextScanTick = input.getLongOr("NextScanTick", 0L);
        scanTicks = Math.max(0, input.getIntOr("ScanTicks", 0));
        scanPlayer = input.read("ScanPlayer", UUIDUtil.CODEC).orElse(null);
        if (input.getString("ScanKind").isPresent()) {
            try {
                scanKind = RelayModuleItem.Kind.valueOf(input.getStringOr("ScanKind", ""));
            } catch (IllegalArgumentException ignored) {
                scanKind = null;
                scanPlayer = null;
                scanTicks = 0;
            }
        }
    }
}
