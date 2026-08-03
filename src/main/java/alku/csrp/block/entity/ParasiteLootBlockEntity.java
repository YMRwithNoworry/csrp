package alku.csrp.block.entity;

import alku.csrp.Csrp;
import alku.csrp.block.ParasiteLootBlock;
import alku.csrp.inventory.ParasiteLootMenu;
import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ParasiteLootBlockEntity extends BaseContainerBlockEntity {
    public static final int CONTAINER_SIZE = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private boolean lootGenerated;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? Math.round(fullness() * 1_000.0F) : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public ParasiteLootBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARASITE_LOOT.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.csrp.parasite_loot");
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
        return isValidParasiteLootItem(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        generateLootIfNeeded();
        return new ParasiteLootMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("LootGenerated", lootGenerated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        lootGenerated = tag.getBoolean("LootGenerated") || !isEmpty();
    }

    public float fullness() {
        int used = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                used++;
            }
        }
        return used / (float) CONTAINER_SIZE;
    }

    public ContainerData dataAccess() {
        return data;
    }

    public static boolean isValidParasiteLootItem(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof BlockItem) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Csrp.MODID);
    }

    private void generateLootIfNeeded() {
        if (lootGenerated || level == null || level.isClientSide) {
            return;
        }
        lootGenerated = true;
        if (!(getBlockState().getBlock() instanceof ParasiteLootBlock block)) {
            setChanged();
            return;
        }
        List<Item> pool = lootPool(block.tier());
        RandomSource random = level.getRandom();
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).isEmpty() && random.nextFloat() < block.tier().slotChance()) {
                items.set(slot, new ItemStack(pool.get(random.nextInt(pool.size()))));
            }
        }
        setChanged();
    }

    private static List<Item> lootPool(ParasiteLootBlock.Tier tier) {
        return switch (tier) {
            case COMMON -> List.of(ModItems.ASSIMILATED_FLESH.get(), ModItems.BONE.get());
            case UNCOMMON -> List.of(
                    ModItems.ADA_SUMMONER_DROP.get(), ModItems.ADA_YELLOWEYE_DROP.get(),
                    ModItems.ADA_MANDUCATER_DROP.get(), ModItems.ADA_REEKER_DROP.get(),
                    ModItems.ADA_LONGARMS_DROP.get(), ModItems.ADA_BOLSTER_DROP.get(),
                    ModItems.ADA_ARACHNIDA_DROP.get(), ModItems.ADA_DEVOURER_DROP.get(),
                    ModItems.ASSIMILATED_FLESH.get(), ModItems.BONE.get());
            case RARE -> List.of(
                    ModItems.BECKON_DROP.get(), ModItems.DISPATCHER_DROP.get(),
                    ModItems.LURECOMPONENT4.get(), ModItems.LURECOMPONENT5.get(), ModItems.LURECOMPONENT6.get());
        };
    }
}
