package alku.nocubessrparmory;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

final class AddonArmorMaterials {
    static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, NoCubesSrpCombatAddon.MOD_ID);
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> TWISTED = register("twisted", 3, 6, 7, 3, 10, 0.5f);
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> PESTILENT = register("pestilent", 4, 7, 8, 4, 9, 2.0f);
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> GORE = register("gore", 6, 8, 9, 6, 9, 2.0f);
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> CARAPACE = register("carapace", 7, 10, 11, 7, 20, 2.0f);
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> EVOLUTION = register("evolution", 10, 13, 15, 10, 20, 3.0f);
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> LIVING = register("living", 9, 11, 13, 8, 20, 2.0f);
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> ADAPTIVE = register("adaptive", 9, 11, 13, 8, 9, 0.0f);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String id, int helmet, int chest,
            int legs, int boots, int enchantment, float toughness) {
        return MATERIALS.register(id, () -> new ArmorMaterial(
                Map.of(ArmorItem.Type.HELMET, helmet, ArmorItem.Type.CHESTPLATE, chest,
                        ArmorItem.Type.LEGGINGS, legs, ArmorItem.Type.BOOTS, boots),
                enchantment, SoundEvents.ARMOR_EQUIP_DIAMOND,
                () -> Ingredient.of(Items.IRON_INGOT),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                        NoCubesSrpCombatAddon.MOD_ID, id))), toughness, 0.0f));
    }
    private AddonArmorMaterials() {}
}
