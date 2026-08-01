package alku.csrp.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Shared crude-parasite traits from the legacy EntityPCrude base. */
public abstract class CrudeParasiteEntity extends PrimitiveParasiteEntity {
    protected CrudeParasiteEntity(EntityType<? extends CrudeParasiteEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance >= 60.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }
}
