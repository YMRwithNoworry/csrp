package alku.csrp.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import alku.csrp.registry.ModTiers;

/** Hijacked sword implementation; the other hijacked tools share its event behavior. */
public final class HijackedToolItem extends SwordItem {
    public HijackedToolItem(Item.Properties properties) {
        super(ModTiers.HIJACKED_IRON, properties.attributes(SwordItem.createAttributes(ModTiers.HIJACKED_IRON, 6.5F, -2.4F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) HijackedHitEffects.apply(attacker, target);
        return result;
    }
}
