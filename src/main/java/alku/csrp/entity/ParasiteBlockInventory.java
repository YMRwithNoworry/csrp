package alku.csrp.entity;

import java.util.List;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stored block inventory of block-breaking parasites. Collected drops are kept
 * in the parasite's persistent data and later dumped into a Gluttonous Cyst.
 */
public final class ParasiteBlockInventory {
    public static final String TAG = "csrp_block_inventory";
    public static final int MAX_STACKS = 36;

    private ParasiteBlockInventory() {
    }

    /**
     * Destroys the block without dropping items into the world and stores its
     * drops in the parasite's inventory. Returns true when the block was
     * collected.
     */
    public static boolean collect(ServerLevel level, BlockPos pos, LivingEntity parasite) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        CompoundTag data = parasite.getPersistentData();
        ListTag list = data.getList(TAG, CompoundTag.TAG_COMPOUND);
        if (list.size() >= MAX_STACKS) {
            return false;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, parasite, ItemStack.EMPTY);
        for (ItemStack drop : drops) {
            if (drop.isEmpty() || list.size() >= MAX_STACKS) {
                continue;
            }
            list.add(drop.save(new CompoundTag()));
        }
        data.put(TAG, list);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, pos, state.getSoundType().getBreakSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        if (list.size() >= GoreEntity.cystThreshold()) {
            launchCyst(level, parasite);
        }
        return true;
    }

    private static void launchCyst(ServerLevel level, LivingEntity parasite) {
        NonNullList<ItemStack> items = takeAll(parasite);
        if (items.isEmpty()) {
            return;
        }
        GoreEntity gore = ModEntities.GORE.get().create(level);
        if (gore == null) {
            return;
        }
        gore.setType((byte) 10);
        gore.setStoredItems(items);
        gore.moveTo(parasite.getX(), parasite.getY() + parasite.getBbHeight() * 0.5D,
                parasite.getZ(), parasite.getYRot(), parasite.getXRot());
        gore.setMotion(level.getRandom().nextDouble() - 0.5D, 0.75D,
                level.getRandom().nextDouble() - 0.5D, 0.25D, 0.75D);
        level.addFreshEntity(gore);
    }

    /**
     * Removes and returns all stored items, clearing the parasite's block
     * inventory.
     */
    public static NonNullList<ItemStack> takeAll(LivingEntity parasite) {
        // Item stacks use the legacy 1.20.1 NBT codec.
        CompoundTag data = parasite.getPersistentData();
        ListTag list = data.getList(TAG, CompoundTag.TAG_COMPOUND);
        NonNullList<ItemStack> items = NonNullList.withSize(
                Math.min(list.size(), MAX_STACKS), ItemStack.EMPTY);
        int slot = 0;
        for (Tag tag : list) {
            if (slot >= items.size()) {
                break;
            }
            if (tag instanceof CompoundTag compound) {
                items.set(slot, ItemStack.of(compound));
            }
            slot++;
        }
        data.remove(TAG);
        return items;
    }
}
