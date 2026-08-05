package alku.csrp.block.entity;

import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gluttonous Cyst container. After 25 seconds it starts consuming one item
 * every 5 seconds (durability items lose 5 durability per second), granting
 * evolution points per consumed item. When empty it becomes a Vacuous Cyst.
 */
public final class ParasiticCystBlockEntity extends BaseContainerBlockEntity {
    public static final int CONTAINER_SIZE = 36;
    private static final int START_DELAY_TICKS = 500;
    private static final int CONSUME_INTERVAL_TICKS = 100;
    private static final int DURABILITY_PER_CONSUME = 5;
    private static final int EVOLUTION_POINTS_PER_ITEM = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int consumeCooldown = START_DELAY_TICKS;

    public ParasiticCystBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARASITIC_CYST.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(blockEntity instanceof ParasiticCystBlockEntity cyst) || level.isClientSide) {
            return;
        }
        cyst.tickCyst((ServerLevel) level);
    }

    private void tickCyst(ServerLevel level) {
        if (consumeCooldown > 0) {
            consumeCooldown--;
            return;
        }
        consumeCooldown = CONSUME_INTERVAL_TICKS;
        if (isEmpty()) {
            level.setBlock(getBlockPos(), ModBlocks.VACUOUS_CYST.get().defaultBlockState(), 3);
            return;
        }
        consumeOne(level);
    }

    private void consumeOne(ServerLevel level) {
        BlockPos pos = getBlockPos();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage()) {
                int damage = Math.min(stack.getMaxDamage(),
                        stack.getDamageValue() + DURABILITY_PER_CONSUME);
                stack.setDamageValue(damage);
                if (damage >= stack.getMaxDamage()) {
                    items.set(slot, ItemStack.EMPTY);
                    EvolutionSystem.addPoints(level, EVOLUTION_POINTS_PER_ITEM,
                            EvolutionSystem.PointSource.CYST);
                }
            } else {
                items.set(slot, ItemStack.EMPTY);
                EvolutionSystem.addPoints(level, EVOLUTION_POINTS_PER_ITEM,
                        EvolutionSystem.PointSource.CYST);
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH,
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    6, 0.2D, 0.1D, 0.2D, 0.02D);
            level.playSound(null, pos, ModSounds.MOVING_FLESH_EAT.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.7F,
                    0.8F + level.getRandom().nextFloat() * 0.4F);
            setChanged();
            return;
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.csrp.parasitic_cyst");
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
        return new alku.csrp.inventory.ParasiticCystMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("ConsumeCooldown", consumeCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        consumeCooldown = tag.getInt("ConsumeCooldown");
    }
}
