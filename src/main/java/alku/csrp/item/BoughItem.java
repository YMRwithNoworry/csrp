package alku.csrp.item;

import alku.csrp.Csrp;
import alku.csrp.entity.SimAdventurerEntity;
import alku.csrp.registry.ModDamageTypes;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class BoughItem extends Item {
    public static final int USE_DURATION = 40;
    private static final ResourceLocation ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "sepeku");
    private static final String ADVANCEMENT_CRITERION = "sepeku";

    public BoughItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return USE_DURATION; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (level instanceof ServerLevel serverLevel) {
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6, 255, false, false));
            user.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 6, 0, false, false));
            user.setDeltaMovement(0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                            Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    user.getX(), user.getY() + user.getBbHeight() * 0.45D, user.getZ(),
                    8, 0.25D, 0.4D, 0.25D, 0.06D);
            if (remainingUseDuration % 10 == 0) {
                level.playSound(null, user.blockPosition(), ModSounds.MOVING_FLESH_GROW.get(),
                        SoundSource.PLAYERS, 0.8F, 0.7F + level.random.nextFloat() * 0.2F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return stack;
        }
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        user.removeEffect(ModMobEffects.RAGE);
        user.invulnerableTime = 0;
        user.hurt(seppukuDamage(serverLevel), Float.MAX_VALUE);
        if (user instanceof ServerPlayer player && !player.isAlive()) {
            spawnAssimilatedAdventurers(serverLevel, player);
            awardAdvancement(player);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.bough.line1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.csrp.bough.line2")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static DamageSource seppukuDamage(ServerLevel level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.SEPPEKU));
    }

    private static void spawnAssimilatedAdventurers(ServerLevel level, ServerPlayer player) {
        int count = 1 + level.random.nextInt(2);
        for (int index = 0; index < count; index++) {
            SimAdventurerEntity adventurer = ModEntities.SIM_ADVENTURER.get().create(level);
            if (adventurer == null) {
                continue;
            }
            adventurer.moveTo(player.getX() + (level.random.nextDouble() - 0.5D) * 1.5D,
                    player.getY(), player.getZ() + (level.random.nextDouble() - 0.5D) * 1.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            adventurer.finalizeSpawn(level, level.getCurrentDifficultyAt(adventurer.blockPosition()),
                    MobSpawnType.TRIGGERED, null);
            level.addFreshEntity(adventurer);
        }
    }

    private static void awardAdvancement(ServerPlayer player) {
        AdvancementHolder advancement = player.server.getAdvancements().get(ADVANCEMENT_ID);
        if (advancement != null) {
            player.getAdvancements().award(advancement, ADVANCEMENT_CRITERION);
        }
    }
}
