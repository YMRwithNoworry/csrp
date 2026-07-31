package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.effect.BleedMobEffect;
import alku.csrp.effect.CothMobEffect;
import alku.csrp.effect.ViralMobEffect;
import alku.csrp.effect.CorrosionMobEffect;
import alku.csrp.effect.NeedlerMobEffect;
import alku.csrp.effect.RageMobEffect;
import alku.csrp.effect.LinkMobEffect;
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

    private ModMobEffects() {
    }
}
