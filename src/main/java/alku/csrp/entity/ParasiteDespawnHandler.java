package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.block.entity.ParasiteCanisterBlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Comparator;
import java.util.List;

/**
 * 还原原版超距消失机制（EntityParasiteBase.func_70623_bb + spawnCyst/storeBefDes）：
 * 寄生体将被自然规则移除时，若附近有调度柱则回收入库（storeBefDes，之后可再部署），
 * 否则在脚下生成活体囊肿（spawnCyst），避免凭空消失。
 */
public final class ParasiteDespawnHandler {
    private static final double DISPATCHER_RECALL_RANGE = 48.0D;

    private ParasiteDespawnHandler() {
    }

    public static void onRemoveWhenFarAway(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !(mob instanceof Parasite)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null || !Csrp.MODID.equals(id.getNamespace())) {
            return;
        }
        NexusParasiteEntity dispatcher = findNearestDispatcher(level, mob);
        if (dispatcher != null && dispatcher.recallParasite(id.toString())) {
            level.sendParticles(ParticleTypes.PORTAL,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                    16, 0.3D, 0.4D, 0.3D, 0.02D);
            return;
        }
        ParasiteCanisterBlockEntity.placeFromDespawn(level, mob.blockPosition());
    }

    private static NexusParasiteEntity findNearestDispatcher(ServerLevel level, Mob mob) {
        List<NexusParasiteEntity> dispatchers = level.getEntitiesOfClass(NexusParasiteEntity.class,
                mob.getBoundingBox().inflate(DISPATCHER_RECALL_RANGE),
                entity -> entity.isAlive() && entity.isDispatcherFamily());
        return dispatchers.stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }
}
