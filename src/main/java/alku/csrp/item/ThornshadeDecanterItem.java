package alku.csrp.item;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.Csrp;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Legacy Thornshade Decanter: two useful doses, then delayed self-destruction. */
public final class ThornshadeDecanterItem extends Item {
    private static final int EFFECT_DURATION_TICKS = 400;
    private static final String USES_KEY = "csrpDecanterUses";
    private static final int EXPLOSION_USE = 3;

    public ThornshadeDecanterItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            user.addEffect(new MobEffectInstance(ModMobEffects.THORNSHADE_THORNS,
                    EFFECT_DURATION_TICKS, 0, false, true));
        }
        if (!(user instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!level.isClientSide && user instanceof Player drinker) {
            int uses = drinker.getPersistentData().getInt(USES_KEY) + 1;
            drinker.getPersistentData().putInt(USES_KEY, uses);
            if (uses >= EXPLOSION_USE) {
                drinker.getPersistentData().putInt(USES_KEY, 0);
                triggerSelfDestruction(level, drinker);
            }
        }
        return stack;
    }

    private static void triggerSelfDestruction(Level level, Player player) {
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.explode(null, player.getX(), player.getY() + 0.5D, player.getZ(),
                3.0F, Level.ExplosionInteraction.NONE);
        player.hurt(level.damageSources().explosion(null, null), 100.0F);
        serverLevel.sendParticles(ParticleTypes.POOF,
                player.getX(), player.getY() + 0.5D, player.getZ(),
                40, 0.5D, 0.5D, 0.5D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.2F, 0.8F);
        if (player instanceof ServerPlayer serverPlayer) {
            AdvancementHolder holder = serverPlayer.server.getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                            "beautiful_self_destruction"));
            if (holder != null) {
                serverPlayer.getAdvancements().award(holder, "triggered");
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.thornshade_decanter.line1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.csrp.thornshade_decanter.line2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
