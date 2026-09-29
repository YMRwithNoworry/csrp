package alku.csrp.entity;

import alku.csrp.Config;

/**
 * The original SRP parasite tiers. Each tier owns the three per-tier options the generation system
 * scales: the damage cap divisor ("Version &lt;tier&gt; Cap"), the armor-bypassing minimum damage
 * ("Version &lt;tier&gt; Minimum Damage") and the sight-check flag ("&lt;tier&gt; Walls").
 */
public enum ParasiteTier {
    INFECTED("infected"),
    ASSIMARA("assimara"),
    FERAL("feral"),
    HIJACKED("hijacked"),
    PRIMITIVE("primitive"),
    ADAPTED("adapted"),
    ANCIENT("ancient"),
    PURE("pure"),
    PREEMINENT("preeminent"),
    DERIVED("derived");

    private final String key;

    ParasiteTier(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** Original {@code damageCap}: maximum damage is {@code maxHealth / cap}. */
    public int damageCap() {
        return Config.tierDamageCap(key);
    }

    /** Original {@code MiniDamage}: extra armor-bypassing damage added to every melee hit. */
    public float minimumDamage() {
        return Config.tierMinimumDamage(key);
    }

    /**
     * Original {@code shouldCheckSight} from {@code EntityAINearestAttackableTargetStatus}: when true
     * the tier always needs line of sight, so the generation X-ray gene never applies to it.
     */
    public boolean forcesSightCheck() {
        return Config.tierWalls(key);
    }
}
