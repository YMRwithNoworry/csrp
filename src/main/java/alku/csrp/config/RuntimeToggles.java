package alku.csrp.config;

/**
 * Runtime switches driven by the original {@code /srparasites} root command.
 *
 * <p>SRP 1.10.9 keeps two mutable flags that are not plain configuration entries:
 * {@code SRPConfig.doTileDrops} (toggled by {@code toggle_dotiledrops}, re-read from
 * {@code SRPConfig} on reload) and {@code ParasiteEventEntity.canSpawnNext} (toggled by
 * {@code toggle_domobevolution}, purely transient and reset on restart). Both are modelled
 * here so the commands keep the original semantics.
 */
public final class RuntimeToggles {
    /** {@code null} means "follow {@code GeneralConfig.parasiteBlockDrops}". */
    private static Boolean blockDropOverride;
    private static boolean mobEvolution = true;

    private RuntimeToggles() {
    }

    /** {@code SRPConfig.doTileDrops}. */
    public static boolean parasiteBlockDrops() {
        return blockDropOverride != null ? blockDropOverride : GeneralConfig.parasiteBlockDrops();
    }

    public static boolean toggleParasiteBlockDrops() {
        blockDropOverride = !parasiteBlockDrops();
        return blockDropOverride;
    }

    /** Drops the runtime override so the value follows the configuration file again. */
    public static void resetParasiteBlockDrops() {
        blockDropOverride = null;
    }

    /** {@code ParasiteEventEntity.canSpawnNext} — gates evolution of every parasite. */
    public static boolean mobEvolution() {
        return mobEvolution;
    }

    public static boolean toggleMobEvolution() {
        mobEvolution = !mobEvolution;
        return mobEvolution;
    }
}
