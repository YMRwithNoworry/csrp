package alku.csrp.client.model.tabula.generated;

import alku.csrp.client.model.tabula.ModelSRP;
import alku.csrp.entity.TabulaAnimationAccess;
import net.minecraft.world.entity.Mob;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import net.minecraft.util.Mth;

/** Direct Citadel port of SRParasites 1.10.8's Tabula-exported {@code ModelTendrilBano}. */
public final class ModelTabula_tendril_bano extends ModelSRP<Mob> {
   public AdvancedModelBox mainbody;
   public AdvancedModelBox jointMLT0;
   public AdvancedModelBox JD;
   public AdvancedModelBox tacle;
   public AdvancedModelBox jointMLT1;
   public AdvancedModelBox tacle_1;
   public AdvancedModelBox jointMLT2;
   public AdvancedModelBox tacle_2;
   public AdvancedModelBox jointMLT3;
   public AdvancedModelBox tacle_3;
   public AdvancedModelBox jointMLT4;
   public AdvancedModelBox tacle_4;
   public AdvancedModelBox jointMLT5;
   public AdvancedModelBox tacle_5;
   public AdvancedModelBox jointMLT6;
   public AdvancedModelBox tacle_6;

   public ModelTabula_tendril_bano() {
      this.texWidth = 100;
      this.texHeight = 64;
      this.tacle_1 = new AdvancedModelBox(this, 19, 0);
      this.tacle_1.setRotationPoint(0.0F, -1.0F, 1.0F);
      this.tacle_1.addBox(-2.0F, -3.0F, -2.0F, 4, 6, 19, 0.0F);
      this.setRotateAngle(this.tacle_1, -0.4712389F, 0.0F, 0.0F);
      this.tacle_5 = new AdvancedModelBox(this, 54, 30);
      this.tacle_5.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tacle_5.addBox(-2.0F, -2.0F, -1.0F, 4, 4, 11, 0.0F);
      this.setRotateAngle(this.tacle_5, (float) (-Math.PI * 5.0 / 12.0), 0.0F, 0.0F);
      this.tacle_6 = new AdvancedModelBox(this, 73, 34);
      this.tacle_6.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tacle_6.addBox(-1.0F, -1.0F, -1.0F, 2, 2, 11, 0.0F);
      this.setRotateAngle(this.tacle_6, -0.89011794F, 0.0F, 0.0F);
      this.jointMLT5 = new AdvancedModelBox(this, 50, 0);
      this.jointMLT5.setRotationPoint(0.0F, 0.0F, 17.0F);
      this.jointMLT5.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.tacle = new AdvancedModelBox(this, 0, 0);
      this.tacle.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tacle.addBox(-3.0F, -3.0F, -2.0F, 6, 6, 13, 0.0F);
      this.setRotateAngle(this.tacle, 0.0F, 0.0F, (float) Math.PI);
      this.tacle_3 = new AdvancedModelBox(this, 0, 25);
      this.tacle_3.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tacle_3.addBox(-2.0F, -2.0F, -1.0F, 4, 4, 18, 0.0F);
      this.setRotateAngle(this.tacle_3, -0.9773844F, 0.0F, 0.0F);
      this.JD = new AdvancedModelBox(this, 8, 0);
      this.JD.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.JD.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.jointMLT3 = new AdvancedModelBox(this, 33, 0);
      this.jointMLT3.setRotationPoint(0.0F, 0.0F, 18.0F);
      this.jointMLT3.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.mainbody = new AdvancedModelBox(this, 0, 0);
      this.mainbody.setRotationPoint(0.0F, 21.3F, 9.0F);
      this.mainbody.addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.setRotateAngle(this.mainbody, 0.0F, (float) Math.PI, 0.0F);
      this.jointMLT4 = new AdvancedModelBox(this, 46, 0);
      this.jointMLT4.setRotationPoint(0.0F, 0.0F, 15.9F);
      this.jointMLT4.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.jointMLT6 = new AdvancedModelBox(this, 54, 0);
      this.jointMLT6.setRotationPoint(0.0F, 0.0F, 10.0F);
      this.jointMLT6.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.jointMLT0 = new AdvancedModelBox(this, 4, 0);
      this.jointMLT0.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.jointMLT0.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.tacle_4 = new AdvancedModelBox(this, 26, 30);
      this.tacle_4.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tacle_4.addBox(-2.5F, -2.5F, 0.0F, 5, 5, 18, 0.0F);
      this.setRotateAngle(this.tacle_4, (float) (-Math.PI * 2.0 / 5.0), 0.0F, 0.0F);
      this.tacle_2 = new AdvancedModelBox(this, 44, 4);
      this.tacle_2.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tacle_2.addBox(-2.5F, -2.5F, -2.0F, 5, 5, 21, 0.0F);
      this.setRotateAngle(this.tacle_2, -1.1868238F, 0.0F, 0.0F);
      this.jointMLT1 = new AdvancedModelBox(this, 25, 0);
      this.jointMLT1.setRotationPoint(0.0F, 0.0F, 10.5F);
      this.jointMLT1.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.jointMLT2 = new AdvancedModelBox(this, 29, 0);
      this.jointMLT2.setRotationPoint(0.0F, 0.0F, 17.5F);
      this.jointMLT2.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.jointMLT1.addChild(this.tacle_1);
      this.jointMLT5.addChild(this.tacle_5);
      this.jointMLT6.addChild(this.tacle_6);
      this.tacle_4.addChild(this.jointMLT5);
      this.JD.addChild(this.tacle);
      this.jointMLT3.addChild(this.tacle_3);
      this.jointMLT0.addChild(this.JD);
      this.tacle_2.addChild(this.jointMLT3);
      this.tacle_3.addChild(this.jointMLT4);
      this.tacle_5.addChild(this.jointMLT6);
      this.mainbody.addChild(this.jointMLT0);
      this.jointMLT4.addChild(this.tacle_4);
      this.jointMLT2.addChild(this.tacle_2);
      this.tacle.addChild(this.jointMLT1);
      this.tacle_1.addChild(this.jointMLT2);
      this.captureDefaultPose();
   }

   @Override
   protected void func_78087_a(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Mob entityIn) {
      float f1 = Mth.sin(ageInTicks * 0.06F + 0.0F) * 0.2F;
      float f2 = Mth.sin(ageInTicks * 0.07F + 8.0F) * 0.3F;
      float f3 = Mth.sin(ageInTicks * 0.06F + 4.0F) * 0.4F;
      this.jointMLT1.rotateAngleX = f1;
      this.jointMLT2.rotateAngleX = f2;
      this.jointMLT3.rotateAngleX = f3;
      this.jointMLT4.rotateAngleX = f1;
      this.jointMLT5.rotateAngleX = f2;
      this.jointMLT6.rotateAngleX = f3;
   }
}
