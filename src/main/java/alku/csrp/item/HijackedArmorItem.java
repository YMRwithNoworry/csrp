package alku.csrp.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

public final class HijackedArmorItem extends ArmorItem {
    public HijackedArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
