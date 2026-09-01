package alku.csrp.animation;

import java.util.ArrayList;
import java.util.List;

/** Registration container used by entities to declare their Citadel animation layers. */
public final class CitadelAnimationManager {
    private CitadelAnimationManager() {
    }

    public static final class ControllerRegistrar {
        private final List<CitadelAnimationController<?>> controllers = new ArrayList<>();

        public void add(CitadelAnimationController<?> controller) {
            controllers.add(controller);
        }

        List<CitadelAnimationController<?>> controllers() {
            return List.copyOf(controllers);
        }
    }
}
