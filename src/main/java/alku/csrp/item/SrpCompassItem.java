package alku.csrp.item;

import alku.csrp.util.NbtData;
import alku.csrp.world.SrpWorldData;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Points to the nearest persistent SRP core record in the current dimension. */
public final class SrpCompassItem extends Item {
    public static final String HAS_TARGET_TAG = "srp_compass_has_target";
    public static final String TARGET_POS_TAG = "srp_compass_target_pos";
    public static final String TARGET_DIMENSION_TAG = "srp_compass_target_dimension";
    private final Target target;

    public SrpCompassItem(Target target, Properties properties) {
        super(properties.stacksTo(1));
        this.target = target;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level instanceof ServerLevel serverLevel && entity.tickCount % 20 == 0) {
            updateTarget(stack, serverLevel, entity.blockPosition());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            BlockPos found = updateTarget(stack, serverLevel, player.blockPosition());
            if (found == null) {
                player.sendSystemMessage(Component.translatable("message.csrp.compass.not_found",
                        Component.translatable(target.translationKey)));
            } else {
                int distance = (int) Math.round(Math.sqrt(found.distSqr(player.blockPosition())));
                player.sendSystemMessage(Component.translatable("message.csrp.compass.found",
                        Component.translatable(target.translationKey), found.getX(), found.getY(), found.getZ(), distance));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = NbtData.copyTag(stack);
        if (!tag.getBoolean(HAS_TARGET_TAG)) {
            tooltip.add(Component.translatable("tooltip.csrp.compass.searching",
                    Component.translatable(target.translationKey)).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        BlockPos pos = BlockPos.of(tag.getLong(TARGET_POS_TAG));
        tooltip.add(Component.translatable("tooltip.csrp.compass.target",
                Component.translatable(target.translationKey), pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.GRAY));
    }

    private BlockPos updateTarget(ItemStack stack, ServerLevel level, BlockPos origin) {
        SrpWorldData data = SrpWorldData.get(level);
        Stream<BlockPos> positions = switch (target) {
            case NODE -> data.nodes().stream().map(SrpWorldData.NodeEntry::pos);
            case COLONY -> data.colonies().stream().map(SrpWorldData.ColonyEntry::pos);
            case ORIGIN -> data.vectors().stream().map(SrpWorldData.VectorEntry::pos);
        };
        BlockPos found = positions.min(Comparator.comparingDouble(pos -> pos.distSqr(origin))).orElse(null);
        CompoundTag current = NbtData.copyTag(stack);
        String dimension = level.dimension().location().toString();
        if (found == null) {
            if (current.getBoolean(HAS_TARGET_TAG)) {
                NbtData.update(stack, tag -> {
                    tag.putBoolean(HAS_TARGET_TAG, false);
                    tag.remove(TARGET_POS_TAG);
                    tag.remove(TARGET_DIMENSION_TAG);
                });
            }
            return null;
        }
        if (!current.getBoolean(HAS_TARGET_TAG) || current.getLong(TARGET_POS_TAG) != found.asLong()
                || !dimension.equals(current.getString(TARGET_DIMENSION_TAG))) {
            NbtData.update(stack, tag -> {
                tag.putBoolean(HAS_TARGET_TAG, true);
                tag.putLong(TARGET_POS_TAG, found.asLong());
                tag.putString(TARGET_DIMENSION_TAG, dimension);
            });
        }
        return found;
    }

    public enum Target {
        NODE("target.csrp.compass.node"),
        COLONY("target.csrp.compass.colony"),
        ORIGIN("target.csrp.compass.origin");

        private final String translationKey;

        Target(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
