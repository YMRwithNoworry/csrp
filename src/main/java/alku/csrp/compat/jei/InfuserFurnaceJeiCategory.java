package alku.csrp.compat.jei;

import alku.csrp.Csrp;
import alku.csrp.recipe.InfuserFurnaceRecipe;
import alku.csrp.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Port of SRP 1.10.9 {@code compatibility/jei/InfuserFurnaceJEICategory}.
 *
 * <p>The original's category showed the top 176x82 of {@code textures/gui/infuser_furnace.png} behind
 * two inputs and two outputs with an animated vanilla-furnace arrow at (79,34).  The slot coordinates
 * and the arrow are reproduced here; the background is drawn from the category's own {@code draw}
 * method because JEI 26.3 renders categories in the order "JEI background, category draw, slots,
 * widgets, drawables", so {@code draw} is the only hook that can paint under the slots.</p>
 */
public final class InfuserFurnaceJeiCategory extends AbstractRecipeCategory<InfuserFurnaceRecipe> {
    public static final Identifier UID = Identifier.fromNamespaceAndPath(Csrp.MODID, "infuser_furnace");
    public static final IRecipeType<InfuserFurnaceRecipe> RECIPE_TYPE =
            IRecipeType.create(UID, InfuserFurnaceRecipe.class);
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Csrp.MODID, "textures/gui/infuser_furnace.png");
    /** The original's background region: {@code createDrawable(BG, 0, 0, 176, 82)}. */
    private static final int WIDTH = 176;
    private static final int HEIGHT = 82;
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    /** The original animated the arrow over 200 ticks, left to right. */
    private static final int ARROW_TICKS = 200;

    private final IDrawable background;

    public InfuserFurnaceJeiCategory(IGuiHelper guiHelper) {
        super(RECIPE_TYPE, Component.literal("Infuser Furnace"),
                guiHelper.createDrawableItemLike(ModBlocks.INFUSER_FURNACE.get()), WIDTH, HEIGHT);
        this.background = guiHelper.createDrawable(BACKGROUND, 0, 0, WIDTH, HEIGHT);
    }

    /**
     * The original's {@code setRecipe} slot layout: smelt input (55,16), infuse input (29,34), then the
     * two outputs at (115,34) and (139,52).  Note the original assigned the infused output to the first
     * output slot and the bottle byproduct to the second, which is the reverse of the order those two
     * slots appear in the GUI — that quirk is preserved rather than silently corrected.
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfuserFurnaceRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(55, 16).add(recipe.smeltIn());
        builder.addInputSlot(29, 34).add(recipe.infuseIn());
        builder.addOutputSlot(115, 34).add(recipe.output());
        if (recipe.hasByproduct()) {
            builder.addOutputSlot(139, 52).add(recipe.byproduct());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, InfuserFurnaceRecipe recipe,
            IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(ARROW_TICKS).setPosition(ARROW_X, ARROW_Y);
    }

    @Override
    public void draw(InfuserFurnaceRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics,
            double mouseX, double mouseY) {
        this.background.draw(graphics, 0, 0);
    }

    /** The background is the mod's own GUI panel, which already has its frame. */
    @Override
    public boolean needsRecipeBorder() {
        return false;
    }
}
