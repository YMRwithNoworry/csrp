package alku.csrp.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import alku.csrp.item.OverlastCanteenItem;
import alku.csrp.registry.ModRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

public final class CanteenRefillRecipe implements CraftingRecipe {
    private final String group;
    private final CraftingBookCategory category;
    private final ItemStackTemplate result;
    private final NonNullList<Ingredient> ingredients;
    private final boolean simple;

    public CanteenRefillRecipe(String group, CraftingBookCategory category, ItemStackTemplate result,
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
        boolean ingredientsMatch = !simple
                ? RecipeMatcher.findMatches(input.items().stream().filter(stack -> !stack.isEmpty()).toList(),
                        ingredients) != null
                : input.size() == 1 && ingredients.size() == 1
                ? ingredients.getFirst().test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
        if (!ingredientsMatch) {
            return false;
        }
        for (ItemStack ingredient : input.items()) {
            if (ingredient.is(result.item())
                    && ingredient.getItem() instanceof OverlastCanteenItem
                    && OverlastCanteenItem.getSips(ingredient) >= OverlastCanteenItem.MAX_SIPS) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack output = result.create();
        for (ItemStack ingredient : input.items()) {
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
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredients);
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModRecipeSerializers.CANTEEN_SHAPELESS.get();
    }

    public static final MapCodec<CanteenRefillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC)
                    .forGetter(recipe -> recipe.category),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Ingredient.CODEC.listOf().fieldOf("ingredients").flatXmap(ingredients -> {
                if (ingredients.isEmpty()) {
                    return DataResult.error(() -> "No ingredients for canteen recipe");
                }
                if (ingredients.size() > 9) {
                    return DataResult.error(() -> "Too many ingredients for canteen recipe");
                }
                return DataResult.success(NonNullList.copyOf(ingredients));
            }, DataResult::success).forGetter(recipe -> recipe.ingredients)
    ).apply(instance, CanteenRefillRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanteenRefillRecipe> STREAM_CODEC = StreamCodec.of(
            CanteenRefillRecipe::toNetwork, CanteenRefillRecipe::fromNetwork);

    public static final RecipeSerializer<CanteenRefillRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private static CanteenRefillRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        int size = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int i = 0; i < size; i++) {
            ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
        }
        return new CanteenRefillRecipe(group, category, ItemStackTemplate.STREAM_CODEC.decode(buffer), ingredients);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, CanteenRefillRecipe recipe) {
        buffer.writeUtf(recipe.group);
        buffer.writeEnum(recipe.category);
        buffer.writeVarInt(recipe.ingredients.size());
        for (Ingredient ingredient : recipe.ingredients) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
        }
        ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result);
    }
}
