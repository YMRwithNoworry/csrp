package alku.csrp.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A typed Relay Tower module. The kind is consumed by relay scan logic. */
public final class RelayModuleItem extends Item {
    private final Kind kind;

    public RelayModuleItem(Kind kind, Properties properties) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp." + kind.id)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.csrp.relay_module_use")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public enum Kind {
        INBORN("module_inborn"),
        ASSIMILATED("module_assimilated"),
        ASSIMARA("module_assimara"),
        HIJACKED("module_hijacked"),
        FERAL("module_feral"),
        CRUDE("module_crude"),
        PRIMITIVE("module_primitive"),
        ADAPTED("module_adapted"),
        NEXUS("module_nexus"),
        DETERRENT("module_deterrent"),
        PURE("module_pure"),
        PREEMINENT("module_preeminent"),
        ANCIENT("module_ancient"),
        DERIVED("module_derived"),
        DESMOID("module_desmoid"),
        ESCHAR("module_eschar"),
        RESISTANCE("module_resistance"),
        IDEAL("module_ideal"),
        ORIGIN("module_origin"),
        PHASE("module_phase"),
        VECTORS("module_vectors"),
        DISLODGEMENT("module_dislodgement");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }
}
