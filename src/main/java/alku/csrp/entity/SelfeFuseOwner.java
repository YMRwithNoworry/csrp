package alku.csrp.entity;

/**
 * The legacy SELFE self-destruct fuse contract.
 *
 * <p>Every parasite family implements it: the families deliberately extend {@code Monster} instead
 * of sharing a parasite superclass, so the fuse is a small composed component
 * ({@link ParasiteFuseState}) plus this interface, rather than a field on a common parent.
 */
public interface SelfeFuseOwner {
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
