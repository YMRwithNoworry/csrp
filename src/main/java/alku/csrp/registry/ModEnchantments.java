package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Code-backed enchantments required by the 1.20.1 Forge registry lifecycle. */
public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Csrp.MODID);

    public static final RegistryObject<Enchantment> PARASITE_KILLER = ENCHANTMENTS.register(
            "parasite_killer", () -> new Enchantment(Enchantment.Rarity.VERY_RARE,
                    EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {
                @Override
                public int getMaxLevel() {
                    return 3;
                }

                @Override
                public int getMinCost(int level) {
                    return 16 + (level - 1) * 5;
                }

                @Override
                public int getMaxCost(int level) {
                    return 21 + (level - 1) * 5;
                }
            });

    private ModEnchantments() {
    }
}
