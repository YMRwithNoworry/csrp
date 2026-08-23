package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** Legacy SRP potion types, ported to the 1.21 potion registry. */
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, Csrp.MODID);

    public static final RegistryObject<Potion> COTH = potion("coth", "coth", ModMobEffects.COTH, 2400);
    public static final RegistryObject<Potion> BLEED = potion("bleed", "bleed", ModMobEffects.BLEED, 2400);
    public static final RegistryObject<Potion> FEAR = potion("fear", "fear", ModMobEffects.FEAR, 2400);
    public static final RegistryObject<Potion> RES = potion("res", "antimall", ModMobEffects.ANTIMALL, 2400);
    public static final RegistryObject<Potion> CORRO = potion("corro", "corrosive", ModMobEffects.CORROSIVE, 2400);
    public static final RegistryObject<Potion> VIRA = potion("vira", "viral", ModMobEffects.VIRAL, 2400);
    public static final RegistryObject<Potion> NEEDLER = potion("needler", "needler", ModMobEffects.NEEDLER, 2400);
    public static final RegistryObject<Potion> VOMIT = potion("vomit", "vomit", ModMobEffects.VOMIT, 2400);
    public static final RegistryObject<Potion> DISTORTED_ENLIGHTENMENT =
            potion("distorted_enlightenment", "distorted_enlightenment", ModMobEffects.DISTORTED_ENLIGHTENMENT, 900);
    public static final RegistryObject<Potion> RAGE = potion("rage", "rage", ModMobEffects.RAGE, 2400);
    public static final RegistryObject<Potion> REPEL = potion("repel", "repel", ModMobEffects.REPEL, 2400);
    public static final RegistryObject<Potion> SENSES = potion("senses", "senses", ModMobEffects.SENSES, 2400);
    public static final RegistryObject<Potion> PREY = potion("prey", "prey", ModMobEffects.PREY, 2400);
    public static final RegistryObject<Potion> DEBAR = potion("debar", "debar", ModMobEffects.DEBAR, 2400);
    public static final RegistryObject<Potion> FOSTER = potion("foster", "foster", ModMobEffects.FOSTER, 2400);
    public static final RegistryObject<Potion> LINK = potion("link", "link", ModMobEffects.LINK, 2400);
    public static final RegistryObject<Potion> PIVOT = potion("pivot", "pivot", ModMobEffects.PIVOT, 2400);
    public static final RegistryObject<Potion> JUGG = potion("jugg", "jugg", ModMobEffects.JUGG, 2400);
    public static final RegistryObject<Potion> PARATE = potion("parate", "parate", ModMobEffects.PARATE, 2400);
    public static final RegistryObject<Potion> PRIMITIVE = potion("primitive", "primitive", ModMobEffects.PRIMITIVE, 2400);
    public static final RegistryObject<Potion> ADAPTED = potion("adapted", "adapted", ModMobEffects.ADAPTED, 2400);
    public static final RegistryObject<Potion> PURE = potion("pure", "pure", ModMobEffects.PURE, 2400);
    public static final RegistryObject<Potion> CRUDE = potion("crude", "crude", ModMobEffects.CRUDE, 2400);
    public static final RegistryObject<Potion> FERAL = potion("feral", "feral", ModMobEffects.FERAL, 2400);
    public static final RegistryObject<Potion> NEXUS = potion("nexus", "nexus", ModMobEffects.NEXUS, 2400);
    public static final RegistryObject<Potion> SPOTTED = potion("spotted", "spotted", ModMobEffects.SPOTTED, 2400);
    public static final RegistryObject<Potion> BRAINING = potion("braining", "braining", ModMobEffects.BRAINING, 2400);
    public static final RegistryObject<Potion> NOVISION = potion("novision", "novision", ModMobEffects.NOVISION, 2400);
    public static final RegistryObject<Potion> THE_SIGN = potion("the_sign", "the_sign", ModMobEffects.THE_SIGN, 2400);
    public static final RegistryObject<Potion> CAMOUFLAGE =
            potion("camouflage", "camouflage", ModMobEffects.CAMOUFLAGE, 6000);
    public static final RegistryObject<Potion> WATER_PREDATION =
            potion("water_predation", "water_predation", ModMobEffects.WATER_PREDATION, 2400);
    public static final RegistryObject<Potion> FROSTBITE =
            potion("frostbite", "frostbite", ModMobEffects.FROSTBITE, 2400);
    public static final RegistryObject<Potion> DOD_SMOKE_TRAIL =
            potion("dod_smoke_trail", "dod_smoke_trail", ModMobEffects.DOD_SMOKE_TRAIL, 200);
    public static final RegistryObject<Potion> INDEAF = potion("indeaf", "indeaf", ModMobEffects.INDEAF, 2400);
    public static final RegistryObject<Potion> OVERHEATING = potion("overheating", "overheating", ModMobEffects.OVERHEATING, 2400);
    public static final RegistryObject<Potion> CONTA = potion("conta", "conta", ModMobEffects.CONTAMINATION, 2400);
    public static final RegistryObject<Potion> MUSCLEOUT = potion("muscleout", "muscleout", ModMobEffects.MUSCLEOUT, 2400);
    public static final RegistryObject<Potion> EFFECTPOS = potion("effectpos", "effectpos", ModMobEffects.EFFECTPOS, 2400);
    public static final RegistryObject<Potion> EFFECTNEG = potion("effectneg", "effectneg", ModMobEffects.EFFECTNEG, 2400);
    public static final RegistryObject<Potion> THORNSHADE_THORNS =
            potion("thornshade_thorns", "thornshade_thorns", ModMobEffects.THORNSHADE_THORNS, 60);

    private static RegistryObject<Potion> potion(String id, String name,
                                                          RegistryObject<MobEffect> effect,
                                                          int duration) {
        return POTIONS.register(id, () -> new Potion(name, new MobEffectInstance(effect.get(), duration)));
    }

    private ModPotions() {
    }
}
