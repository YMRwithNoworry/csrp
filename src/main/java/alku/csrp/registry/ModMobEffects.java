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
import net.minecraft.core.registries.Registries;
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

    private ModMobEffects() {
    }
}
