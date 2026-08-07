package alku.csrp.entity;

import java.util.UUID;

public interface SummonCapacityOwner {
    int getSummonCapacity();

    int getUsedSummonCapacity();

    void reserveTrackedSummon(UUID entityId, int cost);

    void replaceTrackedSummon(UUID previousId, UUID replacementId, int cost);

    void releaseTrackedSummon(UUID entityId);

    default boolean canReserveSummon(int cost) {
        return cost > 0 && getUsedSummonCapacity() + cost <= getSummonCapacity();
    }
}
