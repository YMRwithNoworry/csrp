package alku.csrp.recipe;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Port of SRP 1.10.9 {@code recipes/InfuserFurnaceRecipes} + {@code SRPInfuserFurnaceRecipeInit}.
 *
 * <p>The original filled a static list at preInit; here the table is built lazily on first use so it
 * never touches a {@code DeferredHolder} before the registries are populated.</p>
 *
 * <p>The nine entries are exactly the original's: two conversion recipes that leave a glass bottle
 * behind and seven glass swaps that yield two glass each.</p>
 */
public final class InfuserFurnaceRecipes {
    private static List<InfuserFurnaceRecipe> recipes;

    private InfuserFurnaceRecipes() {
    }

    /** {@code SRPInfuserFurnaceRecipeInit.addGlassSwap}: 32 infested glass + 16 material -> 2 glass. */
    private static void addGlassSwap(List<InfuserFurnaceRecipe> target, net.minecraft.world.item.Item material,
            net.minecraft.world.item.Item glass) {
        target.add(new InfuserFurnaceRecipe(ModBlocks.INFESTED_GLASS.get().asItem(), material,
                new ItemStack(glass, 2), ItemStack.EMPTY));
    }

    private static List<InfuserFurnaceRecipe> build() {
        List<InfuserFurnaceRecipe> table = new ArrayList<>(9);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        table.add(new InfuserFurnaceRecipe(Items.IRON_INGOT, ModItems.DEADBLOOD_FLUID.get(),
                new ItemStack(ModItems.SEMIORGANIC_INGOT.get()), bottle.copy()));
        table.add(new InfuserFurnaceRecipe(ModBlocks.INFESTED_SAND.get().asItem(), ModItems.DEADBLOOD_FLUID.get(),
                new ItemStack(ModBlocks.INFESTED_GLASS.get()), bottle.copy()));
        addGlassSwap(table, ModItems.COOKED_FLESH.get(), ModBlocks.BLOODY_GLASS.get().asItem());
        addGlassSwap(table, ModBlocks.INFESTED_TERRACOTTA.get().asItem(), ModBlocks.ASHEN_GLASS.get().asItem());
        addGlassSwap(table, ModBlocks.POLAND_SKIN_BLOCK.get().asItem(), ModBlocks.SEPIA_GLASS.get().asItem());
        addGlassSwap(table, ModBlocks.HARLESKINN_BLOCK.get().asItem(), ModBlocks.HARLEQUINN_GLASS.get().asItem());
        addGlassSwap(table, Items.ICE, ModBlocks.SHROUDED_GLASS.get().asItem());
        addGlassSwap(table, ModBlocks.RESIDUE_BLOCK.get().asItem(), ModBlocks.MOODY_GLASS.get().asItem());
        addGlassSwap(table, ModBlocks.GOTHSHROOM.get().asItem(), ModBlocks.SHADE_GLASS.get().asItem());
        return List.copyOf(table);
    }

    /** {@code InfuserFurnaceRecipes.find(ItemStack smelt, ItemStack infuse)}. */
    public static InfuserFurnaceRecipe find(ItemStack smelt, ItemStack infuse) {
        if (smelt.isEmpty() || infuse.isEmpty()) {
            return null;
        }
        for (InfuserFurnaceRecipe recipe : all()) {
            if (recipe.matches(smelt, infuse)) {
                return recipe;
            }
        }
        return null;
    }

    /** {@code InfuserFurnaceRecipes.all()}. */
    public static List<InfuserFurnaceRecipe> all() {
        if (recipes == null) {
            recipes = build();
        }
        return recipes;
    }
}
