package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Legacy SRP potion types, ported to the 1.21 potion registry. */
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, Csrp.MODID);

    public static final DeferredHolder<Potion, Potion> COTH = potion("coth", "coth", ModMobEffects.COTH, 2400);
    public static final DeferredHolder<Potion, Potion> FEAR = potion("fear", "fear", ModMobEffects.FEAR, 2400);
    public static final DeferredHolder<Potion, Potion> RES = potion("res", "antimall", ModMobEffects.ANTIMALL, 2400);
    public static final DeferredHolder<Potion, Potion> CORRO = potion("corro", "corrosive", ModMobEffects.CORROSIVE, 2400);
    public static final DeferredHolder<Potion, Potion> VIRA = potion("vira", "viral", ModMobEffects.VIRAL, 2400);
    public static final DeferredHolder<Potion, Potion> VOMIT = potion("vomit", "vomit", ModMobEffects.VOMIT, 2400);
    public static final DeferredHolder<Potion, Potion> DISTORTED_ENLIGHTENMENT =
            potion("distorted_enlightenment", "distorted_enlightenment", ModMobEffects.DISTORTED_ENLIGHTENMENT, 900);
    public static final DeferredHolder<Potion, Potion> RAGE = potion("rage", "rage", ModMobEffects.RAGE, 2400);
    public static final DeferredHolder<Potion, Potion> REPEL = potion("repel", "repel", ModMobEffects.REPEL, 2400);
    public static final DeferredHolder<Potion, Potion> SENSES = potion("senses", "senses", ModMobEffects.SENSES, 2400);
    public static final DeferredHolder<Potion, Potion> DEBAR = potion("debar", "debar", ModMobEffects.DEBAR, 2400);
    public static final DeferredHolder<Potion, Potion> FOSTER = potion("foster", "foster", ModMobEffects.FOSTER, 2400);
    public static final DeferredHolder<Potion, Potion> LINK = potion("link", "link", ModMobEffects.LINK, 2400);
    public static final DeferredHolder<Potion, Potion> PIVOT = potion("pivot", "pivot", ModMobEffects.PIVOT, 2400);
    public static final DeferredHolder<Potion, Potion> JUGG = potion("jugg", "jugg", ModMobEffects.JUGG, 2400);
    public static final DeferredHolder<Potion, Potion> PARATE = potion("parate", "parate", ModMobEffects.PARATE, 2400);
    public static final DeferredHolder<Potion, Potion> PRIMITIVE = potion("primitive", "primitive", ModMobEffects.PRIMITIVE, 2400);
    public static final DeferredHolder<Potion, Potion> ADAPTED = potion("adapted", "adapted", ModMobEffects.ADAPTED, 2400);
    public static final DeferredHolder<Potion, Potion> PURE = potion("pure", "pure", ModMobEffects.PURE, 2400);
    public static final DeferredHolder<Potion, Potion> CRUDE = potion("crude", "crude", ModMobEffects.CRUDE, 2400);
    public static final DeferredHolder<Potion, Potion> FERAL = potion("feral", "feral", ModMobEffects.FERAL, 2400);
    public static final DeferredHolder<Potion, Potion> NEXUS = potion("nexus", "nexus", ModMobEffects.NEXUS, 2400);
    public static final DeferredHolder<Potion, Potion> SPOTTED = potion("spotted", "spotted", ModMobEffects.SPOTTED, 2400);
    public static final DeferredHolder<Potion, Potion> BRAINING = potion("braining", "braining", ModMobEffects.BRAINING, 2400);
    public static final DeferredHolder<Potion, Potion> NOVISION = potion("novision", "novision", ModMobEffects.NOVISION, 2400);
    public static final DeferredHolder<Potion, Potion> THE_SIGN = potion("the_sign", "the_sign", ModMobEffects.THE_SIGN, 2400);
    public static final DeferredHolder<Potion, Potion> INDEAF = potion("indeaf", "indeaf", ModMobEffects.INDEAF, 2400);
    public static final DeferredHolder<Potion, Potion> OVERHEATING = potion("overheating", "overheating", ModMobEffects.OVERHEATING, 2400);
    public static final DeferredHolder<Potion, Potion> CONTA = potion("conta", "conta", ModMobEffects.CONTAMINATION, 2400);
    public static final DeferredHolder<Potion, Potion> MUSCLEOUT = potion("muscleout", "muscleout", ModMobEffects.MUSCLEOUT, 2400);
    public static final DeferredHolder<Potion, Potion> EFFECTPOS = potion("effectpos", "effectpos", ModMobEffects.EFFECTPOS, 2400);
    public static final DeferredHolder<Potion, Potion> EFFECTNEG = potion("effectneg", "effectneg", ModMobEffects.EFFECTNEG, 2400);
    public static final DeferredHolder<Potion, Potion> THORNSHADE_THORNS =
            potion("thornshade_thorns", "thornshade_thorns", ModMobEffects.THORNSHADE_THORNS, 60);

    private static DeferredHolder<Potion, Potion> potion(String id, String name,
                                                          net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                                          int duration) {
        return POTIONS.register(id, () -> new Potion(name, new MobEffectInstance(effect, duration)));
    }

    private ModPotions() {
    }
}
