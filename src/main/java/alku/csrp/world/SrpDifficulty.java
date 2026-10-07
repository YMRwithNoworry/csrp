package alku.csrp.world;

import java.util.Locale;

/** Per-world SRP balance profile introduced by SRP 1.10. */
public enum SrpDifficulty {
    // Original SRPEventHandlerBus#setNewParasiteTask: choice 2 multiplies health/armor/damage/knockback
    // by 4 (changeAttribute bonus 3.0) and choice 3 by 11 (bonus 10.0); choice 0/1 leave them at 1.
    // SRPSaveData#setTotalKills scales positive point gains by 0.5 / 1 / 3 / 10.
    EASY(1.0D, 1.0D, 1.0D, 1.0D, 0.5D),
    NORMAL(1.0D, 1.0D, 1.0D, 1.0D, 1.0D),
    HARD(4.0D, 4.0D, 4.0D, 4.0D, 3.0D),
    IMPOSSIBLE(11.0D, 11.0D, 11.0D, 11.0D, 10.0D);

    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double armorMultiplier;
    private final double knockbackMultiplier;
    private final double pointMultiplier;

    SrpDifficulty(double healthMultiplier, double damageMultiplier, double armorMultiplier,
            double knockbackMultiplier, double pointMultiplier) {
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.armorMultiplier = armorMultiplier;
        this.knockbackMultiplier = knockbackMultiplier;
        this.pointMultiplier = pointMultiplier;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "options.csrp.difficulty." + id();
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    public double healthMultiplier() {
        return healthMultiplier;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double armorMultiplier() {
        return armorMultiplier;
    }

    public double knockbackMultiplier() {
        return knockbackMultiplier;
    }

    public double pointMultiplier() {
        return pointMultiplier;
    }

    public static SrpDifficulty byId(String id) {
        for (SrpDifficulty difficulty : values()) {
            if (difficulty.id().equals(id)) {
                return difficulty;
            }
        }
        return NORMAL;
    }
}
