package alku.csrp.entity;

import net.minecraft.network.syncher.EntityDataAccessor;

/**
 * The legacy SELFE self-destruct fuse contract.
 *
 * <p>Every parasite family implements it: the families deliberately extend {@code Monster} instead
 * of sharing a parasite superclass, so the fuse is a small composed component
 * ({@link ParasiteFuseState}) plus this interface, rather than a field on a common parent.
 */
public interface SelfeFuseOwner {
    /**
     * Legacy {@code DataManager.register SELFE (int)} accessor of this family.
     *
     * <p>Each family registers its own accessor instead of sharing one: {@code SynchedEntityData.defineId}
     * hands out ids per class tree, so a single accessor registered on {@code LivingEntity} can receive
     * the same id as a family's own accessor — whichever class initialises first decides the outcome,
     * and the loser crashes with {@code IllegalArgumentException: Duplicate id value}.
     */
    EntityDataAccessor<Integer> selfeAccessor();

    /**
     * Legacy madeRng: rolled on the first hit, {@code true} when this mob holds its corpse and
     * bursts {@link ParasiteFuseState#FUSE_TICKS} ticks after dying.
     */
    boolean willExplodeOnDeath();

    /** Legacy dyingBurst: arms the fuse on a freshly dead corpse. */
    void startDyingFuse();

    boolean isDyingFuseActive();

    /** Legacy getSelfeFlashIntensity, consumed by the preRenderCallback swell. */
    float getSelfeFlashIntensity(float partialTick);
}
