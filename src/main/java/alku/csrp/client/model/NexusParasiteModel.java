package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.NexusParasiteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public final class NexusParasiteModel extends ParasiteGeoModel<NexusParasiteEntity> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public NexusParasiteModel(String id) {
        model = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "geo/" + id + ".geo.json");
        texture = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/" + id + ".png");
        animation = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "animations/" + id + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(NexusParasiteEntity animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(NexusParasiteEntity animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(NexusParasiteEntity animatable) {
        return animation;
    }

    @Override
    public void setCustomAnimations(NexusParasiteEntity animatable, long instanceId, AnimationState<NexusParasiteEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Apply family-specific animations
        NexusParasiteEntity.Kind kind = animatable.getKind();
        if (kind == null) {
            return;
        }

        switch (kind) {
            case BECKON_SI, BECKON_SII -> applyBeckonLowerStageAnimations(animatable, animationState);
            case BECKON_SIII -> applyBeckonIIIAnimations(animatable, animationState);
            case BECKON_SIV -> applyBeckonIVAnimations(animatable, animationState);
            default -> {}
        }
    }

    /**
     * Apply animations for Beckoner I stage.
     */
    private void applyBeckonLowerStageAnimations(NexusParasiteEntity entity, AnimationState<NexusParasiteEntity> animationState) {
        NexusParasiteEntity.Kind kind = entity.getKind();

        if (kind == NexusParasiteEntity.Kind.BECKON_SI) {
            applyBeckonIAnimations(entity, animationState);
        } else if (kind == NexusParasiteEntity.Kind.BECKON_SII) {
            applyBeckonIIAnimations(entity, animationState);
        }
    }

    /**
     * Apply animations for Beckoner I stage (EntityLeemSI).
     */
    private void applyBeckonIAnimations(NexusParasiteEntity entity, AnimationState<NexusParasiteEntity> animationState) {
        float bodyValue = entity.getBODY();
        float floorTimer = entity.getFloorTimer();

        if (floorTimer >= 0.0F) {
            getBone("mainbody").ifPresent(bone -> {
                bone.setPosY(bone.getPosY() + floorTimer);
            });
        }
    }

    /**
     * Apply animations for Beckoner II stage (EntityLeemSII).
     * Implements procedural animations based on original mod implementation:
     * - Sinusoidal decoration joint rotations (6 main groups: jointDA-jointDF pairs)
     * - Main joint C position oscillation
     * - Tentacle visibility control based on RTTS status
     * - Body positioning based on floor timer (burial system)
     */
    private void applyBeckonIIAnimations(NexusParasiteEntity entity, AnimationState<NexusParasiteEntity> animationState) {
        float ageInTicks = animationState.getAnimatable().tickCount + animationState.getPartialTick();
        float floorTimer = entity.getFloorTimer();
        int parasiteStatus = entity.getParasiteStatus();

        // Core animation wave parameters matching original frequencies
        float f1 = (float) Math.sin(ageInTicks * 0.4857F) * 0.045F;
        float f2 = (float) Math.sin(ageInTicks * 0.41F) * 0.042065F;
        float f3 = (float) Math.sin(ageInTicks * 0.457F) * 0.04265F;

        // Decoration joint A animations
        getBone("jointDA").ifPresent(bone -> {
            bone.setRotY(bone.getRotY() + f2);
            bone.setRotZ(bone.getRotZ() - f1);
        });
        getBone("jointDA_2").ifPresent(bone -> {
            bone.setRotY(bone.getRotY() - f2);
            bone.setRotZ(bone.getRotZ() + f3);
        });

        // Decoration joint B animations
        getBone("jointDB").ifPresent(bone -> {
            bone.setRotY(bone.getRotY() + f3);
            bone.setRotZ(bone.getRotZ() + f1);
        });
        getBone("jointDB_2").ifPresent(bone -> {
            bone.setRotY(bone.getRotY() - f2);
            bone.setRotZ(bone.getRotZ() + f2);
        });

        // Decoration joint C animations
        getBone("jointDC").ifPresent(bone -> {
            bone.setRotY(bone.getRotY() + f3);
            bone.setRotZ(bone.getRotZ() + f1);
        });
        getBone("jointDC_2").ifPresent(bone -> {
            bone.setRotY(bone.getRotY() - f1);
            bone.setRotZ(bone.getRotZ() - f2);
        });

        // Main joint C position oscillation (secondary frequency)
        float jointCOffset = (float) Math.sin(ageInTicks * 0.241F) * 0.0142065F;
        getBone("jointC").ifPresent(bone -> {
            bone.setPosX(bone.getPosX() + jointCOffset);
        });

        // Tentacle visibility control based on RTTS status (parasiteStatus)
        // When RTTS is true (status != 0), tentacles should be hidden
        boolean hideTentacles = parasiteStatus != 0;
        String[] tentacleJoints = {"taclejointA", "taclejointB", "taclejointC", "taclejointD"};
        for (String tentacleName : tentacleJoints) {
            getBone(tentacleName).ifPresent(bone -> bone.setHidden(hideTentacles));
        }

        // Floor timer for burial animation
        if (floorTimer >= 0.0F) {
            getBone("mainbody").ifPresent(bone -> {
                bone.setPosY(bone.getPosY() + floorTimer);
            });
        }
    }

    /**
     * Apply animations for Beckoner III (EntityLeemSIII) based on original mod implementation.
     * Implements complex procedural animations with sinusoidal waves.
     */
    private void applyBeckonIIIAnimations(NexusParasiteEntity entity, AnimationState<NexusParasiteEntity> animationState) {
        float ageInTicks = animationState.getAnimatable().tickCount + animationState.getPartialTick();
        float bodyValue = entity.getBODY();
        float floorTimer = entity.getFloorTimer();
        int parasiteStatus = entity.getParasiteStatus();

        // Tentacle visibility control based on burial status
        boolean buried = parasiteStatus != 0;
        String[] tentacleJoints = {"taclejointA", "taclejointB", "taclejointC", "taclejointD", "taclejointE", "taclejointF"};
        for (String tentacleName : tentacleJoints) {
            getBone(tentacleName).ifPresent(bone -> bone.setHidden(buried));
        }
        getBone("rootA").ifPresent(bone -> bone.setHidden(buried));

        // Decoration joint swaying animations (7 groups: jointDA-DG)
        applyDecorSwayAnimation("jointDA", ageInTicks, 0.17f, 0.02605f, 0.2087f, 0.02345065f, false, false);
        applyDecorSwayAnimation("jointDA_2", ageInTicks, 0.197f, 0.02534065f, 0.17f, 0.02605f, true, false);
        applyDecorSwayAnimation("jointDB", ageInTicks, 0.2087f, 0.02345065f, 0.197f, 0.02534065f, false, false);
        applyDecorSwayAnimation("jointDB_2", ageInTicks, 0.2087f, 0.02345065f, 0.2087f, 0.02345065f, true, false);
        applyDecorSwayAnimation("jointDC", ageInTicks, 0.17f, 0.02605f, 0.2087f, 0.02345065f, false, false);
        applyDecorSwayAnimation("jointDC_2", ageInTicks, 0.197f, 0.02534065f, 0.2087f, 0.02345065f, false, false);
        applyDecorSwayAnimation("jointDD", ageInTicks, 0.17f, 0.02605f, 0.2087f, 0.02345065f, false, false);
        applyDecorSwayAnimation("jointDD_2", ageInTicks, 0.197f, 0.02534065f, 0.17f, 0.02605f, false, false);
        applyDecorSwayAnimation("jointDE", ageInTicks, 0.2087f, 0.02345065f, 0.197f, 0.02534065f, false, false);
        applyDecorSwayAnimation("jointDE_2", ageInTicks, 0.17f, 0.02605f, 0.2087f, 0.02345065f, false, false);
        applyDecorSwayAnimation("jointDF", ageInTicks, 0.197f, 0.02534065f, 0.17f, 0.02605f, false, false);
        applyDecorSwayAnimation("jointDF_2", ageInTicks, 0.2087f, 0.02345065f, 0.197f, 0.02534065f, false, false);
        applyDecorSwayAnimation("jointDG", ageInTicks, 0.2087f, 0.02345065f, 0.17f, 0.02605f, true, true);
        applyDecorSwayAnimation("jointDG_2", ageInTicks, 0.197f, 0.02534065f, 0.2087f, 0.02345065f, true, false);

        // Body segment swaying
        applyBodySegmentSway("jointD", ageInTicks, 0.241f, 0.01542065f, false);
        applyBodySegmentSway("joint", ageInTicks, 0.2841f, 0.01765f, true);

        // Floor timer offset (burial animation)
        if (floorTimer >= 0.0F) {
            getBone("mainbody").ifPresent(bone -> {
                bone.setPosY(bone.getPosY() + floorTimer);
            });
        }
    }

    /**
     * Apply animations for Beckoner IV stage (Venkrol).
     * Implements the complete animation system from original EntityVenkrol.
     */
    private void applyBeckonIVAnimations(NexusParasiteEntity entity, AnimationState<NexusParasiteEntity> animationState) {
        float ageInTicks = animationState.getAnimatable().tickCount + animationState.getPartialTick();
        float bodyValue = entity.getBODY();
        float floorTimer = entity.getFloorTimer();
        int parasiteStatus = entity.getParasiteStatus();

        // Body breathing animation - frequency changes based on status
        // Status 0 (dormant): faster breathing, Status 1+ (active): slower breathing
        float breathFreq1 = parasiteStatus == 0 ? 0.091688F : 0.051688F;
        float breathFreq2 = parasiteStatus == 0 ? 0.053515F : 0.013515F;
        float breathAmpMult = parasiteStatus == 0 ? 0.011F : 0.0011F;

        float breathRotX = 0.3F * (float) Math.sin(ageInTicks * breathFreq1) * breathAmpMult;
        float breathRotZ = -0.6F * (float) Math.sin(ageInTicks * breathFreq2) * breathAmpMult;

        // Apply breathing to body segments (body1-body5)
        String[] bodySegments = {"body1", "body2", "body3", "body4", "body5"};
        for (String segment : bodySegments) {
            getBone(segment).ifPresent(bone -> {
                bone.setRotX(bone.getRotX() + breathRotX);
                bone.setRotZ(bone.getRotZ() + breathRotZ);
            });
        }

        // Tentacle animation - 4 groups: FR (front-right), FL (front-left), BR (back-right), BL (back-left)
        // Each group has 4 joints
        float tentacleWave = parasiteStatus == 0 ?
            0.3F * (float) Math.sin(ageInTicks * 0.1F) * 0.15F :
            0.3F * (float) Math.sin(ageInTicks * 0.1F) * 0.25F;

        // Body value affects tentacle extension (0.0-0.6 range)
        float clampedBody = Math.min(bodyValue, 0.6F);

        // Front-Right tentacle group
        applyTentacleGroupAnimation("tacleFRjoint1", "tacleFRjoint2", "tacleFRjoint3", "tacleFRjoint4",
            clampedBody, tentacleWave, false);

        // Front-Left tentacle group
        applyTentacleGroupAnimation("tacleFLjoint1", "tacleFLjoint2", "tacleFLjoint3", "tacleFLjoint4",
            clampedBody, tentacleWave, true);

        // Back-Right tentacle group
        applyTentacleGroupAnimation("tacleBRjoint1", "tacleBRjoint2", "tacleBRjoint3", "tacleBRjoint4",
            clampedBody, tentacleWave, false);

        // Back-Left tentacle group
        applyTentacleGroupAnimation("tacleBLjoint1", "tacleBLjoint2", "tacleBLjoint3", "tacleBLjoint4",
            clampedBody, tentacleWave, true);

        // Floor timer for burial animation
        if (floorTimer >= 0.0F) {
            getBone("mainbody").ifPresent(bone -> {
                bone.setPosY(bone.getPosY() + floorTimer);
                bone.setRotX(bone.getRotX() + animationState.getPartialTick() * 0.091F);
                bone.setRotZ(bone.getRotZ() + animationState.getPartialTick() * 0.092F);
            });
        }
    }

    /**
     * Apply animation to a tentacle group (4 joints).
     *
     * @param joint1Name First joint (base)
     * @param joint2Name Second joint
     * @param joint3Name Third joint
     * @param joint4Name Fourth joint (tip)
     * @param bodyValue Body expansion value (0.0-0.6)
     * @param wave Tentacle wave motion
     * @param leftSide True if this is a left-side tentacle (inverts rotation)
     */
    private void applyTentacleGroupAnimation(String joint1Name, String joint2Name, String joint3Name,
                                             String joint4Name, float bodyValue, float wave, boolean leftSide) {
        float sign = leftSide ? -1.0F : 1.0F;

        // Joint 1 (base): rotationX affected by body and wave
        getBone(joint1Name).ifPresent(bone -> {
            float rotX = Math.min(bodyValue, 0.6F) + sign * wave;
            bone.setRotX(bone.getRotX() + rotX);
        });

        // Joint 2: rotationY affected by body (clamped to 0.45)
        getBone(joint2Name).ifPresent(bone -> {
            float rotY = sign * Math.min(bodyValue, 0.45F);
            bone.setRotY(bone.getRotY() + rotY);
        });

        // Joint 3: rotationY affected by wave
        getBone(joint3Name).ifPresent(bone -> {
            bone.setRotY(bone.getRotY() + wave);
        });

        // Joint 4: no rotation (kept at 0.0)
        // getBone(joint4Name) intentionally not modified
    }

    /**
     * Apply sinusoidal swaying animation to decoration joints.
     *
     * @param boneName Bone identifier
     * @param time Age in ticks
     * @param freqZ Z-axis frequency
     * @param ampZ Z-axis amplitude
     * @param freqX X-axis frequency
     * @param ampX X-axis amplitude
     * @param invertZ Invert Z rotation
     * @param invertX Invert X rotation
     */
    private void applyDecorSwayAnimation(String boneName, float time, float freqZ, float ampZ,
                                         float freqX, float ampX, boolean invertZ, boolean invertX) {
        getBone(boneName).ifPresent(bone -> {
            float rotZ = (float) Math.sin(time * freqZ) * ampZ;
            float rotX = (float) Math.sin(time * freqX) * ampX;

            bone.setRotZ(bone.getRotZ() + (invertZ ? -rotZ : rotZ));
            bone.setRotX(bone.getRotX() + (invertX ? rotX : -rotX));
        });
    }

    /**
     * Apply Y-axis swaying to body segments.
     *
     * @param boneName Bone identifier
     * @param time Age in ticks
     * @param frequency Wave frequency
     * @param amplitude Wave amplitude
     * @param invert Invert rotation direction
     */
    private void applyBodySegmentSway(String boneName, float time, float frequency, float amplitude, boolean invert) {
        getBone(boneName).ifPresent(bone -> {
            float rotY = (float) Math.sin(time * frequency) * amplitude;
            bone.setRotY(bone.getRotY() + (invert ? -rotY : rotY));
        });
    }
}
