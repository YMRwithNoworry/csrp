package alku.csrp.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Legacy {@code EntityParasiteBase} SELFE fuse, shared by every parasite family.
 *
 * <p>The original rolled {@code madeRng} once, on the first hit: a mob that rolled 0 kept the value
 * for the rest of its life and, on death, {@code onDeathUpdate} burned a {@code fuseTime} (40 tick)
 * fuse before {@code selfExplode} ran. The synced {@code SELFE} field tells the client how far the
 * fuse has burned so the renderer can swell the model.
 *
 * <p>{@code defineId} only fixes the accessor's value type, so one shared accessor can be
 * registered by every family from its own {@code defineSynchedData}.
 */
public final class ParasiteFuseState {
    /** Legacy fuseTime. */
    public static final int FUSE_TICKS = 40;
    /** Death animation length: the corpse stops advancing past it while the fuse burns. */
    public static final int DEATH_ANIMATION_TICKS = 20;
    /** Legacy {@code DataManager.register SELFE (int)}; -1 means no fuse is burning. */
    public static final EntityDataAccessor<Integer> SELFE =
            SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);

    private byte explodesOnDeath = -1;
    /** Per-owner fuse length; the legacy base is 40 but several mobs override it (e.g. the horse: 70). */
    private int fuseTicks = FUSE_TICKS;

    /** Legacy per-mob {@code fuseTime} override (EntityInfHorse:53 = 70, preeminent/pure = 70, Buthol = 30). */
    public void setFuseTicks(int ticks) {
        this.fuseTicks = Math.max(1, ticks);
    }

    /** Legacy madeRng: decided by the first hit and remembered for the rest of the mob's life. */
    public boolean willExplodeOnDeath(LivingEntity owner) {
        if (explodesOnDeath < 0) {
            explodesOnDeath = (byte) owner.getRandom().nextInt(2);
            if (explodesOnDeath == 0 && !owner.level().isClientSide) {
                owner.level().broadcastEntityEvent(owner, (byte) 40);
            }
        }
        return explodesOnDeath == 0;
    }

    public void start(LivingEntity owner) {
        if (getState(owner) < 0) {
            setState(owner, 0);
        }
    }

    public boolean isActive(LivingEntity owner) {
        return getState(owner) >= 0;
    }

    public void clear(LivingEntity owner) {
        setState(owner, -1);
    }

    public int getState(LivingEntity owner) {
        return owner.getEntityData().get(SELFE);
    }

    public void setState(LivingEntity owner, int state) {
        owner.getEntityData().set(SELFE, state);
    }

    /** @return true once the fuse reached {@link #FUSE_TICKS} and the burst should run */
    public boolean advance(LivingEntity owner) {
        int next = getState(owner) + 1;
        setState(owner, next);
        return next >= fuseTicks;
    }

    /** Legacy getSelfeFlashIntensity: 0..1 across the fuse (the original divides by fuseTime - 2). */
    public float flashIntensity(LivingEntity owner, float partialTick) {
        int fuse = getState(owner);
        if (fuse < 0) {
            return 0.0F;
        }
        return Mth.clamp((fuse + partialTick) / (float) (fuseTicks - 2), 0.0F, 1.0F);
    }
}
