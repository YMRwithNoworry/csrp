package alku.csrp.item;

import alku.csrp.util.NbtData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import alku.csrp.registry.ModTiers;
import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import alku.csrp.Config;

public class LivingWeaponItem extends SwordItem {
    public static final String KILLS = "srp_kills";
    private static final int EVOLUTION_HEALTH = 50_000;
    private static final ThreadLocal<Boolean> SCYTHE_SWEEP = ThreadLocal.withInitial(() -> false);
    private final boolean sentient;
    private final Supplier<? extends Item> next;
    private final float reach;
    private final WeaponKind kind;

    public LivingWeaponItem(WeaponKind kind, float damage, float attackSpeed, float reach, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        super(ModTiers.LIVING, Math.round(damage - 1.0F), attackSpeed, properties);
        this.sentient = sentient;
        this.next = next;
        this.reach = reach;
        this.kind = kind;
    }

    public boolean isSentient() { return sentient; }
    public float getReach() { return reach; }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && !target.level().isClientSide) applyWeaponEffect(stack, target, attacker);
        if (result && !target.level().isClientSide && target.isDeadOrDying()) {
            NbtData.update(stack, tag -> {
                tag.putInt(KILLS, tag.getInt(KILLS) + Math.round(target.getMaxHealth()));
            });
        }
        return result;
    }

    private void applyWeaponEffect(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float chance = sentient ? 0.50F : 0.25F;
        int amplifier = sentient ? 1 : 0;
        switch (kind) {
            case AXE -> applyChance(attacker, target, ModMobEffects.CORROSION.get(), 100, amplifier, chance);
            case SWORD -> applyChance(attacker, target, ModMobEffects.BLEED.get(), 100, amplifier, chance);
            case CLEAVER -> applyChance(attacker, target, ModMobEffects.VIRAL.get(), 100, amplifier, chance);
            case LANCE -> applyChance(attacker, target, ModMobEffects.NEEDLER.get(), 200, amplifier,
                    sentient ? 0.25F : 0.10F);
            case SCYTHE -> {
                if (!(attacker instanceof Player player) || SCYTHE_SWEEP.get()) break;
                double radius = sentient ? 8.0D : 4.0D;
                SCYTHE_SWEEP.set(true);
                try {
                    if (sentient) sweep(player, target, player.getBoundingBox().inflate(radius));
                    sweep(player, target, target.getBoundingBox().inflate(radius));
                } finally {
                    SCYTHE_SWEEP.set(false);
                }
            }
            case MAUL -> { }
        }
    }

    private static void sweep(Player player, LivingEntity primary, net.minecraft.world.phys.AABB area) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity != primary && entity.isAlive()
                        && !player.isAlliedTo(entity) && !entity.isAlliedTo(player)
                        && (!(entity instanceof TamableAnimal tame) || !tame.isTame()));
        for (LivingEntity entity : targets) player.attack(entity);
    }

    private static void applyChance(LivingEntity attacker, LivingEntity target,
            MobEffect effect, int duration,
            int amplifier, float chance) {
        if (attacker.getRandom().nextFloat() < chance) {
            MobEffectInstance current = target.getEffect(effect);
            if (current == null || effect == ModMobEffects.NEEDLER.get()) {
                target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
                return;
            }
            int newDuration = current.getDuration() + 40 <= duration ? duration : current.getDuration() + 10;
            int newAmplifier = Math.max(amplifier, Math.min(255, current.getAmplifier() + 1));
            target.addEffect(new MobEffectInstance(effect, newDuration, newAmplifier, false, false));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
            int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof LivingEntity holder)) return;
        if (sentient && holder.tickCount % 40 == 0 && Config.evolutionPhase(level) >= 2
                && holder.getRandom().nextInt(100) == 0) {
            holder.addEffect(new MobEffectInstance(ModMobEffects.PREY.get(), 1200, 0, false, false));
        }
        if (!sentient && holder.tickCount % 80 == 0) evolveIfReady(stack, holder);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int kills = NbtData.copyTag(stack).getInt(KILLS);
        tooltip.add(Component.translatable("tooltip.csrp.living_progress", kills, EVOLUTION_HEALTH));
    }

    protected void evolveIfReady(ItemStack stack, LivingEntity holder) {
        if (sentient || next == null || NbtData.copyTag(stack).getInt(KILLS) <= EVOLUTION_HEALTH) return;
        ItemStack evolved = new ItemStack(next.get());
        NbtData.update(stack, tag -> tag.putInt(KILLS, 0));
        stack.shrink(1);
        holder.spawnAtLocation(evolved);
        if (holder.level() instanceof ServerLevel serverLevel) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (lightning != null) {
                lightning.moveTo(holder.position());
                lightning.setVisualOnly(true);
                serverLevel.addFreshEntity(lightning);
            }
        }
    }

    public enum WeaponKind { SCYTHE, AXE, SWORD, CLEAVER, MAUL, LANCE }
}
