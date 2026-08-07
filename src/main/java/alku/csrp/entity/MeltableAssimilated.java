package alku.csrp.entity;

/** An assimilated body that can participate in the legacy Moving Flesh merge system. */
public interface MeltableAssimilated {
    boolean canMelt();

    boolean isMelting();

    void melt();

    float getMeltRenderScale(float partialTick);
}
