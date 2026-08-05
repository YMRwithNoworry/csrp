package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.recipe.CanteenRefillRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Csrp.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CanteenRefillRecipe>> CANTEEN_SHAPELESS =
            RECIPE_SERIALIZERS.register("canteen_shapeless", CanteenRefillRecipe.Serializer::new);

    private ModRecipeSerializers() {
    }
}
