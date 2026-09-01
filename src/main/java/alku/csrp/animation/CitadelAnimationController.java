package alku.csrp.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** One independently evaluated animation layer, equivalent to an original model pass. */
public final class CitadelAnimationController<T> {
    private final String name;
    private final int transitionTicks;
    private final Function<CitadelAnimationState<T>, CitadelPlayState> predicate;
    private final Map<String, CitadelRawAnimation> triggerableAnimations = new LinkedHashMap<>();

    public CitadelAnimationController(T owner, String name, int transitionTicks,
            Function<CitadelAnimationState<T>, CitadelPlayState> predicate) {
        this.name = name;
        this.transitionTicks = Math.max(0, transitionTicks);
        this.predicate = predicate;
    }

    public CitadelAnimationController<T> triggerableAnim(String trigger, CitadelRawAnimation animation) {
        triggerableAnimations.put(trigger, animation);
        return this;
    }

    public String name() {
        return name;
    }

    public int transitionTicks() {
        return transitionTicks;
    }

    public CitadelPlayState evaluate(CitadelAnimationState<T> state) {
        return predicate.apply(state);
    }

    public CitadelRawAnimation triggeredAnimation(String trigger) {
        return triggerableAnimations.get(trigger);
    }
}
