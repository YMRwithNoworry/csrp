package alku.csrp.item;
import net.minecraft.world.level.Level;

import alku.csrp.entity.Parasite;
import alku.csrp.entity.ParasiteTransformation;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Creative-only wand for forcing one step of parasite evolution or devolution. */
public final class ParasiteEvolutionWandItem extends Item {
    private final Mode mode;

    public ParasiteEvolutionWandItem(Mode mode, Properties properties) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
            InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof Parasite)) {
            return InteractionResult.PASS;
        }
        if (target.level().isClientSide) {
            return mode == Mode.DEVOLUTION || ParasiteTransformation.canEvolve(target)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(target.level() instanceof ServerLevel)) {
            return InteractionResult.PASS;
        }
        boolean changed = mode == Mode.EVOLUTION
                ? ParasiteTransformation.evolve(target) : ParasiteTransformation.devolve(target);
        return changed ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context, List<Component> tooltip, TooltipFlag flag) {
        String tooltipKey = "tooltip.csrp." + mode.translationKey;
        tooltip.add(Component.translatable(tooltipKey,
                Component.translatable(tooltipKey + ".action").withStyle(ChatFormatting.RED)));
    }

    public enum Mode {
        EVOLUTION("itemevolve"),
        DEVOLUTION("itemdevolve");

        private final String translationKey;

        Mode(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
