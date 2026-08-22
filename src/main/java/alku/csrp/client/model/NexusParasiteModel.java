package alku.csrp.client.model;

import alku.csrp.Csrp;
import alku.csrp.entity.NexusParasiteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public final class NexusParasiteModel extends ParasiteGeoModel<NexusParasiteEntity> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public NexusParasiteModel(String id) {
        model = new ResourceLocation(Csrp.MODID, "geo/" + id + ".geo.json");
        texture = new ResourceLocation(Csrp.MODID, "textures/entity/" + id + ".png");
        animation = new ResourceLocation(Csrp.MODID, "animations/" + id + ".animation.json");
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
            case DISPATCHER_SII -> applyDispatcherIIAnimations(animatable, animationState);
            case DISPATCHER_SIV -> applyDispatcherIVAnimations(animatable, animationState);
            default -> {}
        }
        if (kind == NexusParasiteEntity.Kind.DISPATCHER_SI
                || kind == NexusParasiteEntity.Kind.DISPATCHER_SII
                || kind == NexusParasiteEntity.Kind.DISPATCHER_SIII
                || kind == NexusParasiteEntity.Kind.DISPATCHER_SIV) {
            // Dispatcher head is a standalone `h` bone in the exported models.
            getBone("h").ifPresent(bone -> bone.setHidden(false));
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

    /**
     * Apply animations for Dispatcher II (EntityDodSII) based on original mod implementation.
     * Implements complex procedural tentacle, neck, muscle, and hair animations with sinusoidal waves.
     */
    private void applyDispatcherIIAnimations(NexusParasiteEntity entity, AnimationState<NexusParasiteEntity> animationState) {
        float ageInTicks = animationState.getAnimatable().tickCount + animationState.getPartialTick();

        // Tentacle swaying animations - 9 groups (taclejointA through taclejointI)
        // Each group has a main joint and 2 child joints
        float tentacleF1 = (float) Math.sin(ageInTicks * 0.095986F) * 0.1429872F;
        float tentacleF2 = (float) Math.sin(ageInTicks * 0.0758786F) * 0.20219871F;
        float tentacleF3 = (float) Math.sin(ageInTicks * 0.0986F) * 0.1758872F;

        // Apply tentacle animations to 9 groups
        applyTentacleJointAnimation("taclejointA", tentacleF2, -tentacleF3);
        applyTentacleJointAnimation("taclejointB", tentacleF3, tentacleF1);
        applyTentacleJointAnimation("taclejointC", tentacleF1, -tentacleF2);
        applyTentacleJointAnimation("taclejointD", -tentacleF2, tentacleF3);
        applyTentacleJointAnimation("taclejointE", tentacleF3, -tentacleF1);
        applyTentacleJointAnimation("taclejointF", -tentacleF1, tentacleF2);
        applyTentacleJointAnimation("taclejointG", tentacleF2, tentacleF3);
        applyTentacleJointAnimation("taclejointH", -tentacleF3, tentacleF1);
        applyTentacleJointAnimation("taclejointI", tentacleF1, tentacleF3);

        // Neck/joint swaying animations
        float neckF1 = (float) Math.sin(ageInTicks * 0.0786F) * 0.0929872F;
        float neckF2 = (float) Math.sin(ageInTicks * 0.06786F) * 0.099872F;
        float neckF3 = (float) Math.sin(ageInTicks * 0.08986F) * 0.09158872F;

        // Apply neck joint animations - jointNA series
        getBone("jointNA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF1);
            bone.setRotZ(bone.getRotZ() + neckF2);
        });
        getBone("jointNAA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF2);
            bone.setRotZ(bone.getRotZ() + neckF3);
        });
        getBone("jointNAB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF3);
            bone.setRotZ(bone.getRotZ() + neckF1);
        });

        // Apply neck joint animations - jointNB series
        getBone("jointNB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF3);
            bone.setRotZ(bone.getRotZ() + neckF1);
        });
        getBone("jointNBA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF1);
            bone.setRotZ(bone.getRotZ() + neckF2);
        });
        getBone("jointNBB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF2);
            bone.setRotZ(bone.getRotZ() + neckF3);
        });

        // Muscle joint swaying animations - NA muscle series
        float muscleF2A = (float) Math.sin(ageInTicks * 0.04936786F) * 0.4172F;

        getBone("jointNAMA").ifPresent(bone -> {
            bone.setRotZ(bone.getRotZ() - muscleF2A);
        });
        getBone("jointNAMB").ifPresent(bone -> {
            bone.setRotZ(bone.getRotZ() + muscleF2A);
        });
        getBone("jointNAMC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + muscleF2A);
        });
        getBone("jointNAMD").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - muscleF2A);
        });

        // Muscle joint swaying animations - NB muscle series (reversed)
        float muscleF2B = -(float) Math.sin(ageInTicks * 0.0436786F) * 0.472F;

        getBone("jointNBMA").ifPresent(bone -> {
            bone.setRotZ(bone.getRotZ() - muscleF2B);
        });
        getBone("jointNBMB").ifPresent(bone -> {
            bone.setRotZ(bone.getRotZ() + muscleF2B);
        });
        getBone("jointNBMC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + muscleF2B);
        });
        getBone("jointNBMD").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - muscleF2B);
        });

        // Hair swaying animations
        float hairF1 = (float) Math.sin(ageInTicks * 0.5786F) * 0.0929872F;
        float hairF2 = (float) Math.sin(ageInTicks * 0.46786F) * 0.099872F;
        float hairF3 = (float) Math.sin(ageInTicks * 0.68986F) * 0.09158872F;

        // Hair joint animations - NA hair series
        getBone("hair_jointNAA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF1);
            bone.setRotZ(bone.getRotZ() + hairF2);
        });
        getBone("hair_jointNAB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF2);
            bone.setRotZ(bone.getRotZ() + hairF3);
        });
        getBone("hair_jointNAC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF3);
            bone.setRotZ(bone.getRotZ() + hairF1);
        });

        // Hair joint animations - NB hair series (reversed)
        getBone("hair_jointNBA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF1);
            bone.setRotZ(bone.getRotZ() - hairF2);
        });
        getBone("hair_jointNBB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF2);
            bone.setRotZ(bone.getRotZ() - hairF3);
        });
        getBone("hair_jointNBC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3);
            bone.setRotZ(bone.getRotZ() - hairF1);
        });
    }

    /**
     * Apply tentacle joint animation (for Dispatcher II tentacles).
     * Each tentacle group has a main joint and 2 child joints.
     *
     * @param jointName Main joint name (e.g., "taclejointA")
     * @param rotX Rotation X value
     * @param rotY Rotation Y value
     */
    private void applyTentacleJointAnimation(String jointName, float rotX, float rotY) {
        getBone(jointName).ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + rotX);
            bone.setRotY(bone.getRotY() + rotY);
        });
    }

    /**
     * Apply animations for Dispatcher IV (EntityDodSIV) based on original mod implementation.
     * Implements complex procedural animations with:
     * - 10 tentacle groups (taclejointA-J), each with 6 segments
     * - 3 neck/head groups (jointNA, NB, NC)
     * - Appendage animations (jointNAMA/B/C/D, jointNBMA/B/C/D, jointNCMA/B/C/D)
     * - Hair/tendril animations (hair_jointNAA/B/C, NBA/B/C, NCA/B/C)
     */
    private void applyDispatcherIVAnimations(NexusParasiteEntity entity, AnimationState<NexusParasiteEntity> animationState) {
        float ageInTicks = animationState.getAnimatable().tickCount + animationState.getPartialTick();

        // Tentacle animations - 10 groups with different frequency parameters
        float tentacleF1 = (float) Math.sin(ageInTicks * 0.11095986F) * 0.16429871F;
        float tentacleF2 = (float) Math.sin(ageInTicks * 0.09758786F) * 0.22021987F;
        float tentacleF3 = (float) Math.sin(ageInTicks * 0.110986F) * 0.19758873F;

        // Apply animations to 10 tentacle groups (A-J)
        // Each uses different combinations of the three wave functions
        applyDispatcherIVTentacle("taclejointA", tentacleF2, -tentacleF3);
        applyDispatcherIVTentacle("taclejointB", -tentacleF3, tentacleF1);
        applyDispatcherIVTentacle("taclejointC", tentacleF1, tentacleF2);
        applyDispatcherIVTentacle("taclejointD", -tentacleF2, tentacleF3);
        applyDispatcherIVTentacle("taclejointE", tentacleF1, tentacleF2);
        applyDispatcherIVTentacle("taclejointF", -tentacleF3, -tentacleF1);
        applyDispatcherIVTentacle("taclejointG", tentacleF2, -tentacleF1);
        applyDispatcherIVTentacle("taclejointH", -tentacleF1, tentacleF2);
        applyDispatcherIVTentacle("taclejointI", tentacleF1, -tentacleF2);
        applyDispatcherIVTentacle("taclejointJ", -tentacleF2, -tentacleF1);

        // Neck/head animations - 3 main neck groups
        float neckF1 = (float) Math.sin(ageInTicks * 0.0786F) * 0.0929872F;
        float neckF2 = (float) Math.sin(ageInTicks * 0.06786F) * 0.099872F;
        float neckF3 = (float) Math.sin(ageInTicks * 0.08986F) * 0.09158872F;

        // jointNA group
        getBone("jointNA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF1);
            bone.setRotZ(bone.getRotZ() - neckF3);
        });
        getBone("jointNA_1").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - neckF1);
            bone.setRotZ(bone.getRotZ() + neckF2);
        });
        getBone("jointNA_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - neckF3);
            bone.setRotZ(bone.getRotZ() + neckF2);
        });

        // jointNB group
        getBone("jointNB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - neckF3);
            bone.setRotZ(bone.getRotZ() + neckF2);
        });
        getBone("jointNB_1").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF3);
            bone.setRotZ(bone.getRotZ() - neckF1);
        });

        // jointNC group
        getBone("jointNC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - neckF3);
            bone.setRotZ(bone.getRotZ() + neckF2);
        });
        getBone("jointNC_1").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + neckF3);
            bone.setRotZ(bone.getRotZ() - neckF1);
        });

        // Appendage animations - jointNAMA/B/C/D series
        float appendageF2A = (float) Math.sin(ageInTicks * 0.04936786F) * 0.4172F;
        getBone("jointNAMA").ifPresent(bone -> bone.setRotZ(bone.getRotZ() - appendageF2A));
        getBone("jointNAMB").ifPresent(bone -> bone.setRotZ(bone.getRotZ() + appendageF2A));
        getBone("jointNAMC").ifPresent(bone -> bone.setRotX(bone.getRotX() + appendageF2A));
        getBone("jointNAMD").ifPresent(bone -> bone.setRotX(bone.getRotX() - appendageF2A));

        // jointNBMA/B/C/D series (reversed phase)
        float appendageF2B = -(float) Math.sin(ageInTicks * 0.0436786F) * 0.472F;
        getBone("jointNBMA").ifPresent(bone -> bone.setRotZ(bone.getRotZ() - appendageF2B));
        getBone("jointNBMB").ifPresent(bone -> bone.setRotZ(bone.getRotZ() + appendageF2B));
        getBone("jointNBMC").ifPresent(bone -> bone.setRotX(bone.getRotX() + appendageF2B));
        getBone("jointNBMD").ifPresent(bone -> bone.setRotX(bone.getRotX() - appendageF2B));

        // jointNCMA/B/C/D series (different frequency)
        float appendageF2C = (float) Math.sin(ageInTicks * 0.053936787F) * 0.3172F;
        getBone("jointNCMA").ifPresent(bone -> bone.setRotZ(bone.getRotZ() - appendageF2C));
        getBone("jointNCMB").ifPresent(bone -> bone.setRotZ(bone.getRotZ() + appendageF2C));
        getBone("jointNCMC").ifPresent(bone -> bone.setRotX(bone.getRotX() + appendageF2C));
        getBone("jointNCMD").ifPresent(bone -> bone.setRotX(bone.getRotX() - appendageF2C));

        // Hair/tendril animations - fast oscillating frequencies
        float hairF1NA = (float) Math.sin(ageInTicks * 0.5786F) * 0.0929872F;
        float hairF2NA = (float) Math.sin(ageInTicks * 0.46786F) * 0.099872F;
        float hairF3NA = (float) Math.sin(ageInTicks * 0.68986F) * 0.09158872F;

        // hair_jointNA series
        getBone("hair_jointNAA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF1NA);
            bone.setRotY(bone.getRotY() + hairF2NA);
        });
        getBone("hair_jointNAB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF2NA);
            bone.setRotY(bone.getRotY() - hairF1NA);
        });
        getBone("hair_jointNAC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3NA);
            bone.setRotY(bone.getRotY() + hairF1NA);
        });
        getBone("hair_jointNAA_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3NA);
            bone.setRotY(bone.getRotY() + hairF1NA);
        });
        getBone("hair_jointNAB_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF3NA);
            bone.setRotY(bone.getRotY() + hairF1NA);
        });
        getBone("hair_jointNAC_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF2NA);
            bone.setRotY(bone.getRotY() + hairF3NA);
        });

        // hair_jointNB series (reversed phase)
        getBone("hair_jointNBA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF1NA);
            bone.setRotY(bone.getRotY() - hairF2NA);
        });
        getBone("hair_jointNBB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF2NA);
            bone.setRotY(bone.getRotY() - hairF1NA);
        });
        getBone("hair_jointNBC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3NA);
            bone.setRotY(bone.getRotY() - hairF1NA);
        });
        getBone("hair_jointNBA_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF3NA);
            bone.setRotY(bone.getRotY() + hairF1NA);
        });
        getBone("hair_jointNBB_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3NA);
            bone.setRotY(bone.getRotY() + hairF1NA);
        });
        getBone("hair_jointNBC_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF2NA);
            bone.setRotY(bone.getRotY() + hairF3NA);
        });

        // hair_jointNC series (different frequencies)
        float hairF1NC = (float) Math.sin(ageInTicks * 0.4786F) * 0.09929872F;
        float hairF2NC = (float) Math.sin(ageInTicks * 0.66786F) * 0.09129872F;
        float hairF3NC = (float) Math.sin(ageInTicks * 0.58986F) * 0.09215887F;

        getBone("hair_jointNCA").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF1NC);
            bone.setRotY(bone.getRotY() + hairF2NC);
        });
        getBone("hair_jointNCB").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + hairF2NC);
            bone.setRotY(bone.getRotY() - hairF1NC);
        });
        getBone("hair_jointNCC").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3NC);
            bone.setRotY(bone.getRotY() + hairF1NC);
        });
        getBone("hair_jointNCA_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3NC);
            bone.setRotY(bone.getRotY() + hairF1NC);
        });
        getBone("hair_jointNCB_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF3NC);
            bone.setRotY(bone.getRotY() + hairF1NC);
        });
        getBone("hair_jointNCC_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() - hairF2NC);
            bone.setRotY(bone.getRotY() + hairF3NC);
        });
    }

    /**
     * Apply animation to a Dispatcher IV tentacle joint.
     * Each tentacle has 6 segments (_2, _3, _4 child joints).
     *
     * @param jointName Main joint name (e.g., "taclejointA")
     * @param rotX Rotation X value
     * @param rotY Rotation Y value
     */
    private void applyDispatcherIVTentacle(String jointName, float rotX, float rotY) {
        getBone(jointName).ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + rotX);
            bone.setRotY(bone.getRotY() + rotY);
        });
        // Apply to child segments with progressive damping
        getBone(jointName + "_2").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + rotX * 0.9F);
            bone.setRotY(bone.getRotY() + rotY * 0.9F);
        });
        getBone(jointName + "_3").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + rotX * 0.8F);
            bone.setRotY(bone.getRotY() + rotY * 0.8F);
        });
        getBone(jointName + "_4").ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + rotX * 0.7F);
            bone.setRotY(bone.getRotY() + rotY * 0.7F);
        });
    }
}
