package alku.csrp.gametest;

import alku.csrp.Csrp;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModPotions;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Runtime test for the brewing recipes ported from 1.10.9 {@code potion/Recipe.java}.
 *
 * <p>The original registered five {@code IBrewingRecipe}s programmatically; NeoForge 26.3 removed both
 * that interface and {@code RegisterBrewingRecipesEvent}, so they are shipped as
 * {@code data/csrp/recipe/brewing/*.json}.  This test loads them through the real
 * {@link RecipeManager}, so a malformed or missing JSON fails here rather than silently removing the
 * potion chain from the game.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class BrewingRecipeGameTests {
    private static final String MODID = Csrp.MODID;
    /** 1 base-to-fear + 1 fear-to-splash + 1 splash-to-lingering + 2 deadblood + 2 decanter. */
    private static final int EXPECTED_RECIPES = 7;
    private static final int MAX_TICKS = 200;
    private static final int SETUP_TICKS = 1;
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private BrewingRecipeGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(MODID, "brewing_recipes"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "brewing_recipes/legacy_srp_brews"),
                new LegacyBrewsTest(testData(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, SETUP_TICKS, true, Rotation.NONE);
    }

    private static final class LegacyBrewsTest extends GameTestInstance {
        LegacyBrewsTest(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
            super(testData);
        }

        @Override
        public void run(GameTestHelper helper) {
            RecipeManager manager = helper.getLevel().getServer().getRecipeManager();
            List<BrewingRecipe> ours = manager.getRecipes().stream()
                    .map(RecipeHolder::value)
                    .filter(BrewingRecipe.class::isInstance)
                    .map(BrewingRecipe.class::cast)
                    .filter(LegacyBrewsTest::isLegacySrpBrew)
                    .toList();

            helper.assertTrue(ours.size() == EXPECTED_RECIPES,
                    Component.literal("expected " + EXPECTED_RECIPES + " legacy SRP brewing recipes but loaded "
                            + ours.size()));

            // The original's BaseToFearRecipe consumes the alveolar fluid with a flint reagent
            // (potion/Recipe.java:64 -> Items.field_151145_ak, which the official 1.12.2 MCP mappings
            // resolve to Items.FLINT).
            boolean baseStep = ours.stream().anyMatch(recipe ->
                    recipe.getInput().test(new ItemStack(ModItems.ALVEOLAR_FLUID.get()))
                            && recipe.getReagent().test(new ItemStack(Items.FLINT))
                            && isFearPotion(recipe.getOutput().create()));
            helper.assertTrue(baseStep,
                    Component.literal("the alveolar-fluid + flint -> fear potion step is missing"));

            boolean deadblood = ours.stream().anyMatch(recipe ->
                    recipe.getReagent().test(new ItemStack(ModItems.DISEASED_SPONGE.get()))
                            && recipe.getOutput().create().is(ModItems.DEADBLOOD_FLUID.get()));
            helper.assertTrue(deadblood,
                    Component.literal("the diseased sponge -> deadblood step is missing"));

            boolean decanter = ours.stream().anyMatch(recipe ->
                    recipe.getReagent().test(new ItemStack(ModItems.THORNSHADE_BERRY.get()))
                            && recipe.getOutput().create().is(ModItems.THORNSHADE_DECANTER.get()));
            helper.assertTrue(decanter,
                    Component.literal("the thornshade berry -> decanter step is missing"));

            helper.succeed();
        }

        /** One of the three outputs the original's brewing recipes produced. */
        private static boolean isLegacySrpBrew(BrewingRecipe recipe) {
            ItemStack output = recipe.getOutput().create();
            if (isFearPotion(output)) {
                return true;
            }
            return output.is(ModItems.DEADBLOOD_FLUID.get())
                    || output.is(ModItems.THORNSHADE_DECANTER.get());
        }

        private static boolean isFearPotion(ItemStack stack) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            return contents != null
                    && contents.potion().map(holder -> holder.value() == ModPotions.FEAR.get()).orElse(false);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("csrp legacy SRP brewing recipes");
        }
    }
}
