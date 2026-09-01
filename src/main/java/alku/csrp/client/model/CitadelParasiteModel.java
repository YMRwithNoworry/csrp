package alku.csrp.client.model;

import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.animation.CitadelAnimationCache;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;
import java.util.List;
import java.util.Optional;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Generic Citadel model executing animation formulae from a Tabula export resource. */
public class CitadelParasiteModel<T extends Mob & CitadelAnimatedEntity> extends LegacyTabulaModel<T>
        implements CitadelTextureProvider<T> {
    private static final float MOVING_ROTATION_SCALE = 0.72F;

    private final String modelId;
    private final ResourceLocation texture;
    private final LegacyAnimationLibrary animations;

    public CitadelParasiteModel(String modelId) {
        this(modelId, modelId);
    }

    public CitadelParasiteModel(String modelId, String animationId) {
        super(modelId);
        this.modelId = modelId;
        texture = ResourceLocation.fromNamespaceAndPath("csrp", "textures/entity/" + modelId + ".png");
        animations = new LegacyAnimationLibrary(animationId);
    }

    public String modelId() {
        return modelId;
    }

    public ResourceLocation texture(T entity) {
        return texture;
    }

    protected final Optional<AdvancedModelBox> getBone(String name) {
        return Optional.ofNullable(findPart(name));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void animateLegacy(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        boolean moving = limbSwingAmount > 0.01F && actualMovement(entity);
        CitadelAnimationCache cache = entity.getCitadelAnimationCache();
        List<CitadelAnimationController<?>> controllers = cache.controllers(entity);
        for (CitadelAnimationController rawController : controllers) {
            CitadelAnimationController<Object> controller = rawController;
            CitadelAnimationCache.TriggerState trigger = cache.trigger(controller.name());
            CitadelRawAnimation selected = null;
            float animationTime = 0.0F;
            if (trigger != null) {
                selected = controller.triggeredAnimation(trigger.animation());
                if (selected != null) {
                    animationTime = (ageInTicks - trigger.startTick()) / 20.0F;
                    float duration = animations.duration(selected.name());
                    if (!selected.loop() && duration > 0.0F && animationTime > duration) {
                        cache.clearTrigger(controller.name(), trigger);
                        selected = null;
                    }
                } else {
                    cache.clearTrigger(controller.name(), trigger);
                }
            }
            if (selected == null) {
                CitadelAnimationState<Object> state = new CitadelAnimationState<>(entity, moving,
                        ageInTicks - entity.tickCount);
                CitadelPlayState result = controller.evaluate(state);
                selected = state.selectedAnimation();
                if (result != CitadelPlayState.CONTINUE || selected == null) {
                    continue;
                }
                animationTime = selected.loop()
                        ? ageInTicks / 20.0F
                        : (ageInTicks - cache.selectionStart(controller.name(), selected.name(),
                                entity.tickCount)) / 20.0F;
            }
            if (animations.contains(selected.name())) {
                animations.apply(this, selected.name(), Math.max(0.0F, animationTime),
                        moving ? MOVING_ROTATION_SCALE : 1.0F);
            }
        }
        customize(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    protected void customize(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
    }

    private static boolean actualMovement(Mob entity) {
        double deltaX = entity.getX() - entity.xo;
        double deltaY = entity.getY() - entity.yo;
        double deltaZ = entity.getZ() - entity.zo;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 1.0E-6D;
    }
}
