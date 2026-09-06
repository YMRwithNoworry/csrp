package alku.csrp.client.model.tabula.generated;

import alku.csrp.client.model.tabula.ModelSRP;
import alku.csrp.entity.TabulaAnimationAccess;
import net.minecraft.world.entity.Mob;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import net.minecraft.util.Mth;

/** Direct Citadel port of SRParasites 1.10.8's Tabula-exported {@code ModelTendrilCanra}. */
public final class ModelTabula_tendril_canra extends ModelSRP<Mob> {
   public AdvancedModelBox mainbody;
   public AdvancedModelBox taclejointL1;
   public AdvancedModelBox tentacle;
   public AdvancedModelBox taclejointL2;
   public AdvancedModelBox tentacle_1;
   public AdvancedModelBox taclejointL3;
   public AdvancedModelBox tentacle_2;
   public AdvancedModelBox taclejointL4;
   public AdvancedModelBox tentacle_3;

   public ModelTabula_tendril_canra() {
      this.texWidth = 64;
      this.texHeight = 32;
      this.tentacle_3 = new AdvancedModelBox(this, 30, 16);
      this.tentacle_3.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_3.addBox(-1.0F, -1.0F, -1.0F, 2, 2, 12, 0.0F);
      this.setRotateAngle(this.tentacle_3, 0.0F, -0.80285144F, 0.0F);
      this.mainbody = new AdvancedModelBox(this, 0, 0);
      this.mainbody.setRotationPoint(0.0F, 21.7F, 5.0F);
      this.mainbody.addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.setRotateAngle(this.mainbody, 0.0F, (float) Math.PI, (float) (-Math.PI / 2));
      this.taclejointL1 = new AdvancedModelBox(this, 4, 0);
      this.taclejointL1.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.taclejointL1.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.taclejointL3 = new AdvancedModelBox(this, 24, 0);
      this.taclejointL3.setRotationPoint(0.0F, 0.0F, 9.0F);
      this.taclejointL3.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.tentacle_2 = new AdvancedModelBox(this, 0, 15);
      this.tentacle_2.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_2.addBox(-1.5F, -1.5F, -1.0F, 3, 3, 12, 0.0F);
      this.setRotateAngle(this.tentacle_2, 0.0F, (float) (-Math.PI / 5), 0.0F);
      this.tentacle = new AdvancedModelBox(this, 0, 0);
      this.tentacle.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.tentacle.addBox(-2.5F, -2.5F, -1.0F, 5, 5, 10, 0.0F);
      this.tentacle_1 = new AdvancedModelBox(this, 30, 0);
      this.tentacle_1.setRotationPoint(0.0F, 0.0F, 1.0F);
      this.tentacle_1.addBox(-2.0F, -2.0F, -1.0F, 4, 4, 12, 0.0F);
      this.setRotateAngle(this.tentacle_1, 0.0F, -0.54105204F, 0.0F);
      this.taclejointL4 = new AdvancedModelBox(this, 28, 0);
      this.taclejointL4.setRotationPoint(0.0F, 0.0F, 9.0F);
      this.taclejointL4.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.taclejointL2 = new AdvancedModelBox(this, 20, 0);
      this.taclejointL2.setRotationPoint(0.0F, 0.0F, 7.0F);
      this.taclejointL2.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F);
      this.taclejointL4.addChild(this.tentacle_3);
      this.mainbody.addChild(this.taclejointL1);
      this.tentacle_1.addChild(this.taclejointL3);
      this.taclejointL3.addChild(this.tentacle_2);
      this.taclejointL1.addChild(this.tentacle);
      this.taclejointL2.addChild(this.tentacle_1);
      this.tentacle_2.addChild(this.taclejointL4);
      this.tentacle.addChild(this.taclejointL2);
      this.captureDefaultPose();
   }
}
