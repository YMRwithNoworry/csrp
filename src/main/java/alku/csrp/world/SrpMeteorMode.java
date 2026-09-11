package alku.csrp.world;

/**
 * Per-world "meteor infection" toggle chosen on the create-world screen.
 *
 * <p>Port of the Meteor On/Off cycle button from SRParasites 1.10.8
 * {@code GuiSRPWorldSettings}, which stored the choice in
 * {@code SRPConfigWorld.meteorActive}. The modern port keeps the selection on
 * the world itself so each save can opt in or out independently.</p>
 */
public enum SrpMeteorMode {
    ON("on"),
    OFF("off");

    private final String id;

    SrpMeteorMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Whether the periodic meteor infection event may run for this world. */
    public boolean enabled() {
        return this == ON;
    }

    public String translationKey() {
        return "options.csrp.meteor." + id;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    public static SrpMeteorMode of(boolean enabled) {
        return enabled ? ON : OFF;
    }

    /** @return the matching mode, or {@code null} when the id is unknown. */
    public static SrpMeteorMode byId(String id) {
        for (SrpMeteorMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
