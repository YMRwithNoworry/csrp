package alku.csrp.client.model.tabula.generated;

import alku.csrp.client.model.tabula.ModelSRP;
import alku.csrp.entity.TabulaAnimationAccess;
import net.minecraft.world.entity.Mob;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import net.minecraft.util.Mth;

/** Direct Citadel port of SRParasites 1.10.8's Tabula-exported {@code ModelTendrilAnged}. */
public final class ModelTabula_tendril_anged extends ModelSRP<Mob> {
   public AdvancedModelBox mainbody;
   public AdvancedModelBox taclejointUL1;
   public AdvancedModelBox tentacle;
   public AdvancedModelBox taclejointUL2;
   public AdvancedModelBox tentacle_1;
   public AdvancedModelBox taclejointUL3;
   public AdvancedModelBox tentacle_2;
   public AdvancedModelBox taclejointUL4;
   public AdvancedModelBox tentacle_3;

   public ModelTabula_tendril_anged() {
      this.texWidth = 64;
      this.texHeight = 35;
      this.tentacle_3 = new AdvancedModelBox(this, 16, 15);
      this.tentacle_3.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_3.addBox(-0.5F, -0.5F, 0.0F, 1, 1, 16, 0.0F);
      this.setRotateAngle(this.tentacle_3, 0.6457718F, 0.0F, 0.0F);
      this.mainbody = new AdvancedModelBox(this, 0, 0);
      this.mainbody.setRotationPoint(0.0F, 22.0F, 6.0F);
      this.mainbody.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.taclejointUL4 = new AdvancedModelBox(this, 26, 0);
      this.taclejointUL4.setRotationPoint(0.0F, 0.0F, 12.0F);
      this.taclejointUL4.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.tentacle_1 = new AdvancedModelBox(this, 28, 0);
      this.tentacle_1.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_1.addBox(-1.5F, -1.5F, 0.0F, 3, 3, 12, 0.0F);
      this.setRotateAngle(this.tentacle_1, 0.61086524F, 0.0F, 0.0F);
      this.tentacle = new AdvancedModelBox(this, 0, 0);
      this.tentacle.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tentacle.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 10, 0.0F);
      this.taclejointUL1 = new AdvancedModelBox(this, 4, 0);
      this.taclejointUL1.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.taclejointUL1.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.setRotateAngle(this.taclejointUL1, 0.0F, (float) Math.PI, 0.0F);
      this.taclejointUL2 = new AdvancedModelBox(this, 18, 0);
      this.taclejointUL2.setRotationPoint(0.0F, 0.0F, 8.0F);
      this.taclejointUL2.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.taclejointUL3 = new AdvancedModelBox(this, 22, 0);
      this.taclejointUL3.setRotationPoint(0.0F, 0.0F, 10.0F);
      this.taclejointUL3.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.tentacle_2 = new AdvancedModelBox(this, 0, 14);
      this.tentacle_2.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_2.addBox(-1.0F, -1.0F, 0.0F, 2, 2, 14, 0.0F);
      this.setRotateAngle(this.tentacle_2, 0.40142572F, 0.0F, 0.0F);
      this.taclejointUL4.addChild(this.tentacle_3);
      this.tentacle_2.addChild(this.taclejointUL4);
      this.taclejointUL2.addChild(this.tentacle_1);
      this.taclejointUL1.addChild(this.tentacle);
      this.mainbody.addChild(this.taclejointUL1);
      this.tentacle.addChild(this.taclejointUL2);
      this.tentacle_1.addChild(this.taclejointUL3);
      this.taclejointUL3.addChild(this.tentacle_2);
      this.captureDefaultPose();
   }

   @Override
   protected void func_78087_a(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Mob entityIn) {
      float f1 = Mth.sin(ageInTicks * 0.06F + 0.0F) * 0.2F;
      float f2 = Mth.sin(ageInTicks * 0.07F + 8.0F) * 0.3F;
      float f3 = Mth.sin(ageInTicks * 0.06F + 4.0F) * 0.4F;
      this.taclejointUL2.rotateAngleX = f1;
      this.taclejointUL3.rotateAngleX = f2;
      this.taclejointUL4.rotateAngleX = f3;
   }
}
