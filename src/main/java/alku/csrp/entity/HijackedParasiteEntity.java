package alku.csrp.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Shared hostile state for legacy hijacked mobs. */
public abstract class HijackedParasiteEntity extends PrimitiveParasiteEntity {
    protected HijackedParasiteEntity(EntityType<? extends HijackedParasiteEntity> type, Level level, int experience) {
        super(type, level);
        xpReward = experience;
    }

    protected static AttributeSupplier.Builder createAttributes(double health, double armor, double damage,
                                                                 double knockbackResistance, double movementSpeed,
                                                                 double followRange) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.MOVEMENT_SPEED, movementSpeed)
                .add(Attributes.FOLLOW_RANGE, followRange);
    }

    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }

}
