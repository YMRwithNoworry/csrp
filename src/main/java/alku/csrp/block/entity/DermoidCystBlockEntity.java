package alku.csrp.block.entity;

import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The original SRP Dermoid Cyst's 27-slot container. */
public final class DermoidCystBlockEntity extends BaseContainerBlockEntity {
    public static final int CONTAINER_SIZE = 27;
    private static final int OPENER_RECHECK_INTERVAL = 80;
    private static final double OPENER_RANGE = 5.0D;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int numPlayersUsing;
    private int ticksSinceSync;

    public DermoidCystBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DERMOID_CYST.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dermoid_cyst");
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
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
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    public void startOpen(ContainerUser containerUser) {
        if (isRemoved() || containerUser.getLivingEntity().isSpectator()) {
            return;
        }
        numPlayersUsing++;
        playFleshGrowl();
        setChanged();
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        if (isRemoved() || containerUser.getLivingEntity().isSpectator()) {
            return;
        }
        numPlayersUsing = Math.max(0, numPlayersUsing - 1);
        playFleshGrowl();
        setChanged();
    }

    public int getNumPlayersUsing() {
        return numPlayersUsing;
    }

    public boolean isNamedBoris() {
        Component customName = getCustomName();
        if (customName == null) {
            return false;
        }
        String name = customName.getString().trim();
        return "Boris".equalsIgnoreCase(name) || "Borris".equalsIgnoreCase(name);
    }

    public void serverTick() {
        if (level == null || level.isClientSide() || ++ticksSinceSync % OPENER_RECHECK_INTERVAL != 0) {
            return;
        }

        int previous = numPlayersUsing;
        numPlayersUsing = level.getEntitiesOfClass(Player.class,
                new AABB(worldPosition).inflate(OPENER_RANGE), player ->
                        player.containerMenu instanceof ChestMenu menu && menu.getContainer() == this).size();
        if (numPlayersUsing != previous) {
            setChanged();
        }
    }

    private void playFleshGrowl() {
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, ModSounds.get("flesh.growl"),
                    SoundSource.BLOCKS, 10.0F, 1.0F);
        }
    }
}
