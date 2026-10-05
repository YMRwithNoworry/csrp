package alku.csrp.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Port of SRP 1.10.9 {@code recipes/InfuserFurnaceRecipe}.
 *
 * <p>The original matched two {@code Ingredient}s (the smelt slot and the infuse slot) and produced a
 * main output plus an optional bottle output.  Both ingredients were declared from plain
 * {@code ItemStack}s, so a plain item comparison is equivalent here — 1.12.2's
 * {@code Ingredient#apply(ItemStack)} ignores the stack size, which is why the 32/16 amounts in the
 * original's table are display amounts only and the furnace consumes one of each.</p>
 */
public record InfuserFurnaceRecipe(Item smeltIn, Item infuseIn, ItemStack output, ItemStack byproduct) {
    public boolean matches(ItemStack smelt, ItemStack infuse) {
        if (smelt.isEmpty() || infuse.isEmpty()) {
            return false;
        }
        return smelt.is(this.smeltIn) && infuse.is(this.infuseIn);
    }

    public boolean hasByproduct() {
        return !this.byproduct.isEmpty();
    }
}
