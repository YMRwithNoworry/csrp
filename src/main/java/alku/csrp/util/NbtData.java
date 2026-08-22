package alku.csrp.util;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** 1.20.1 replacement for the 1.21 CustomData component helpers, backed by ItemStack NBT. */
public final class NbtData {
    private NbtData() {
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> action) {
        CompoundTag tag = stack.getOrCreateTag();
        action.accept(tag);
    }

    public static CompoundTag copyTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
    }

    public static CompoundTag tag(ItemStack stack) {
        return stack.getTag();
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag);
    }
}
