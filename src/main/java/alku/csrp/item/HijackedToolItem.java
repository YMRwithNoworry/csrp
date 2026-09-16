package alku.csrp.item;

import alku.csrp.registry.ModTiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Hijacked sword implementation; the other hijacked tools share its event behavior. */
public final class HijackedToolItem extends Item {
    public HijackedToolItem(Item.Properties properties) {
        super(properties.sword(ModTiers.HIJACKED_IRON, 6.5F, -2.4F));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        HijackedHitEffects.apply(attacker, target);
    }
}
