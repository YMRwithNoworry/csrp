package alku.csrp.mixin;

import alku.csrp.entity.ParasiteDespawnHandler;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在原版自然消失判定生效前，为寄生体执行原版 SRP 的 cyst/recall 流程。
 */
@Mixin(Mob.class)
public abstract class MobDespawnMixin {
    @Inject(method = "removeWhenFarAway", at = @At("HEAD"))
    private void csrp$beforeFarDespawn(double distance, CallbackInfoReturnable<Boolean> cir) {
        ParasiteDespawnHandler.onRemoveWhenFarAway((Mob) (Object) this);
    }
}
