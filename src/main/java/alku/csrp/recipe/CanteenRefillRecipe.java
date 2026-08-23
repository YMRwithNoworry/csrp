package alku.csrp.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import alku.csrp.item.OverlastCanteenItem;
import alku.csrp.registry.ModRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;

public final class CanteenRefillRecipe implements CraftingRecipe {
    private static final ResourceLocation ID = new ResourceLocation("csrp", "canteen_refill");
    private final String group;
    private final CraftingBookCategory category;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;
    private final boolean simple;

    public CanteenRefillRecipe(String group, CraftingBookCategory category, ItemStack result,
            NonNullList<Ingredient> ingredients) {
        this.group = group;
        this.category = category;
        this.result = result;
        this.ingredients = ingredients;
        this.simple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        if (input.getContainerSize() != ingredients.size()) {
            return false;
        }
        boolean ingredientsMatch = !simple
                ? RecipeMatcher.findMatches(input.getItems().stream().filter(stack -> !stack.isEmpty()).toList(),
                        ingredients) != null
                : input.getContainerSize() == 1 && ingredients.size() == 1
                ? ingredients.get(0).test(input.getItem(0))
                : RecipeMatcher.findMatches(input.getItems(), ingredients) != null;
        if (!ingredientsMatch) {
            return false;
        }
        for (ItemStack ingredient : input.getItems()) {
            if (ingredient.is(result.getItem())
                    && ingredient.getItem() instanceof OverlastCanteenItem
                    && OverlastCanteenItem.getSips(ingredient) >= OverlastCanteenItem.MAX_SIPS) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        ItemStack output = result.copy();
        for (ItemStack ingredient : input.getItems()) {
            if (ingredient.getItem() instanceof OverlastCanteenItem) {
                OverlastCanteenItem.setState(output,
                        OverlastCanteenItem.MAX_SIPS,
                        OverlastCanteenItem.getCanteenDurability(ingredient));
                break;
            }
        }
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CANTEEN_SHAPELESS.get();
    }

    @Override
    public ResourceLocation getId() { return ID; }

    public static final class Serializer implements RecipeSerializer<CanteenRefillRecipe> {
        @Override
        public CanteenRefillRecipe fromJson(ResourceLocation id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory category = CraftingBookCategory.CODEC.parse(
                    com.mojang.serialization.JsonOps.INSTANCE, json.get("category"))
                    .result().orElse(CraftingBookCategory.MISC);
            JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ingredients = NonNullList.withSize(array.size(), Ingredient.EMPTY);
            for (int i = 0; i < array.size(); i++) ingredients.set(i, Ingredient.fromJson(array.get(i)));
            return new CanteenRefillRecipe(group, category,
                    net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(
                            GsonHelper.getAsJsonObject(json, "result")), ingredients);
        }

        @Override
        public CanteenRefillRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);
            ingredients.replaceAll(ingredient -> Ingredient.fromNetwork(buffer));
            return new CanteenRefillRecipe(group, category, buffer.readItem(), ingredients);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CanteenRefillRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
        }
    }
}
