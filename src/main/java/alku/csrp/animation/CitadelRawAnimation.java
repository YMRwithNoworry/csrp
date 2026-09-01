package alku.csrp.animation;

import java.util.Objects;

/** A selected legacy Tabula animation clip evaluated by the Citadel runtime. */
public final class CitadelRawAnimation {
    private final String name;
    private final boolean loop;

    private CitadelRawAnimation(String name, boolean loop) {
        this.name = name;
        this.loop = loop;
    }

    public static CitadelRawAnimation begin() {
        return new CitadelRawAnimation("", true);
    }

    public CitadelRawAnimation thenLoop(String animationName) {
        return new CitadelRawAnimation(Objects.requireNonNull(animationName), true);
    }

    public CitadelRawAnimation thenPlay(String animationName) {
        return new CitadelRawAnimation(Objects.requireNonNull(animationName), false);
    }

    public String name() {
        return name;
    }

    public boolean loop() {
        return loop;
    }
}
