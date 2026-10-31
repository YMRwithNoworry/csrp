package alku.csrp.mixin;

import alku.csrp.entity.ParasiteDespawnHandler;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the parasite despawn effects only on the two distance-based discard paths. */
@Mixin(Mob.class)
public abstract class MobDespawnMixin {
    @Inject(method = "checkDespawn", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V", ordinal = 1),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V", ordinal = 2)
    })
    private void csrp$beforeFarDespawn(CallbackInfo ci) {
        ParasiteDespawnHandler.onNaturalDespawn((Mob) (Object) this);
    }
}
