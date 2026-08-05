package alku.csrp.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import alku.csrp.item.OverlastCanteenItem;
import alku.csrp.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

public final class CanteenRefillRecipe implements CraftingRecipe {
    private static final int SIPS_PER_REFILL = 2;

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
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) {
            return false;
        }
        if (!simple) {
            return RecipeMatcher.findMatches(input.items().stream().filter(stack -> !stack.isEmpty()).toList(),
                    ingredients) != null;
        }
        return input.size() == 1 && ingredients.size() == 1
                ? ingredients.getFirst().test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack output = result.copy();
        for (ItemStack ingredient : input.items()) {
            if (ingredient.getItem() instanceof OverlastCanteenItem) {
                OverlastCanteenItem.setState(output,
                        OverlastCanteenItem.getSips(ingredient) + SIPS_PER_REFILL,
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
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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

    public static final class Serializer implements RecipeSerializer<CanteenRefillRecipe> {
        private static final MapCodec<CanteenRefillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC)
                        .forGetter(recipe -> recipe.category),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(ingredients -> {
                    Ingredient[] values = ingredients.toArray(Ingredient[]::new);
                    if (values.length == 0) {
                        return DataResult.error(() -> "No ingredients for canteen recipe");
                    }
                    if (values.length > 9) {
                        return DataResult.error(() -> "Too many ingredients for canteen recipe");
                    }
                    return DataResult.success(NonNullList.of(Ingredient.EMPTY, values));
                }, DataResult::success).forGetter(recipe -> recipe.ingredients)
        ).apply(instance, CanteenRefillRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CanteenRefillRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<CanteenRefillRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CanteenRefillRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static CanteenRefillRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);
            ingredients.replaceAll(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            return new CanteenRefillRecipe(group, category, ItemStack.STREAM_CODEC.decode(buffer), ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, CanteenRefillRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    }
}
