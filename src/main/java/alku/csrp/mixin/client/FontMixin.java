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
            method = {"drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
                    "m_271880_(Ljava/lang/String;FFIZLorg/joml/Matrix4f;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I"},
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String csrp$distortString(String text) {
        return DerivedTextDistortion.distort(text);
    }

    @ModifyVariable(
            method = {"drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
                    "m_272085_(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/gui/Font$DisplayMode;II)I"},
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence csrp$distortSequence(FormattedCharSequence text) {
        return DerivedTextDistortion.distort(text);
    }

    @ModifyVariable(
            method = {"drawInBatch8xOutline(Lnet/minecraft/util/FormattedCharSequence;FFII"
                    + "Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    "m_168645_(Lnet/minecraft/util/FormattedCharSequence;FFII"
                    + "Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence csrp$distortOutlinedSequence(FormattedCharSequence text) {
        return DerivedTextDistortion.distort(text);
    }
}
