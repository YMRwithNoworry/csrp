package alku.csrp.entity;

/** Entity-side contract used by the creative Variant Wand. */
public interface ManualVariantProvider {
    int getManualVariant();

    void setManualVariant(int variant);

    default int getMaxManualVariants() {
        return 10;
    }

    default void cycleManualVariant() {
        int next = getManualVariant() + 1;
        if (next >= getMaxManualVariants()) {
            next = 0;
        }
        setManualVariant(next);
    }
}
