#!/usr/bin/env nu

# Mechanically converts a decompiled SRParasites 1.12 ModelSRP Java model into
# the Citadel-backed compatibility shape used by this port. Entity-specific
# animation access still needs to be reviewed after generation.
def main [
    source: path,
    destination: path,
    package_name: string,
    entity_type: string = "Mob",
    entity_import: string = "net.minecraft.world.entity.Mob",
    model_name: string = "",
] {
    let original = (open --raw $source)
    let class_body = ($original | str substring ($original | str index-of "public class ")..)
    let class_name = ($class_body | parse -r 'public class (?<name>Model[A-Za-z0-9_]+) extends ModelSRP' | get name.0)
    let output_name = if ($model_name | is-empty) { $class_name } else { $model_name }

    let converted = ($class_body
        | str replace $"public class ($class_name) extends ModelSRP" $"public final class ($output_name) extends ModelSRP<($entity_type)>"
        | str replace $"public ($class_name)()" $"public ($output_name)()"
        | str replace --all "ModelRenderer" "AdvancedModelBox"
        | str replace --all "this.field_78090_t" "this.texWidth"
        | str replace --all "this.field_78089_u" "this.texHeight"
        | str replace --all ".field_78795_f" ".rotateAngleX"
        | str replace --all ".field_78796_g" ".rotateAngleY"
        | str replace --all ".field_78808_h" ".rotateAngleZ"
        | str replace --all ".field_78800_c" ".rotationPointX"
        | str replace --all ".field_78797_d" ".rotationPointY"
        | str replace --all ".field_78798_e" ".rotationPointZ"
        | str replace --all ".field_82906_o" ".offsetX"
        | str replace --all ".field_82908_p" ".offsetY"
        | str replace --all ".field_82907_q" ".offsetZ"
        | str replace --all ".field_78807_k" ".showModel"
        | str replace --all ".field_78809_i" ".mirror"
        | str replace --all ".field_70173_aa" ".tickCount"
        | str replace --all ".field_70181_x" ".getDeltaMovement().y"
        | str replace --all ".diggingModel" ".getDigModel()"
        | str replace --all ".chargeFlag" ".getChargeFlag()"
        | str replace --all ".raining" ".isRaining()"
        | str replace --all ".vomit" ".getVomitTicks()"
        | str replace --all ".isScreaming()" ".getScreaming()"
        | str replace --all ".func_78793_a(" ".setRotationPoint("
        | str replace --all ".func_78790_a(" ".addBox("
        | str replace --all ".func_78792_a(" ".addChild("
        | str replace --all "MathHelper.func_76134_b" "Mth.cos"
        | str replace --all "MathHelper.func_76126_a" "Mth.sin"
        | str replace --all "EntityLivingBase" ($entity_type)
        | str replace --all "EntityParasiteBase" "TabulaAnimationAccess"
        | str replace --all "public void func_78086_a(" "@Override\n   public void prepareMobModel("
        | str replace --regex '(?s)\n\s*public void func_78088_a\([^\{]+\{.*?\n\s*\}\n\n\s*public void func_78087_a' "\n\n   @Override\n   protected void func_78087_a"
        | str replace --regex '(?s)\n\s*public void func_78088_a\(.*\n\}' "\n}"
        | str replace --regex '(?s)\n\s*@Override\s*\n\s*public void renderC\(.*\n\}' "\n}"
        | str replace --regex 'float scaleFactor, Entity entityIn\)' $"float scaleFactor, ($entity_type) entityIn)"
        | str replace --regex '\bEntity entityIn\b' $"($entity_type) entityIn"
        | str replace --regex 'Entity[A-Za-z0-9_]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*\(Entity[A-Za-z0-9_]+\)entityIn;' 'TabulaAnimationAccess $1 = animationAccess(entityIn);'
        | str replace --regex 'Entity[A-Za-z0-9_]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*entityIn;' 'TabulaAnimationAccess $1 = animationAccess(entityIn);'
        | str replace --regex 'Entity[A-Za-z0-9_]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*\(Entity[A-Za-z0-9_]+\)entitylivingbaseIn;' 'TabulaAnimationAccess $1 = animationAccess(entitylivingbaseIn);'
        | str replace --regex '\(Entity[A-Za-z0-9_]+\)entityIn' "entityIn"
        | str replace --regex 'int (var[0-9]+) = false;' 'boolean $1 = false;'
        | str replace --regex '\b(parasite|mob|pa|ven|pod|in)\.tickCount\b' "entityIn.tickCount"
        | str replace --regex '\b(parasite|mob|pa|ven|pod|in)\.getDeltaMovement\(\)' "entityIn.getDeltaMovement()"
        | str replace --regex '\n   \}\n\n   @Override\n   protected void func_78087_a' "\n      this.captureDefaultPose();\n   }\n\n   @Override\n   protected void func_78087_a")

    let header = $"package ($package_name);\n\nimport alku.csrp.client.model.tabula.ModelSRP;\nimport alku.csrp.entity.TabulaAnimationAccess;\nimport ($entity_import);\nimport com.github.alexthe666.citadel.client.model.AdvancedModelBox;\nimport net.minecraft.util.Mth;\n\n/** Direct Citadel port of SRParasites 1.10.8's Tabula-exported {@code ($class_name)}. */\n"

    mkdir ($destination | path dirname)
    $"($header)($converted)" | save --force $destination
}
