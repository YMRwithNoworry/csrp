package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** Legacy SRP potion types, ported to the 1.21 potion registry. */
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, Csrp.MODID);

    public static final RegistryObject<Potion> COTH = potion("coth", "coth", ModMobEffects.COTH.get(), 2400);
    public static final RegistryObject<Potion> BLEED = potion("bleed", "bleed", ModMobEffects.BLEED.get(), 2400);
    public static final RegistryObject<Potion> FEAR = potion("fear", "fear", ModMobEffects.FEAR.get(), 2400);
    public static final RegistryObject<Potion> RES = potion("res", "antimall", ModMobEffects.ANTIMALL.get(), 2400);
    public static final RegistryObject<Potion> CORRO = potion("corro", "corrosive", ModMobEffects.CORROSIVE.get(), 2400);
    public static final RegistryObject<Potion> VIRA = potion("vira", "viral", ModMobEffects.VIRAL.get(), 2400);
    public static final RegistryObject<Potion> NEEDLER = potion("needler", "needler", ModMobEffects.NEEDLER.get(), 2400);
    public static final RegistryObject<Potion> VOMIT = potion("vomit", "vomit", ModMobEffects.VOMIT.get(), 2400);
    public static final RegistryObject<Potion> DISTORTED_ENLIGHTENMENT =
            potion("distorted_enlightenment", "distorted_enlightenment", ModMobEffects.DISTORTED_ENLIGHTENMENT.get(), 900);
    public static final RegistryObject<Potion> RAGE = potion("rage", "rage", ModMobEffects.RAGE.get(), 2400);
    public static final RegistryObject<Potion> REPEL = potion("repel", "repel", ModMobEffects.REPEL.get(), 2400);
    public static final RegistryObject<Potion> SENSES = potion("senses", "senses", ModMobEffects.SENSES.get(), 2400);
    public static final RegistryObject<Potion> PREY = potion("prey", "prey", ModMobEffects.PREY.get(), 2400);
    public static final RegistryObject<Potion> DEBAR = potion("debar", "debar", ModMobEffects.DEBAR.get(), 2400);
    public static final RegistryObject<Potion> FOSTER = potion("foster", "foster", ModMobEffects.FOSTER.get(), 2400);
    public static final RegistryObject<Potion> LINK = potion("link", "link", ModMobEffects.LINK.get(), 2400);
    public static final RegistryObject<Potion> PIVOT = potion("pivot", "pivot", ModMobEffects.PIVOT.get(), 2400);
    public static final RegistryObject<Potion> JUGG = potion("jugg", "jugg", ModMobEffects.JUGG.get(), 2400);
    public static final RegistryObject<Potion> PARATE = potion("parate", "parate", ModMobEffects.PARATE.get(), 2400);
    public static final RegistryObject<Potion> PRIMITIVE = potion("primitive", "primitive", ModMobEffects.PRIMITIVE.get(), 2400);
    public static final RegistryObject<Potion> ADAPTED = potion("adapted", "adapted", ModMobEffects.ADAPTED.get(), 2400);
    public static final RegistryObject<Potion> PURE = potion("pure", "pure", ModMobEffects.PURE.get(), 2400);
    public static final RegistryObject<Potion> CRUDE = potion("crude", "crude", ModMobEffects.CRUDE.get(), 2400);
    public static final RegistryObject<Potion> FERAL = potion("feral", "feral", ModMobEffects.FERAL.get(), 2400);
    public static final RegistryObject<Potion> NEXUS = potion("nexus", "nexus", ModMobEffects.NEXUS.get(), 2400);
    public static final RegistryObject<Potion> SPOTTED = potion("spotted", "spotted", ModMobEffects.SPOTTED.get(), 2400);
    public static final RegistryObject<Potion> BRAINING = potion("braining", "braining", ModMobEffects.BRAINING.get(), 2400);
    public static final RegistryObject<Potion> NOVISION = potion("novision", "novision", ModMobEffects.NOVISION.get(), 2400);
    public static final RegistryObject<Potion> THE_SIGN = potion("the_sign", "the_sign", ModMobEffects.THE_SIGN.get(), 2400);
    public static final RegistryObject<Potion> CAMOUFLAGE =
            potion("camouflage", "camouflage", ModMobEffects.CAMOUFLAGE.get(), 6000);
    public static final RegistryObject<Potion> WATER_PREDATION =
            potion("water_predation", "water_predation", ModMobEffects.WATER_PREDATION.get(), 2400);
    public static final RegistryObject<Potion> FROSTBITE =
            potion("frostbite", "frostbite", ModMobEffects.FROSTBITE.get(), 2400);
    public static final RegistryObject<Potion> DOD_SMOKE_TRAIL =
            potion("dod_smoke_trail", "dod_smoke_trail", ModMobEffects.DOD_SMOKE_TRAIL.get(), 200);
    public static final RegistryObject<Potion> INDEAF = potion("indeaf", "indeaf", ModMobEffects.INDEAF.get(), 2400);
    public static final RegistryObject<Potion> OVERHEATING = potion("overheating", "overheating", ModMobEffects.OVERHEATING.get(), 2400);
    public static final RegistryObject<Potion> CONTA = potion("conta", "conta", ModMobEffects.CONTAMINATION.get(), 2400);
    public static final RegistryObject<Potion> MUSCLEOUT = potion("muscleout", "muscleout", ModMobEffects.MUSCLEOUT.get(), 2400);
    public static final RegistryObject<Potion> EFFECTPOS = potion("effectpos", "effectpos", ModMobEffects.EFFECTPOS.get(), 2400);
    public static final RegistryObject<Potion> EFFECTNEG = potion("effectneg", "effectneg", ModMobEffects.EFFECTNEG.get(), 2400);
    public static final RegistryObject<Potion> THORNSHADE_THORNS =
            potion("thornshade_thorns", "thornshade_thorns", ModMobEffects.THORNSHADE_THORNS.get(), 60);

    private static RegistryObject<Potion> potion(String id, String name,
                                                          net.minecraft.world.effect.MobEffect effect,
                                                          int duration) {
        return POTIONS.register(id, () -> new Potion(name, new MobEffectInstance(effect, duration)));
    }

    private ModPotions() {
    }
}
