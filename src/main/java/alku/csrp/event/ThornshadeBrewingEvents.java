package alku.csrp.event;

import alku.csrp.Csrp;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Awkward potion plus Thornshade Berry creates the legacy decanter.
 *
 * <p>NEEDS-DESIGN: NeoForge 26.3 removed {@code IBrewingRecipe} and
 * {@code RegisterBrewingRecipesEvent}; brewing is now data-driven through the
 * vanilla {@code BrewingRecipe} type (input reagent / output). This programmatic
 * handler must be replaced by a {@code csrp:thornshade_decanter} brewing recipe
 * JSON to restore the potion behaviour.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ThornshadeBrewingEvents {
    private ThornshadeBrewingEvents() {
    }
}
