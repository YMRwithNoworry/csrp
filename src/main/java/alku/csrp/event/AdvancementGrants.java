package alku.csrp.event;

import alku.csrp.Csrp;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared helpers for granting the reconstructed SRP advancements.
 *
 * <p>The original mod (1.10.9) grants most of its hidden advancements through
 * {@code AdvancementProgress#getRemainingCriteria()} loops, so several call
 * sites must award <em>every</em> criterion that is still pending rather than a
 * single named one. {@link #awardAllPending(ServerPlayer, String)} mirrors that
 * behaviour.
 */
public final class AdvancementGrants {
    private AdvancementGrants() {
    }

    /** Resolves an advancement in this mod's namespace, or {@code null} when it is not loaded. */
    public static AdvancementHolder holder(ServerPlayer player, String path) {
        return player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path));
    }

    /** Awards one criterion; returns {@code true} only when it was actually newly granted. */
    public static boolean award(ServerPlayer player, String path, String criterion) {
        AdvancementHolder holder = holder(player, path);
        if (holder == null) {
            return false;
        }
        // award() returns false when the criterion was already obtained.
        return player.getAdvancements().award(holder, criterion);
    }

    /**
     * Awards every criterion of the advancement that is still pending.
     *
     * <p>Equivalent to the original {@code grantRicardoShearAdvancement} /
     * {@code PacketBestiarySeenCelestial#grantAdvancement} /
     * {@code CelestialEventManager#grantDarkDaysAdvancement} helpers.
     *
     * @return {@code true} when at least one criterion was newly granted
     */
    public static boolean awardAllPending(ServerPlayer player, String path) {
        AdvancementHolder holder = holder(player, path);
        if (holder == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) {
            return false;
        }
        List<String> pending = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) {
            pending.add(criterion);
        }
        boolean granted = false;
        for (String criterion : pending) {
            granted |= player.getAdvancements().award(holder, criterion);
        }
        return granted;
    }
}
