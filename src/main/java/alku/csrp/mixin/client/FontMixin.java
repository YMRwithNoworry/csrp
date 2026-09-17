package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public abstract class FontMixin {
    @ModifyVariable(
            method = "prepareText(Ljava/lang/String;FFIZI)"
                    + "Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String csrp$distortString(String text) {
        return DerivedTextDistortion.distort(text);
    }

    @ModifyVariable(
            method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)"
                    + "Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence csrp$distortSequence(FormattedCharSequence text) {
        return DerivedTextDistortion.distort(text);
    }

    @ModifyVariable(
            method = "prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)"
                    + "Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence csrp$distortOutlinedSequence(FormattedCharSequence text) {
        return DerivedTextDistortion.distort(text);
    }
}
