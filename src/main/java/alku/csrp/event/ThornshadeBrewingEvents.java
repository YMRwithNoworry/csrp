package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/** Awkward potion plus Thornshade Berry creates the legacy decanter. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ThornshadeBrewingEvents {
    private ThornshadeBrewingEvents() {
    }

    @SubscribeEvent
    public static void registerRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack input) {
                return input.is(Items.POTION)
                        && input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                                .is(Potions.AWKWARD);
            }

            @Override
            public boolean isIngredient(ItemStack ingredient) {
                return ingredient.is(ModItems.THORNSHADE_BERRY);
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                return isInput(input) && isIngredient(ingredient)
                        ? new ItemStack(ModItems.THORNSHADE_DECANTER.get()) : ItemStack.EMPTY;
            }
        });
    }
}
