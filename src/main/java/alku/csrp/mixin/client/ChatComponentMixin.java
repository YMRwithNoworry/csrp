package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    private static final String EXTRACT_RENDER_STATE =
            "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V";

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Inject(method = EXTRACT_RENDER_STATE, at = @At("HEAD"))
    private void csrp$beginChatDistortion(CallbackInfo callback) {
        DerivedTextDistortion.beginRenderScope();
    }

    @Inject(method = EXTRACT_RENDER_STATE, at = @At("RETURN"))
    private void csrp$endChatDistortion(CallbackInfo callback) {
        DerivedTextDistortion.endRenderScope();
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("RETURN"))
    private void csrp$preserveSystemMessages(GuiMessage message, CallbackInfo callback) {
        GuiMessageTag tag = message.tag();
        if (tag != GuiMessageTag.system() && tag != GuiMessageTag.systemSinglePlayer()) {
            return;
        }
        for (int index = 0; index < trimmedMessages.size(); index++) {
            GuiMessage.Line line = trimmedMessages.get(index);
            if (line.addedTime() != message.addedTime() || line.tag() != tag) {
                break;
            }
            trimmedMessages.set(index, new GuiMessage.Line(line.parent(),
                    DerivedTextDistortion.bypass(line.content()), line.endOfEntry()));
        }
    }
}
