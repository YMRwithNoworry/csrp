package alku.csrp.compat.jei;

import alku.csrp.Csrp;
import alku.csrp.recipe.InfuserFurnaceRecipes;
import alku.csrp.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Port of SRP 1.10.9 {@code compatibility/jei/SRPJEIPlugin}.
 *
 * <p>The original registered two categories — the infuser furnace and SRP brewing — plus the brewing
 * stand and infuser furnace catalysts, and hid sixteen advancement icon items from JEI's ingredient
 * list.  On 26.3 the brewing half needs no plugin code: those recipes are real
 * {@code minecraft:brewing} recipes ({@code data/csrp/recipe/brewing/*.json}), so JEI's built-in
 * brewing category displays them by itself.  What remains here is the infuser furnace category, its
 * catalyst, and the icon blacklist.</p>
 *
 * <p>JEI is a {@code compileOnly} dependency: the mod builds and runs without it, and this class is only
 * ever loaded by JEI's own plugin discovery.</p>
 */
@JeiPlugin
public final class SrpJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID =
            Identifier.fromNamespaceAndPath(Csrp.MODID, "jei_plugin");

    /**
     * {@code SRPJEIPlugin.blacklistAdvancementIcon}: the advancement icon items the original removed
     * from JEI so they do not clutter the ingredient list.
     */
    private static final List<String> HIDDEN_ADVANCEMENT_ICONS = List.of(
            "dark_days_icon",
            "adapted_icon",
            "primitive_icon",
            "crude_icon",
            "pure_icon",
            "hunt_season_icon",
            "guerilla_icon",
            "ecstasy_icon",
            "enemy_of_enemy_icon",
            "fog_nullifier_icon",
            "self_destruct_icon",
            "potion_columbus_icon",
            "potion_stolas_icon",
            "hellfire_chemical_warfare_icon",
            "cosmic_structural_failure_icon",
            "roots_icon");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new InfuserFurnaceJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(InfuserFurnaceJeiCategory.RECIPE_TYPE, InfuserFurnaceRecipes.all());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(InfuserFurnaceJeiCategory.RECIPE_TYPE,
                ModBlocks.INFUSER_FURNACE.get());
    }

    /** {@code blacklistAdvancementIcon}: hide the advancement icons once JEI is running. */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> icons = new ArrayList<>();
        for (String name : HIDDEN_ADVANCEMENT_ICONS) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Csrp.MODID, name));
            if (item != null && item != Items.AIR) {
                icons.add(new ItemStack(item));
            }
        }
        if (!icons.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, icons);
        }
    }
}
