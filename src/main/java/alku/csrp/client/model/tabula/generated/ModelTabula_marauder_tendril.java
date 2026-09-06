package alku.csrp.client.model.tabula.generated;

import alku.csrp.client.model.tabula.ModelSRP;
import alku.csrp.entity.TabulaAnimationAccess;
import net.minecraft.world.entity.Mob;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import net.minecraft.util.Mth;

/** Direct Citadel port of SRParasites 1.10.8's Tabula-exported {@code ModelTendrilEsor}. */
public final class ModelTabula_marauder_tendril extends ModelSRP<Mob> {
   public AdvancedModelBox mainbody;
   public AdvancedModelBox taclejointLA0;
   public AdvancedModelBox tentacle;
   public AdvancedModelBox taclejointLA1;
   public AdvancedModelBox tentacle_1;
   public AdvancedModelBox taclejointLA2;
   public AdvancedModelBox tentacle_2;
   public AdvancedModelBox taclejointLA3;
   public AdvancedModelBox tentacle_3;

   public ModelTabula_marauder_tendril() {
      this.texWidth = 55;
      this.texHeight = 32;
      this.taclejointLA0 = new AdvancedModelBox(this, 4, 0);
      this.taclejointLA0.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.taclejointLA0.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.taclejointLA1 = new AdvancedModelBox(this, 14, 0);
      this.taclejointLA1.setRotationPoint(0.0F, 0.0F, 6.0F);
      this.taclejointLA1.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.tentacle_1 = new AdvancedModelBox(this, 14, 0);
      this.tentacle_1.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_1.addBox(-1.5F, -1.5F, -1.0F, 3, 3, 7, 0.0F);
      this.setRotateAngle(this.tentacle_1, 0.5061455F, 0.0F, 0.0F);
      this.tentacle = new AdvancedModelBox(this, 0, 0);
      this.tentacle.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tentacle.addBox(-1.0F, -1.0F, -2.0F, 2, 2, 10, 0.0F);
      this.tentacle_3 = new AdvancedModelBox(this, 0, 12);
      this.tentacle_3.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_3.addBox(-0.5F, -0.5F, -1.0F, 1, 1, 12, 0.0F);
      this.setRotateAngle(this.tentacle_3, 0.82030475F, 0.0F, 0.0F);
      this.tentacle_2 = new AdvancedModelBox(this, 22, 0);
      this.tentacle_2.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_2.addBox(-1.0F, -1.0F, -1.0F, 2, 2, 12, 0.0F);
      this.setRotateAngle(this.tentacle_2, 0.8552113F, 0.0F, 0.0F);
      this.taclejointLA2 = new AdvancedModelBox(this, 27, 0);
      this.taclejointLA2.setRotationPoint(0.0F, 0.0F, 4.0F);
      this.taclejointLA2.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.mainbody = new AdvancedModelBox(this, 0, 0);
      this.mainbody.setRotationPoint(0.0F, 23.0F, 6.0F);
      this.mainbody.addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.setRotateAngle(this.mainbody, 0.0F, (float) Math.PI, 0.0F);
      this.taclejointLA3 = new AdvancedModelBox(this, 38, 0);
      this.taclejointLA3.setRotationPoint(0.0F, 0.0F, 9.0F);
      this.taclejointLA3.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.mainbody.addChild(this.taclejointLA0);
      this.tentacle.addChild(this.taclejointLA1);
      this.taclejointLA1.addChild(this.tentacle_1);
      this.taclejointLA0.addChild(this.tentacle);
      this.taclejointLA3.addChild(this.tentacle_3);
      this.taclejointLA2.addChild(this.tentacle_2);
      this.tentacle_1.addChild(this.taclejointLA2);
      this.tentacle_2.addChild(this.taclejointLA3);
      this.captureDefaultPose();
   }

   @Override
   protected void func_78087_a(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Mob entityIn) {
      float f1 = Mth.sin(ageInTicks * 0.06F + 0.0F) * 0.2F;
      float f2 = Mth.sin(ageInTicks * 0.07F + 8.0F) * 0.3F;
      float f3 = Mth.sin(ageInTicks * 0.06F + 4.0F) * 0.4F;
      this.taclejointLA1.rotateAngleX = f1;
      this.taclejointLA2.rotateAngleX = f2;
      this.taclejointLA3.rotateAngleX = f3;
   }
}
