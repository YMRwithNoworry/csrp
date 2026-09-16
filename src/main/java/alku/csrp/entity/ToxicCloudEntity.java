package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntitySpawnReason;

public final class ToxicCloudEntity extends AreaEffectCloud {
    public ToxicCloudEntity(EntityType<? extends ToxicCloudEntity> type, Level level) {
        super(type, level);
    }

    public static ToxicCloudEntity create(Level level, double x, double y, double z) {
        ToxicCloudEntity cloud = ModEntities.CLOUD_TOXIC.get().create(level, new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, true));
        if (cloud == null) {
            throw new IllegalStateException("cloudtoxic entity type is not available");
        }
        cloud.setPos(x, y, z);
        return cloud;
    }
}
