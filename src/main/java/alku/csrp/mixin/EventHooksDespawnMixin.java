package alku.csrp.mixin;

import alku.csrp.entity.ParasiteDespawnHandler;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Handles the explicit ALLOW branch, which discards inside NeoForge's event hook. */
@Mixin(value = EventHooks.class, remap = false)
public abstract class EventHooksDespawnMixin {
    @Inject(method = "checkMobDespawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;discard()V", remap = false))
    private static void csrp$beforeForcedDespawn(Mob mob, CallbackInfoReturnable<Boolean> cir) {
        ParasiteDespawnHandler.onNaturalDespawn(mob);
    }
}
