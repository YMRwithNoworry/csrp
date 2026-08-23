package alku.csrp.item;

import java.util.List;
import java.util.function.Supplier;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LivingMaulItem extends LivingWeaponItem {
    private static final String DASH = "srp_maul_dash";
    private static final String DASH_TICKS = "srp_maul_dash_t";
    private static final String DASH_X = "srp_maul_dash_dx";
    private static final String DASH_Y = "srp_maul_dash_dy";
    private static final String DASH_Z = "srp_maul_dash_dz";
    private static final String DASH_STEP = "srp_maul_dash_step";
    private static final String SLAM_PENDING = "srp_maul_slam_pending";
    private static final String SLAM_TICKS = "srp_maul_slam_pending_t";

    public LivingMaulItem(float damage, float speed, float reach, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        super(WeaponKind.MAUL, damage, speed, reach, sentient, next, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isSentient() ? UseAnim.BOW : UseAnim.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        return isSentient() ? 72000 : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSentient() && player.isShiftKeyDown() && pending(player) && !player.onGround()) {
            player.setDeltaMovement(0.0D, -3.5D, 0.0D);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (isSentient()) {
            player.startUsingItem(hand);
        } else if (!level.isClientSide) {
            slam((ServerLevel) level, player, stack);
            player.getCooldowns().addCooldown(this, 200);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!isSentient() || !(entity instanceof Player player) || level.isClientSide
                || player.getCooldowns().isOnCooldown(this)) return;
        int used = getUseDuration(stack) - timeLeft;
        if (used < 6) return;
        float charge = Math.min(1.0F, ((used / 20.0F) * (used / 20.0F) + used / 10.0F) / 3.0F);
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, Math.max(-0.25D, Math.min(0.95D, look.y)), look.z).normalize();
        int ticks = Math.max(3, (int) Math.ceil(4.0F * charge));
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean(DASH, true);
        tag.putInt(DASH_TICKS, ticks);
        tag.putDouble(DASH_X, direction.x);
        tag.putDouble(DASH_Y, direction.y);
        tag.putDouble(DASH_Z, direction.z);
        tag.putDouble(DASH_STEP, 16.0D * charge / ticks);
        tag.putBoolean(SLAM_PENDING, false);
        player.getCooldowns().addCooldown(this, 500);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!isSentient() || level.isClientSide || !(entity instanceof Player player)) return;
        CompoundTag tag = player.getPersistentData();
        if (tag.getBoolean(SLAM_PENDING)) {
            player.fallDistance = 0.0F;
            if (player.onGround() || player.isInWater() || player.isInLava()) {
                slam((ServerLevel) level, player, stack);
                clearPending(tag);
            } else if (tag.getInt(SLAM_TICKS) <= 0) {
                clearPending(tag);
            } else {
                tag.putInt(SLAM_TICKS, tag.getInt(SLAM_TICKS) - 1);
            }
        }
        if (!tag.getBoolean(DASH)) return;
        if (!selected || player.getMainHandItem().getItem() != this) {
            armSlam(tag);
            return;
        }
        int ticks = tag.getInt(DASH_TICKS);
        if (ticks <= 0 || player.horizontalCollision || player.verticalCollision) {
            armSlam(tag);
            return;
        }
        double step = tag.getDouble(DASH_STEP);
        Vec3 movement = new Vec3(tag.getDouble(DASH_X) * step,
                Math.max(-1.25D, Math.min(1.25D, tag.getDouble(DASH_Y) * step)),
                tag.getDouble(DASH_Z) * step);
        List<LivingEntity> collisions = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(movement).inflate(1.0D), target -> validTarget(player, target));
        if (!collisions.isEmpty()) {
            slam((ServerLevel) level, player, stack);
            collisions.getFirst().hurt(player.damageSources().playerAttack(player),
                    (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F);
            tag.putBoolean(DASH, false);
            clearPending(tag);
            return;
        }
        player.setDeltaMovement(movement);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        tag.putInt(DASH_TICKS, ticks - 1);
    }

    private static boolean pending(Player player) {
        return player.getPersistentData().getBoolean(SLAM_PENDING);
    }

    private static void armSlam(CompoundTag tag) {
        tag.putBoolean(DASH, false);
        tag.putBoolean(SLAM_PENDING, true);
        tag.putInt(SLAM_TICKS, 200);
    }

    private static void clearPending(CompoundTag tag) {
        tag.putBoolean(SLAM_PENDING, false);
        tag.putInt(SLAM_TICKS, 0);
    }

    private static boolean validTarget(Player player, LivingEntity target) {
        return target != player && target.isAlive() && !player.isAlliedTo(target) && !target.isAlliedTo(player)
                && (!(target instanceof TamableAnimal tame) || !tame.isTame());
    }

    private static void slam(ServerLevel level, Player player, ItemStack stack) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(4.0D, 1.5D, 4.0D), entity -> validTarget(player, entity))) {
            var source = player.damageSources().playerAttack(player);
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F;
            target.hurt(source, damage);
            Vec3 away = target.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            target.push(away.x * 1.25D, Math.max(0.85D, target.getDeltaMovement().y), away.z * 1.25D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0D, player.getZ(),
                16, 1.4D, 0.25D, 1.4D, 0.0D);
        BlockPos center = player.blockPosition();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            if (x * x + z * z > 16) continue;
            BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    center.offset(x, 0, z));
            var state = level.getBlockState(ground.below());
            if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, 6,
                    0.35D, 0.06D, 0.35D, 0.15D);
        }
        level.playSound(null, player.blockPosition(), ModSounds.get("vengeance.attack.rock"),
                SoundSource.PLAYERS, 0.9F, 0.9F);
        stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        player.getPersistentData().putBoolean(DASH, false);
    }
}
