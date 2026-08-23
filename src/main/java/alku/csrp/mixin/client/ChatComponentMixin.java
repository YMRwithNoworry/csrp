package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Final
    @Shadow
    private List<GuiMessage.Line> f_93761_;

    @Inject(method = {"render", "m_280165_"}, at = @At("HEAD"), require = 0)
    private void csrp$beginChatDistortion(GuiGraphics graphics, int tickCount, int mouseX,
            int mouseY, CallbackInfo callback) {
        DerivedTextDistortion.beginRenderScope();
    }

    @Inject(method = {"render", "m_280165_"}, at = @At("RETURN"), require = 0)
    private void csrp$endChatDistortion(GuiGraphics graphics, int tickCount, int mouseX,
            int mouseY, CallbackInfo callback) {
        DerivedTextDistortion.endRenderScope();
    }

    @Inject(method = {
            "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            "m_240465_(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V"
    }, at = @At("RETURN"), require = 0)
    private void csrp$preserveSystemMessages(Component message, MessageSignature signature, int addedTime,
            GuiMessageTag tag, boolean displayOnly, CallbackInfo callback) {
        if (tag != GuiMessageTag.system() && tag != GuiMessageTag.systemSinglePlayer()) {
            return;
        }
        for (int index = 0; index < f_93761_.size(); index++) {
            GuiMessage.Line line = f_93761_.get(index);
            if (line.addedTime() != addedTime || line.tag() != tag) {
                break;
            }
            f_93761_.set(index, new GuiMessage.Line(line.addedTime(),
                    DerivedTextDistortion.bypass(line.content()), line.tag(), line.endOfEntry()));
        }
    }
}
