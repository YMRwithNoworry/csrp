package alku.csrp.client.model;

import alku.csrp.client.model.tabula.LegacyModelBox;
import alku.csrp.entity.KirinEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Original 1.10.8 {@code ModelKirin} animation running on the self-contained Tabula parts. */
public final class KirinCitadelModel extends LegacyTabulaModel<LegacyMobRenderState> {
    private final LegacyModelBox mainbody;
    private final LegacyModelBox jointURAX;
    private final LegacyModelBox jointURAY;
    private final LegacyModelBox jointURA1;
    private final LegacyModelBox jointURA2;
    private final LegacyModelBox jointURA3;
    private final LegacyModelBox jointURA4;
    private final LegacyModelBox jointULAX;
    private final LegacyModelBox jointULAY;
    private final LegacyModelBox jointULA1;
    private final LegacyModelBox jointULA2;
    private final LegacyModelBox jointULA3;
    private final LegacyModelBox jointULA4;
    private final LegacyModelBox jointMRAX;
    private final LegacyModelBox jointMRAY;
    private final LegacyModelBox jointMRA1;
    private final LegacyModelBox jointMRA2;
    private final LegacyModelBox jointMRA3;
    private final LegacyModelBox jointMRA4;
    private final LegacyModelBox jointMLAX;
    private final LegacyModelBox jointMLAY;
    private final LegacyModelBox jointMLA1;
    private final LegacyModelBox jointMLA2;
    private final LegacyModelBox jointMLA3;
    private final LegacyModelBox jointMLA4;
    private final LegacyModelBox jointDRAX;
    private final LegacyModelBox jointDRAY;
    private final LegacyModelBox jointDRA1;
    private final LegacyModelBox jointDRA2;
    private final LegacyModelBox jointDRA3;
    private final LegacyModelBox jointDRA4;
    private final LegacyModelBox jointDLAX;
    private final LegacyModelBox jointDLAY;
    private final LegacyModelBox jointDLA1;
    private final LegacyModelBox jointDLA2;
    private final LegacyModelBox jointDLA3;
    private final LegacyModelBox jointDLA4;
    private final LegacyModelBox jointH;
    private final LegacyModelBox jointLM;
    private final LegacyModelBox jointRM;

    public KirinCitadelModel() {
        super("kirin");
        mainbody = part("mainbody");
        jointURAX = part("jointURAX");
        jointURAY = part("jointURAY");
        jointURA1 = part("jointURA1");
        jointURA2 = part("jointURA2");
        jointURA3 = part("jointURA3");
        jointURA4 = part("jointURA4");
        jointULAX = part("jointULAX");
        jointULAY = part("jointULAY");
        jointULA1 = part("jointULA1");
        jointULA2 = part("jointULA2");
        jointULA3 = part("jointULA3");
        jointULA4 = part("jointULA4");
        jointMRAX = part("jointMRAX");
        jointMRAY = part("jointMRAY");
        jointMRA1 = part("jointMRA1");
        jointMRA2 = part("jointMRA2");
        jointMRA3 = part("jointMRA3");
        jointMRA4 = part("jointMRA4");
        jointMLAX = part("jointMLAX");
        jointMLAY = part("jointMLAY");
        jointMLA1 = part("jointMLA1");
        jointMLA2 = part("jointMLA2");
        jointMLA3 = part("jointMLA3");
        jointMLA4 = part("jointMLA4");
        jointDRAX = part("jointDRAX");
        jointDRAY = part("jointDRAY");
        jointDRA1 = part("jointDRA1");
        jointDRA2 = part("jointDRA2");
        jointDRA3 = part("jointDRA3");
        jointDRA4 = part("jointDRA4");
        jointDLAX = part("jointDLAX");
        jointDLAY = part("jointDLAY");
        jointDLA1 = part("jointDLA1");
        jointDLA2 = part("jointDLA2");
        jointDLA3 = part("jointDLA3");
        jointDLA4 = part("jointDLA4");
        jointH = part("jointH");
        jointLM = part("jointLM");
        jointRM = part("jointRM");
    }

    @Override
    protected void animateLegacy(LivingEntity legacyEntity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        KirinEntity entity = (KirinEntity) legacyEntity;
        float f11 = Mth.cos(ageInTicks * 0.130998F) * 0.107215F;
        float f22 = Mth.cos(ageInTicks * 0.0819112F) * 0.1206261F;
        float f33 = Mth.cos(ageInTicks * 0.0627955F) * 0.09067262F;

        jointURAX.rotateAngleX = -f11;
        jointURAY.rotateAngleY = f22;
        jointURA1.rotateAngleY = -f33;
        jointURA2.rotateAngleZ = -f11;
        jointURA3.rotateAngleY = f22;
        jointURA4.rotateAngleZ = -f22;
        jointULAX.rotateAngleX = f11;
        jointULAY.rotateAngleY = f33;
        jointULA1.rotateAngleY = -f11;
        jointULA2.rotateAngleZ = f11;
        jointULA3.rotateAngleY = -f22;
        jointULA4.rotateAngleZ = f33;
        jointMRAX.rotateAngleX = f11;
        jointMRAY.rotateAngleY = f33;
        jointMRA1.rotateAngleY = -f22;
        jointMRA2.rotateAngleZ = f22;
        jointMRA3.rotateAngleY = -f11;
        jointMRA4.rotateAngleZ = -f22;
        jointMLAX.rotateAngleX = -f33;
        jointMLAY.rotateAngleY = f33;
        jointMLA1.rotateAngleY = -f22;
        jointMLA2.rotateAngleZ = -f11;
        jointMLA3.rotateAngleY = f22;
        jointMLA4.rotateAngleZ = -f11;
        jointDRAX.rotateAngleX = f11;
        jointDRAY.rotateAngleY = -f22;
        jointDRA1.rotateAngleY = -f22;
        jointDRA2.rotateAngleZ = -f22;
        jointDRA3.rotateAngleY = f22;
        jointDRA4.rotateAngleZ = f22;
        jointDLAX.rotateAngleX = -f11;
        jointDLAY.rotateAngleY = -f22;
        jointDLA1.rotateAngleY = f22;
        jointDLA2.rotateAngleZ = f22;
        jointDLA3.rotateAngleY = -f22;
        jointDLA4.rotateAngleZ = f22;

        f11 = Mth.cos(ageInTicks * 0.1730998F) * 0.1307215F;
        f22 = Mth.cos(ageInTicks * 0.09819112F) * 0.1720626F;
        jointH.rotateAngleX = f22;
        jointLM.rotateAngleY = f11;
        jointRM.rotateAngleY = -f11;

        if (entity.isShadowHitFlashing() || entity.isLaserCharging() || entity.isChargingJudgementCut()) {
            mainbody.rotationPointX += Mth.cos(ageInTicks * 2.95F) * 0.08912576F;
            mainbody.rotationPointZ += Mth.cos(ageInTicks * 2.95F) * 0.08912575F;
        }
        if (entity.isShadowClone()) {
            float amplitude = entity.isShadowHitFlashing() ? 2.0F : 1.0F;
            float distance = entity.isShadowHitFlashing() ? 0.3F : 1.0F;
            mainbody.rotationPointX += -Mth.cos(ageInTicks * 2.27F * amplitude) * 0.59F * distance;
            mainbody.rotationPointZ += -Mth.cos(ageInTicks * 2.6F * amplitude) * 0.55F * distance;
        }
    }
}
