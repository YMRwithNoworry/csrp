package alku.csrp.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public final class HijackedArmorItem extends Item {
    public HijackedArmorItem(Holder<ArmorMaterial> material, ArmorType type, Item.Properties properties) {
        super(properties.humanoidArmor(material.value(), type));
    }
}
