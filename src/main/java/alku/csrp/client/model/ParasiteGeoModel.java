package alku.csrp.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Shared rendering behavior for imported SRP Geo models. */
public abstract class ParasiteGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final float MOVING_ROTATION_SCALE = 0.72F;

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (!animationState.isMoving()) {
            return;
        }

        // Extracted locomotion clips contain full-strength limb rotation. Keep the model's
        // baked pose intact while reducing only the animated rotation delta.
        for (var bone : getAnimationProcessor().getRegisteredBones()) {
            if (!shouldDampenMovingRotation(animatable, bone)) {
                continue;
            }
            BoneSnapshot initialSnapshot = bone.getInitialSnapshot();
            if (initialSnapshot == null) {
                continue;
            }

            bone.setRotX(dampenRotation(initialSnapshot.getRotX(), bone.getRotX()));
            bone.setRotY(dampenRotation(initialSnapshot.getRotY(), bone.getRotY()));
            bone.setRotZ(dampenRotation(initialSnapshot.getRotZ(), bone.getRotZ()));
        }
    }

    protected boolean shouldDampenMovingRotation(T animatable, CoreGeoBone bone) {
        return true;
    }

    private static float dampenRotation(float initialRotation, float animatedRotation) {
        return initialRotation + (animatedRotation - initialRotation) * MOVING_ROTATION_SCALE;
    }
}
