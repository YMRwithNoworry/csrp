package alku.csrp.registry;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import alku.csrp.Csrp;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Csrp.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LIVING = MATERIALS.register(
            "living", () -> material(Map.of(ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 10,
                    ArmorItem.Type.LEGGINGS, 8, ArmorItem.Type.BOOTS, 4), 15, SoundEvents.ARMOR_EQUIP_IRON,
                    3.0F, 0.0F, "livings"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SENTIENT = MATERIALS.register(
            "sentient", () -> material(Map.of(ArmorItem.Type.HELMET, 5, ArmorItem.Type.CHESTPLATE, 12,
                    ArmorItem.Type.LEGGINGS, 12, ArmorItem.Type.BOOTS, 5), 18, SoundEvents.ARMOR_EQUIP_NETHERITE,
                    4.0F, 0.0F, "sentients"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HIJACKED_IRON = MATERIALS.register(
            "hijacked_iron", () -> material(Map.of(ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 9,
                    ArmorItem.Type.LEGGINGS, 7, ArmorItem.Type.BOOTS, 4), 14, SoundEvents.ARMOR_EQUIP_IRON,
                    3.0F, 0.0F, "hijacked_iron"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VENKROL = MATERIALS.register(
            "venkrol", () -> material(Map.of(ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0,
                    ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 4), 20, SoundEvents.ARMOR_EQUIP_IRON,
                    3.0F, 0.0F, "venkrol_boot"));

    private static ArmorMaterial material(Map<ArmorItem.Type, Integer> defense, int enchantmentValue,
            Holder<net.minecraft.sounds.SoundEvent> equipSound, float toughness, float knockbackResistance,
            String layer) {
        return new ArmorMaterial(defense, enchantmentValue, equipSound,
                () -> Ingredient.of(ModItems.BLOODY_IRON_INGOT.get()),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, layer))),
                toughness, knockbackResistance);
    }

    private ModArmorMaterials() {
    }
}
