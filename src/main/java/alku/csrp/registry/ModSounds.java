package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Csrp.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> REGISTERED = new LinkedHashMap<>();

    public static final RegistryObject<SoundEvent> BUGLIN_GROWL = register("lodo.growl");
    public static final RegistryObject<SoundEvent> BUGLIN_HURT = register("lodo.hurt");
    public static final RegistryObject<SoundEvent> BUGLIN_DEATH = register("lodo.death");
    public static final RegistryObject<SoundEvent> BUGLIN_GROW = register("lodo.mudo");
    public static final RegistryObject<SoundEvent> BUGLIN_EMERGE = register("lodo.emerge");
    public static final RegistryObject<SoundEvent> RUPTER_LIVING = register("rupter.living");
    public static final RegistryObject<SoundEvent> RUPTER_HURT = register("rupter.hurt");
    public static final RegistryObject<SoundEvent> RUPTER_DEATH = register("rupter.death");
    public static final RegistryObject<SoundEvent> RUPTER_STEP = register("rupter.step");
    public static final RegistryObject<SoundEvent> RUPTER_CLOUD = register("rupter.cloud");
    public static final RegistryObject<SoundEvent> DREDGE_LIVING = register("done.growl");
    public static final RegistryObject<SoundEvent> DREDGE_HURT = register("done.hurt");
    public static final RegistryObject<SoundEvent> DREDGE_DEATH = register("done.death");
    public static final RegistryObject<SoundEvent> THRALL_LIVING = register("mes.growl");
    public static final RegistryObject<SoundEvent> THRALL_HURT = register("mes.hurt");
    public static final RegistryObject<SoundEvent> THRALL_DEATH = register("mes.death");
    public static final RegistryObject<SoundEvent> INCOMPLETE_SMALL_LIVING = register("inhoos.growl");
    public static final RegistryObject<SoundEvent> INCOMPLETE_SMALL_HURT = register("inhoos.hurt");
    public static final RegistryObject<SoundEvent> INCOMPLETE_SMALL_DEATH = register("inhoos.death");
    public static final RegistryObject<SoundEvent> INCOMPLETE_MEDIUM_LIVING = register("inhoom.growl");
    public static final RegistryObject<SoundEvent> INCOMPLETE_MEDIUM_HURT = register("inhoom.hurt");
    public static final RegistryObject<SoundEvent> INCOMPLETE_MEDIUM_DEATH = register("inhoom.death");
    public static final RegistryObject<SoundEvent> LITE_FLESH_SLIDE = register("lite_flesh.slide");
    public static final RegistryObject<SoundEvent> DISC_THREE = register("srparasites.discthree");
    public static final RegistryObject<SoundEvent> HOST_LIVING = register("host.growl");
    public static final RegistryObject<SoundEvent> HOST_HURT = register("host.hurt");
    public static final RegistryObject<SoundEvent> HOST_DEATH = register("host.death");
    public static final RegistryObject<SoundEvent> DRACONITE_LIVING = register("draconite.living");
    public static final RegistryObject<SoundEvent> DRACONITE_DEATH = register("draconite.death");
    public static final RegistryObject<SoundEvent> DRACONITE_FIRE_SHOOT = register("draconite.fire_shoot");

    // 通用寄生体音效
    public static final RegistryObject<SoundEvent> MOB_SHOOT = register("mob.shoot");
    public static final RegistryObject<SoundEvent> MOB_SWIPE = register("mob.swipe");
    public static final RegistryObject<SoundEvent> MOB_TENDRIL = register("mob.tendril");

    // 适应型音效
    public static final RegistryObject<SoundEvent> ADAPTED_DIG = register("adapted.dig");
    public static final RegistryObject<SoundEvent> ADAPTED_V = register("adapted.v");

    // 攻击音效
    public static final RegistryObject<SoundEvent> ATTACK_BANO = register("attack.bano");
    public static final RegistryObject<SoundEvent> ATTACK_EMANA = register("attack.emana");
    public static final RegistryObject<SoundEvent> ATTACK_THROW = register("attack.throw");

    // Emana系列音效
    public static final RegistryObject<SoundEvent> EMANA_GROWL = register("emana.growl");
    public static final RegistryObject<SoundEvent> EMANA_HURT = register("emana.hurt");
    public static final RegistryObject<SoundEvent> EMANA_DEATH = register("emana.death");
    public static final RegistryObject<SoundEvent> EMANA_SHOOTING = register("emana.shooting");
    public static final RegistryObject<SoundEvent> AEMANA_SHOOTINGPOST = register("aemana.shootingpost");

    // 其他音效
    public static final RegistryObject<SoundEvent> PARASITE_MELT = register("parasite.melt");
    public static final RegistryObject<SoundEvent> SHRIMP_EAT = register("shrimp.eat");
    public static final RegistryObject<SoundEvent> SHYCO_SPECIAL = register("shyco.special");

    public static final RegistryObject<SoundEvent> KIRIN_LIVING = register("kirin.living");
    public static final RegistryObject<SoundEvent> KIRIN_HURT = register("kirin.hurt");
    public static final RegistryObject<SoundEvent> KIRIN_DEATH = register("kirin.death");
    public static final RegistryObject<SoundEvent> KIRIN_BLACK_HOLE = register("kirin.black_hole");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_LIVING = register("sim_adventurer.living");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_HURT = register("sim_adventurer.hurt");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_DEATH = register("sim_adventurer.death");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_MELT = register("sim_adventurer.melt");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_EXPLODE = register("sim_adventurer.explode");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_HEAD_LIVING = register("sim_adventurer_head.living");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_HEAD_HURT = register("sim_adventurer_head.hurt");
    public static final RegistryObject<SoundEvent> SIM_ADVENTURER_HEAD_DEATH = register("sim_adventurer_head.death");
    public static final RegistryObject<SoundEvent> MOVING_FLESH_LIVING = register("moving_flesh.living");
    public static final RegistryObject<SoundEvent> MOVING_FLESH_HURT = register("moving_flesh.hurt");
    public static final RegistryObject<SoundEvent> MOVING_FLESH_DEATH = register("moving_flesh.death");
    public static final RegistryObject<SoundEvent> MOVING_FLESH_EAT = register("moving_flesh.eat");
    public static final RegistryObject<SoundEvent> MOVING_FLESH_GROW = register("moving_flesh.grow");
    public static final RegistryObject<SoundEvent> MOVING_FLESH_PRIMITIVE = register("moving_flesh.primitive");
    public static final RegistryObject<SoundEvent> MARAUDER_LIVING = register("marauder.living");
    public static final RegistryObject<SoundEvent> MARAUDER_HURT = register("marauder.hurt");
    public static final RegistryObject<SoundEvent> MARAUDER_DEATH = register("marauder.death");
    public static final RegistryObject<SoundEvent> SCENT_WAVE = register("scent.wave");
    public static final RegistryObject<SoundEvent> SCENT_MUSIC = register("scent.music");
    public static final RegistryObject<SoundEvent> RATHOL_BOOM = register("rathol.boom");
    public static final RegistryObject<SoundEvent> NADE_IGNITE = register("nade.s");
    public static final RegistryObject<SoundEvent> DORPA_RANGE = register("dorpa.range");
    public static final RegistryObject<SoundEvent> CARRIER_COLONY_LIVING = register("vesta.growl");
    public static final RegistryObject<SoundEvent> CARRIER_COLONY_HURT = register("vesta.hurt");
    public static final RegistryObject<SoundEvent> CARRIER_COLONY_DEATH = register("vesta.death");
    public static final RegistryObject<SoundEvent> COLONY_CREATED = register("colony.one");
    public static final RegistryObject<SoundEvent> HEAVY_MULTIPLE_STEP = register("step.heavy_multiple");
    public static final RegistryObject<SoundEvent> MOB_EXPLOSION = register("mob.explotion");
    public static final RegistryObject<SoundEvent> INFECTED_ENDERMAN_PORTAL =
            register("infectedenderman.portal");
    public static final RegistryObject<SoundEvent> ORB_START = register("orb.s");
    public static final RegistryObject<SoundEvent> ORB_END = register("orb.e");
    public static final RegistryObject<SoundEvent> COMPENDIUM_UNLOCK_ENTITY = register("compendium.unlock_entity");
    public static final RegistryObject<SoundEvent> COMPENDIUM_UNLOCK_BLOCK = register("compendium.unlock_block");
    public static final RegistryObject<SoundEvent> COMPENDIUM_UNLOCK_CELESTIAL = register("compendium.unlock_celestial");
    public static final RegistryObject<SoundEvent> COMPENDIUM_UNLOCK_EFFECT = register("compendium.unlock_effect");
    public static final RegistryObject<SoundEvent> COMPENDIUM_UNLOCK_ITEM = register("compendium.unlock_item");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_1 = register("evolution.phase1");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_2 = register("evolution.phase2");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_3 = register("evolution.phase3");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_4 = register("evolution.phase4");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_5 = register("evolution.phase5");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_6 = register("evolution.phase6");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_7 = register("evolution.phase7");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_8 = register("evolution.phase8");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_9 = register("evolution.phase9");
    public static final RegistryObject<SoundEvent> EVOLUTION_PHASE_10 = register("evolution.phase10");
    public static final RegistryObject<SoundEvent> DARK_DAYS_START = register("celestial.dark_days_start");
    public static final RegistryObject<SoundEvent> DARK_DAYS_RUMBLE = register("celestial.dark_days_rumble");
    public static final RegistryObject<SoundEvent> DARK_DAYS_ENDING = register("celestial.dark_days_ending");
    public static final RegistryObject<SoundEvent> LURE_USE = register("lure.use");
    public static final RegistryObject<SoundEvent> CARCASS_USE = register("lure.carcass");
    public static final RegistryObject<SoundEvent> ADAPTATION_PARTIAL = register("adaptation.parcial");
    public static final RegistryObject<SoundEvent> ADAPTATION_FULL = register("adaptation.full");
    public static final RegistryObject<SoundEvent> PRIMITIVE_BURROWER_DIG = register("zaa.dig");
    public static final RegistryObject<SoundEvent> PRIMITIVE_TOZOON_DIG = register("wymo.dig");
    public static final RegistryObject<SoundEvent> ADAPTED_BURROWER_DIG = register("azaa.dig");
    public static final RegistryObject<SoundEvent> ADAPTED_TOZOON_DIG = register("awymo.dig");

    static {
        SoundEventCatalog.EVENTS.forEach(ModSounds::register);
    }

    private ModSounds() {
    }

    public static SoundEvent evolutionPhase(int phase) {
        return switch (phase) {
            case 1 -> EVOLUTION_PHASE_1.get();
            case 2 -> EVOLUTION_PHASE_2.get();
            case 3 -> EVOLUTION_PHASE_3.get();
            case 4 -> EVOLUTION_PHASE_4.get();
            case 5 -> EVOLUTION_PHASE_5.get();
            case 6 -> EVOLUTION_PHASE_6.get();
            case 7 -> EVOLUTION_PHASE_7.get();
            case 8 -> EVOLUTION_PHASE_8.get();
            case 9 -> EVOLUTION_PHASE_9.get();
            default -> EVOLUTION_PHASE_10.get();
        };
    }

    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> sound = REGISTERED.get(name);
        if (sound == null) {
            throw new IllegalArgumentException("Unknown csrp sound event: " + name);
        }
        return sound.get();
    }

    private static RegistryObject<SoundEvent> register(String name) {
        RegistryObject<SoundEvent> existing = REGISTERED.get(name);
        if (existing != null) {
            return existing;
        }
        ResourceLocation id = new ResourceLocation(Csrp.MODID, name);
        RegistryObject<SoundEvent> sound =
                SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
        REGISTERED.put(name, sound);
        return sound;
    }
}
