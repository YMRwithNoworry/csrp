package alku.csrp.registry;

import java.util.function.Supplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public final class ModTiers {
    public static final Tier LIVING = new Tier() {
        @Override public int getUses() { return 1000; }
        @Override public float getSpeed() { return 7.0F; }
        @Override public float getAttackDamageBonus() { return 0.0F; }
        @Override public int getLevel() { return 3; }
        @Override public int getEnchantmentValue() { return 14; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.LIVING_CORE.get()); }
    };

    public static final Tier HIJACKED_IRON = new Tier() {
        @Override public int getUses() { return 1561; }
        @Override public float getSpeed() { return 7.0F; }
        @Override public float getAttackDamageBonus() { return 2.5F; }
        @Override public int getLevel() { return 2; }
        @Override public int getEnchantmentValue() { return 14; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.BLOODY_IRON_INGOT.get()); }
    };

    private ModTiers() {
    }
}
