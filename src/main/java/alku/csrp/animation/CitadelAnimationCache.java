package alku.csrp.animation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Per-entity controller and trigger state. */
public final class CitadelAnimationCache {
    private List<CitadelAnimationController<?>> controllers;
    private final Map<String, TriggerState> triggers = new HashMap<>();
    private final Map<String, SelectionState> selections = new HashMap<>();

    public synchronized List<CitadelAnimationController<?>> controllers(CitadelAnimatedEntity entity) {
        if (controllers == null) {
            CitadelAnimationManager.ControllerRegistrar registrar =
                    new CitadelAnimationManager.ControllerRegistrar();
            entity.registerControllers(registrar);
            controllers = registrar.controllers();
        }
        return controllers;
    }

    public synchronized void trigger(String controller, String animation, int tick) {
        triggers.put(controller, new TriggerState(animation, tick));
    }

    public synchronized TriggerState trigger(String controller) {
        return triggers.get(controller);
    }

    public synchronized void clearTrigger(String controller, TriggerState expected) {
        triggers.remove(controller, expected);
    }

    public synchronized int selectionStart(String controller, String animation, int tick) {
        SelectionState previous = selections.get(controller);
        if (previous == null || !previous.animation().equals(animation)) {
            previous = new SelectionState(animation, tick);
            selections.put(controller, previous);
        }
        return previous.startTick();
    }

    public record TriggerState(String animation, int startTick) {
    }

    private record SelectionState(String animation, int startTick) {
    }
}
