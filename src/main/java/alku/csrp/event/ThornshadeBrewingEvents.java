package alku.csrp.event;

import net.minecraft.world.item.alchemy.PotionUtils;
import alku.csrp.Csrp;
import alku.csrp.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Awkward potion plus Thornshade Berry creates the legacy decanter. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ThornshadeBrewingEvents {
    private ThornshadeBrewingEvents() {
    }

}
