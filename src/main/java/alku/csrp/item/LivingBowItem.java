package alku.csrp.item;

import alku.csrp.util.NbtData;
import java.util.List;
import java.util.function.Supplier;
import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

public final class LivingBowItem extends BowItem {
    public static final String DAMAGE = "srp_damage";
    private static final int EVOLUTION_DAMAGE = 50_000;
    private static final ThreadLocal<Double> DRAW_SECONDS = ThreadLocal.withInitial(() -> 0.0D);
    private final boolean sentient;
    private final Supplier<? extends Item> next;

    public LivingBowItem(boolean sentient, Supplier<? extends Item> next, Item.Properties properties) {
        super(properties.durability(1000));
        this.sentient = sentient;
        this.next = next;
    }

    public boolean isSentient() { return sentient; }
    public Supplier<? extends Item> next() { return next; }

    @Override
    public void releaseUsing(ItemStack weapon, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;
        ItemStack ammo = player.getProjectile(weapon);
        if (ammo.isEmpty()) return;
        int charge = getUseDuration(weapon, user) - timeLeft;
        charge = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(weapon, level, player, charge, true);
        if (charge < 0) return;
        float power = getPowerForTime(charge);
        if (power < 0.1F) return;
        List<ItemStack> projectiles = draw(weapon, ammo, player);
        if (level instanceof ServerLevel serverLevel && !projectiles.isEmpty()) {
            DRAW_SECONDS.set(charge / 20.0D);
            try {
                shoot(serverLevel, player, player.getUsedItemHand(), weapon, projectiles,
                        power * 4.4F, 0.0F, power == 1.0F, null);
            } finally {
                DRAW_SECONDS.remove();
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity,
            float inaccuracy, float angle, @Nullable LivingEntity target) {
        projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle,
                0.0F, velocity, inaccuracy);
        if (projectile instanceof AbstractArrow arrow) {
            double multiplier = Math.min(DRAW_SECONDS.get(), 2.0D);
            arrow.setBaseDamage(arrow.getBaseDamage() * multiplier + 1.0D);
        }
        if (projectile instanceof Arrow arrow) {
            arrow.addEffect(new MobEffectInstance(ModMobEffects.BLEED.get(), 200, 0, false, true));
            arrow.addEffect(new MobEffectInstance(ModMobEffects.DOD_SMOKE_TRAIL.get(), 200, 0, false, true));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
            int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && sentient && entity instanceof LivingEntity holder
                && holder.tickCount % 40 == 0 && Config.evolutionPhase(level) >= 2
                && holder.getRandom().nextInt(10) == 0) {
            holder.addEffect(new MobEffectInstance(ModMobEffects.PREY.get(), 1200, 0, false, false));
        }
    }

    public void addDamage(ItemStack stack, float damage, LivingEntity holder) {
        if (holder.level().isClientSide) return;
        NbtData.update(stack, tag -> tag.putInt(DAMAGE,
                tag.getInt(DAMAGE) + Math.round(damage)));
        CompoundData.evolve(stack, holder, sentient, next, DAMAGE, EVOLUTION_DAMAGE);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int damage = NbtData.copyTag(stack).getInt(DAMAGE);
        tooltip.add(Component.translatable("tooltip.csrp.living_progress", damage, EVOLUTION_DAMAGE));
    }

    private static final class CompoundData {
        private static void evolve(ItemStack stack, LivingEntity holder, boolean sentient,
                Supplier<? extends Item> next, String key, int threshold) {
            if (sentient || next == null || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getInt(key) < threshold) return;
            ItemStack evolved = new ItemStack(next.get());
            stack.shrink(1);
            holder.spawnAtLocation(evolved);
        }
    }
}
