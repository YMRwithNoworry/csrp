package alku.csrp.mixin;

import alku.csrp.registry.ModMobCategories;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.SpawnState.class)
public abstract class NaturalSpawnStateMixin {
    // Official and SRG selectors are both needed because this project has no refmap.
    // Only bypass the category caps: normal placement/position/finalization checks still run,
    // including EvolutionEvents' configurable per-dimension parasite population limit.
    @Inject(method = {
            "canSpawnForCategory(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/ChunkPos;)Z",
            "m_186548_(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/ChunkPos;)Z"
    }, at = @At("HEAD"), cancellable = true, require = 0)
    private void csrp$useIndependentParasiteCap(MobCategory category, ChunkPos chunkPos,
            CallbackInfoReturnable<Boolean> callback) {
        if (category == ModMobCategories.PARASITE) {
            callback.setReturnValue(true);
        }
    }
}
