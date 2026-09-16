package alku.csrp.registry;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import alku.csrp.Csrp;

public final class ModArmorMaterials {
    public static final ArmorMaterial LIVING = new ArmorMaterial(
            15, Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 10,
                    ArmorType.LEGGINGS, 8, ArmorType.BOOTS, 4), 15, SoundEvents.ARMOR_EQUIP_IRON,
            3.0F, 0.0F, repairTag("living_repair"), asset("livings"));
    public static final ArmorMaterial SENTIENT = new ArmorMaterial(
            18, Map.of(ArmorType.HELMET, 5, ArmorType.CHESTPLATE, 12,
                    ArmorType.LEGGINGS, 12, ArmorType.BOOTS, 5), 18, SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0F, 0.0F, repairTag("sentient_repair"), asset("sentients"));
    public static final ArmorMaterial HIJACKED_IRON = new ArmorMaterial(
            14, Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 9,
                    ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4), 14, SoundEvents.ARMOR_EQUIP_IRON,
            3.0F, 0.0F, repairTag("hijacked_iron_repair"), asset("hijacked_iron"));
    public static final ArmorMaterial VENKROL = new ArmorMaterial(
            20, Map.of(ArmorType.HELMET, 0, ArmorType.CHESTPLATE, 0,
                    ArmorType.LEGGINGS, 0, ArmorType.BOOTS, 4), 20, SoundEvents.ARMOR_EQUIP_IRON,
            3.0F, 0.0F, repairTag("venkrol_repair"), asset("venkrol_boot"));
    /** 原版 armor_mobility：防御{头3,胸7,腿6,靴3}、附魔18、韧性1.0；护甲层沿用原版的香草皮革贴图。 */
    public static final ArmorMaterial MOBILITY = new ArmorMaterial(
            5, Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 7,
                    ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3), 18,
            SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F,
            ItemTags.REPAIRS_LEATHER_ARMOR, EquipmentAssets.LEATHER);

    private static TagKey<Item> repairTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Csrp.MODID, path));
    }

    private static ResourceKey<EquipmentAsset> asset(String path) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Csrp.MODID, path));
    }

    private ModArmorMaterials() {
    }
}
