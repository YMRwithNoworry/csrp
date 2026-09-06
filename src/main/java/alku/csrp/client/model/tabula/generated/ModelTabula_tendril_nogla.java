package alku.csrp.client.model.tabula.generated;

import alku.csrp.client.model.tabula.ModelSRP;
import alku.csrp.entity.TabulaAnimationAccess;
import net.minecraft.world.entity.Mob;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import net.minecraft.util.Mth;

/** Direct Citadel port of SRParasites 1.10.8's Tabula-exported {@code ModelTendrilNogla}. */
public final class ModelTabula_tendril_nogla extends ModelSRP<Mob> {
   public AdvancedModelBox mainbody;
   public AdvancedModelBox taclejointL;
   public AdvancedModelBox tentacle;
   public AdvancedModelBox taclejointL1;
   public AdvancedModelBox tentacle_1;
   public AdvancedModelBox taclejointL2;
   public AdvancedModelBox tentacle_2;
   public AdvancedModelBox taclejointL3;
   public AdvancedModelBox tentacle_3;

   public ModelTabula_tendril_nogla() {
      this.texWidth = 80;
      this.texHeight = 32;
      this.taclejointL2 = new AdvancedModelBox(this, 32, 0);
      this.taclejointL2.setRotationPoint(0.0F, -0.2F, 9.0F);
      this.taclejointL2.addBox(-1.0F, -1.0F, 0.0F, 2, 2, 1, 0.0F);
      this.tentacle_1 = new AdvancedModelBox(this, 38, 0);
      this.tentacle_1.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_1.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 11, 0.0F);
      this.setRotateAngle(this.tentacle_1, -0.61086524F, 0.0F, 0.0F);
      this.tentacle_3 = new AdvancedModelBox(this, 42, 16);
      this.tentacle_3.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_3.addBox(-1.0F, -1.0F, 0.0F, 2, 2, 13, 0.0F);
      this.setRotateAngle(this.tentacle_3, (float) (-Math.PI / 4), 0.0F, 0.0F);
      this.taclejointL1 = new AdvancedModelBox(this, 24, 0);
      this.taclejointL1.setRotationPoint(0.0F, -0.2F, 11.0F);
      this.taclejointL1.addBox(-1.5F, -1.5F, 0.0F, 3, 3, 1, 0.0F);
      this.mainbody = new AdvancedModelBox(this, 0, 0);
      this.mainbody.setRotationPoint(0.0F, 21.5F, 6.0F);
      this.mainbody.addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.tentacle_2 = new AdvancedModelBox(this, 27, 15);
      this.tentacle_2.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_2.addBox(-1.5F, -1.5F, 0.0F, 3, 3, 11, 0.0F);
      this.setRotateAngle(this.tentacle_2, -0.8552113F, 0.0F, 0.0F);
      this.taclejointL3 = new AdvancedModelBox(this, 38, 0);
      this.taclejointL3.setRotationPoint(0.0F, -0.2F, 9.0F);
      this.taclejointL3.addBox(-0.5F, -0.5F, 0.0F, 1, 1, 1, 0.0F);
      this.taclejointL = new AdvancedModelBox(this, 4, 0);
      this.taclejointL.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.taclejointL.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 1, 0.0F);
      this.setRotateAngle(this.taclejointL, 0.0F, (float) Math.PI, (float) Math.PI);
      this.tentacle = new AdvancedModelBox(this, 0, 0);
      this.tentacle.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tentacle.addBox(-2.5F, -2.5F, -1.0F, 5, 5, 14, 0.0F);
      this.tentacle_1.addChild(this.taclejointL2);
      this.taclejointL1.addChild(this.tentacle_1);
      this.taclejointL3.addChild(this.tentacle_3);
      this.tentacle.addChild(this.taclejointL1);
      this.taclejointL2.addChild(this.tentacle_2);
      this.tentacle_2.addChild(this.taclejointL3);
      this.mainbody.addChild(this.taclejointL);
      this.taclejointL.addChild(this.tentacle);
      this.captureDefaultPose();
   }

   @Override
   protected void func_78087_a(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Mob entityIn) {
      float f1 = Mth.sin(ageInTicks * 0.06F + 0.0F) * 0.2F;
      float f2 = Mth.sin(ageInTicks * 0.07F + 8.0F) * 0.3F;
      float f3 = Mth.sin(ageInTicks * 0.06F + 4.0F) * 0.4F;
      this.taclejointL1.rotateAngleX = f1;
      this.taclejointL2.rotateAngleX = f2;
      this.taclejointL3.rotateAngleX = f3;
   }
}
