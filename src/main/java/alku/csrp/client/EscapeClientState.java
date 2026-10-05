package alku.csrp.client;

/**
 * Port of SRP 1.10.9 {@code client/EscapeClientState}: the client-side flag that says whether the death
 * screen should offer the escape system.  Set from {@code EscapeOfferPayload} and read by
 * {@link EscapeDeathScreenEvents}.
 */
public final class EscapeClientState {
    private static boolean offer;

    private EscapeClientState() {
    }

    public static void setOffer(boolean value) {
        offer = value;
    }

    public static boolean offer() {
        return offer;
    }
}
