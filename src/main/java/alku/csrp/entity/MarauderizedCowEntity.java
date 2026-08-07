package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Legacy Marauderized cow: its ranged attack leaves a virulent, corrosive vomit cloud. */
public final class MarauderizedCowEntity extends MarauderizedParasiteEntity {
    private static final byte VOMIT_EVENT = 100;
    private static final int VOMIT_COOLDOWN_TICKS = 200;

    private int vomitTicks;

    public MarauderizedCowEntity(EntityType<? extends MarauderizedCowEntity> type, Level level) {
        super(type, level, 12, AnimationProfile.COW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMarauderizedAttributes(38.0D, 8.0D, 15.0D, 0.8D, 0.20D, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new VomitGoal());
    }

    @Override
    protected double meleeSpeed() {
        return 1.5D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && vomitTicks-- > 0) {
            spawnVomitParticles();
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            int count = 3 + random.nextInt(2);
            for (int index = 0; index < count; index++) {
                BuglinEntity buglin = ModEntities.BUGLIN.get().create(serverLevel);
                if (buglin == null) {
                    continue;
                }
                buglin.moveTo(getX() + (random.nextDouble() - 0.5D) * 1.5D,
                        getY() + getBbHeight() * 0.5D + 0.5D,
                        getZ() + (random.nextDouble() - 0.5D) * 1.5D, getYRot(), 0.0F);
                buglin.setTarget(getTarget());
                serverLevel.addFreshEntity(buglin);
            }
        }
        super.die(source);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == VOMIT_EVENT) {
            vomitTicks = 40;
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void spitVomit(LivingEntity target) {
        ParasiteCombatEffects.spawnVomitCloud(this, 4.5D, 3.0F, 100, 300, 20);
        startAttackAnimation();
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        level().broadcastEntityEvent(this, VOMIT_EVENT);
    }

    private void spawnVomitParticles() {
        Vec3 direction = getViewVector(1.0F);
        Vec3 start = getEyePosition().add(direction.scale(1.2D));
        for (int index = 0; index < 6; index++) {
            level().addParticle(ParticleTypes.SNEEZE, start.x, start.y - 0.2D, start.z,
                    direction.x * 0.2D + (random.nextDouble() - 0.5D) * 0.25D,
                    0.02D + random.nextDouble() * 0.1D,
                    direction.z * 0.2D + (random.nextDouble() - 0.5D) * 0.25D);
        }
    }

    private final class VomitGoal extends Goal {
        private int cooldown;
        private boolean fired;

        private VomitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && hasLineOfSight(target)
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 49.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return !fired;
        }

        @Override
        public void start() {
            fired = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                spitVomit(target);
            }
            fired = true;
        }

        @Override
        public void stop() {
            cooldown = VOMIT_COOLDOWN_TICKS;
        }
    }
}
