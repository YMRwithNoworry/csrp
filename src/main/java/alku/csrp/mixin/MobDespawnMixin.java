package alku.csrp.mixin;

import alku.csrp.entity.ParasiteDespawnHandler;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在原版自然消失判定生效前，为寄生体执行原版 SRP 的 cyst/recall 流程。
 *
 * <p>注入点位于 {@code removeWhenFarAway} 的 HEAD，而原版 {@code Mob#checkDespawn}
 * 正是用它作为「是否移除」的判据，所以这里必须可取消：当收尾流程既回收不进调度柱、
 * 也落不下活体囊肿时返回 false，让原版保留这只生物，而不是让它凭空消失。
 */
@Mixin(Mob.class)
public abstract class MobDespawnMixin {
    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void csrp$beforeFarDespawn(double distance, CallbackInfoReturnable<Boolean> cir) {
        if (!ParasiteDespawnHandler.leaveDespawnTrace((Mob) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
