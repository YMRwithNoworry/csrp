package alku.nocubessrparmory;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

final class AddonTiers {
    static final Tier TWISTED = tier(800, 10, 10, 10);
    static final Tier PESTILENT = tier(2000, 10, 15, 15);
    static final Tier GORE = tier(3000, 12, 15, 15);
    static final Tier CARAPACE = tier(4000, 10, 20, 20);
    static final Tier EVOLUTION = tier(10000, 16, 22, 20);
    static final Tier OVERLORD = tier(8000, 20, 96, 20);
    private static Tier tier(int uses, float speed, float damage, int enchantment) {
        return new Tier() {
            public int getUses() { return uses; }
            public float getSpeed() { return speed; }
            public float getAttackDamageBonus() { return damage; }
            public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
            public int getEnchantmentValue() { return enchantment; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        };
    }
    private AddonTiers() {}
}
