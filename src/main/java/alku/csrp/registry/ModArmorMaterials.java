package alku.csrp.registry;

import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** Armor materials in Forge 1.20.1 are plain values rather than registry entries. */
public final class ModArmorMaterials {
    public static final ArmorMaterial LIVING = material("living", Map.of(
            ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 10,
            ArmorItem.Type.LEGGINGS, 8, ArmorItem.Type.BOOTS, 4), 15,
            SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.0F, true);
    public static final ArmorMaterial SENTIENT = material("sentient", Map.of(
            ArmorItem.Type.HELMET, 5, ArmorItem.Type.CHESTPLATE, 12,
            ArmorItem.Type.LEGGINGS, 12, ArmorItem.Type.BOOTS, 5), 18,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.0F, true);
    public static final ArmorMaterial HIJACKED_IRON = material("hijacked_iron", Map.of(
            ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 9,
            ArmorItem.Type.LEGGINGS, 7, ArmorItem.Type.BOOTS, 4), 14,
            SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.0F, true);
    public static final ArmorMaterial VENKROL = material("venkrol", Map.of(
            ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0,
            ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 4), 20,
            SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.0F, true);
    public static final ArmorMaterial MOBILITY = material("mobility", Map.of(
            ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7,
            ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3), 18,
            SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F, false);

    private static ArmorMaterial material(String name, Map<ArmorItem.Type, Integer> defense,
            int enchantmentValue, SoundEvent equipSound, float toughness, float knockbackResistance,
            boolean modRepair) {
        return new ArmorMaterial() {
            private static final int[] DURABILITY = {13, 15, 16, 11};
            @Override public int getDurabilityForType(ArmorItem.Type type) {
                return DURABILITY[type.getSlot().getIndex()] * 15;
            }
            @Override public int getDefenseForType(ArmorItem.Type type) { return defense.getOrDefault(type, 0); }
            @Override public int getEnchantmentValue() { return enchantmentValue; }
            @Override public SoundEvent getEquipSound() { return equipSound; }
            @Override public Ingredient getRepairIngredient() {
                return modRepair ? Ingredient.of(ModItems.BLOODY_IRON_INGOT.get()) : Ingredient.EMPTY;
            }
            @Override public String getName() { return name; }
            @Override public float getToughness() { return toughness; }
            @Override public float getKnockbackResistance() { return knockbackResistance; }
        };
    }
    private ModArmorMaterials() {}
}
