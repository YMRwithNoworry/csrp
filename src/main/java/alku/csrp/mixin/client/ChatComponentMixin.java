package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

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

    @Inject(method = "addMessageToDisplayQueue", at = @At("RETURN"), require = 0)
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
            trimmedMessages.set(index, new GuiMessage.Line(line.addedTime(),
                    DerivedTextDistortion.bypass(line.content()), line.tag(), line.endOfEntry()));
        }
    }
}
