package alku.csrp.client.model;

import alku.csrp.entity.BuglinEntity;

/** Citadel-backed original Buglin model. */
public final class BuglinModel extends CitadelParasiteModel<BuglinEntity> {
    public BuglinModel() {
        super("buglin");
    }

    @Override
    protected void customize(BuglinEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        getBone("mainbody").ifPresent(bone -> bone.rotateAngleX = 0.0F);
    }
}
