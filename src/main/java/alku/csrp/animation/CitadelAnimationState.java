package alku.csrp.animation;

/** Per-frame inputs exposed to the migrated entity animation predicates. */
public final class CitadelAnimationState<T> {
    private final T animatable;
    private final boolean moving;
    private final float partialTick;
    private CitadelRawAnimation selectedAnimation;

    public CitadelAnimationState(T animatable, boolean moving, float partialTick) {
        this.animatable = animatable;
        this.moving = moving;
        this.partialTick = partialTick;
    }

    public CitadelPlayState setAndContinue(CitadelRawAnimation animation) {
        selectedAnimation = animation;
        return CitadelPlayState.CONTINUE;
    }

    public T getAnimatable() {
        return animatable;
    }

    public boolean isMoving() {
        return moving;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public CitadelRawAnimation selectedAnimation() {
        return selectedAnimation;
    }
}
