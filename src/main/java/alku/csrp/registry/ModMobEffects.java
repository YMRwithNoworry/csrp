package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.effect.BleedMobEffect;
import alku.csrp.effect.CothMobEffect;
import alku.csrp.effect.ViralMobEffect;
import alku.csrp.effect.CorrosionMobEffect;
import alku.csrp.effect.FearMobEffect;
import alku.csrp.effect.FeralMobEffect;
import alku.csrp.effect.NeedlerMobEffect;
import alku.csrp.effect.RageMobEffect;
import alku.csrp.effect.LinkMobEffect;
import alku.csrp.effect.RepelMobEffect;
import alku.csrp.effect.ParasiteKillingMobEffect;
import alku.csrp.effect.MarkerMobEffect;
import alku.csrp.effect.AttributeMobEffect;
import alku.csrp.effect.DodSmokeTrailMobEffect;
import alku.csrp.effect.DistortedEnlightenmentMobEffect;
import alku.csrp.effect.OverheatingMobEffect;
import alku.csrp.effect.ContaminationMobEffect;
import alku.csrp.effect.EffectPosMobEffect;
import alku.csrp.effect.EffectNegMobEffect;
import alku.csrp.effect.IndeafMobEffect;
import alku.csrp.effect.FosterMobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Csrp.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> COTH =
            EFFECTS.register("coth", CothMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> BLEED =
            EFFECTS.register("bleed", BleedMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> VIRAL =
            EFFECTS.register("viral", ViralMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> CORROSION =
            EFFECTS.register("corrosive", CorrosionMobEffect::new);

    /** Original field-name alias used by ported call sites. */
    public static final DeferredHolder<MobEffect, MobEffect> CORROSIVE =
            CORROSION;

    /** Compatibility id for worlds created by early CSRP development builds. */
    public static final DeferredHolder<MobEffect, MobEffect> CORROSION_LEGACY =
            EFFECTS.register("corrosion", CorrosionMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> RAGE =
            EFFECTS.register("rage", RageMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> NEEDLER =
            EFFECTS.register("needler", NeedlerMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> LINK =
            EFFECTS.register("link", LinkMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FEAR =
            EFFECTS.register("fear", FearMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FERAL =
            EFFECTS.register("feral", FeralMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> REPEL =
            EFFECTS.register("repel", RepelMobEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> PRIMITIVE =
            EFFECTS.register("primitive", () -> new ParasiteKillingMobEffect(9391173));

    public static final DeferredHolder<MobEffect, MobEffect> ADAPTED =
            EFFECTS.register("adapted", () -> new ParasiteKillingMobEffect(8345678));

    public static final DeferredHolder<MobEffect, MobEffect> PURE =
            EFFECTS.register("pure", () -> new ParasiteKillingMobEffect(894258));

    public static final DeferredHolder<MobEffect, MobEffect> CRUDE =
            EFFECTS.register("crude", () -> new ParasiteKillingMobEffect(894258));

    public static final DeferredHolder<MobEffect, MobEffect> NEXUS =
            EFFECTS.register("nexus", () -> new ParasiteKillingMobEffect(4749384));

    public static final DeferredHolder<MobEffect, MobEffect> DOD_SMOKE_TRAIL =
            EFFECTS.register("dod_smoke_trail", DodSmokeTrailMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> THORNSHADE_THORNS =
            EFFECTS.register("thornshade_thorns", () -> new MarkerMobEffect(false, 4333438));
    public static final DeferredHolder<MobEffect, MobEffect> ANTIMALL =
            EFFECTS.register("antimall", () -> new MarkerMobEffect(true, 8938092));
    public static final DeferredHolder<MobEffect, MobEffect> DISTORTED_ENLIGHTENMENT =
            EFFECTS.register("distorted_enlightenment", DistortedEnlightenmentMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> VOMIT =
            EFFECTS.register("vomit", () -> new AttributeMobEffect(false, 7498817,
                    Attributes.FOLLOW_RANGE, ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "vomit_follow_range"), 0.9D));
    public static final DeferredHolder<MobEffect, MobEffect> SENSES =
            EFFECTS.register("senses", () -> new AttributeMobEffect(false, 9346775,
                    Attributes.FOLLOW_RANGE, ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "senses_follow_range"), 0.1D));
    public static final DeferredHolder<MobEffect, MobEffect> PREY =
            EFFECTS.register("prey", () -> new MarkerMobEffect(true, 4800055));
    public static final DeferredHolder<MobEffect, MobEffect> DEBAR =
            EFFECTS.register("debar", () -> new MarkerMobEffect(false, 10359627));
    public static final DeferredHolder<MobEffect, MobEffect> FOSTER =
            EFFECTS.register("foster", FosterMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PIVOT =
            EFFECTS.register("pivot", () -> new MarkerMobEffect(false, 16757187));
    public static final DeferredHolder<MobEffect, MobEffect> JUGG =
            EFFECTS.register("jugg", () -> new MarkerMobEffect(false, 12433541));
    public static final DeferredHolder<MobEffect, MobEffect> PARATE =
            EFFECTS.register("parate", () -> new MarkerMobEffect(false, 11753270));
    public static final DeferredHolder<MobEffect, MobEffect> SPOTTED =
            EFFECTS.register("spotted", () -> new MarkerMobEffect(false, 8149607));
    public static final DeferredHolder<MobEffect, MobEffect> BRAINING =
            EFFECTS.register("braining", () -> new MarkerMobEffect(false, 7958149));
    public static final DeferredHolder<MobEffect, MobEffect> NOVISION =
            EFFECTS.register("novision", () -> new MarkerMobEffect(false, 1582649));
    public static final DeferredHolder<MobEffect, MobEffect> INDEAF =
            EFFECTS.register("indeaf", IndeafMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> OVERHEATING =
            EFFECTS.register("overheating", OverheatingMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> CONTAMINATION =
            EFFECTS.register("conta", ContaminationMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> MUSCLEOUT =
            EFFECTS.register("muscleout", () -> new MarkerMobEffect(true, 15499138));
    public static final DeferredHolder<MobEffect, MobEffect> EFFECTPOS =
            EFFECTS.register("effectpos", EffectPosMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> EFFECTNEG =
            EFFECTS.register("effectneg", EffectNegMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> THE_SIGN =
            EFFECTS.register("the_sign", () -> new MarkerMobEffect(false, 8970751));

    private ModMobEffects() {
    }
}
