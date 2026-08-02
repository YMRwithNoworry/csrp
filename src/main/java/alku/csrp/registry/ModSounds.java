package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Csrp.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_GROWL = register("lodo.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_HURT = register("lodo.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_DEATH = register("lodo.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_GROW = register("lodo.mudo");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_EMERGE = register("lodo.emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUPTER_LIVING = register("rupter.living");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUPTER_HURT = register("rupter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUPTER_DEATH = register("rupter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUPTER_STEP = register("rupter.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUPTER_CLOUD = register("rupter.cloud");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREDGE_LIVING = register("done.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREDGE_HURT = register("done.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREDGE_DEATH = register("done.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> THRALL_LIVING = register("mes.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> THRALL_HURT = register("mes.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> THRALL_DEATH = register("mes.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCOMPLETE_SMALL_LIVING = register("inhoos.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCOMPLETE_SMALL_HURT = register("inhoos.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCOMPLETE_SMALL_DEATH = register("inhoos.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCOMPLETE_MEDIUM_LIVING = register("inhoom.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCOMPLETE_MEDIUM_HURT = register("inhoom.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> INCOMPLETE_MEDIUM_DEATH = register("inhoom.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LITE_FLESH_SLIDE = register("lite_flesh.slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_LIVING = register("host.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_HURT = register("host.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOST_DEATH = register("host.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRACONITE_LIVING = register("draconite.living");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRACONITE_DEATH = register("draconite.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRACONITE_FIRE_SHOOT = register("draconite.fire_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIRIN_LIVING = register("kirin.living");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIRIN_HURT = register("kirin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIRIN_DEATH = register("kirin.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIRIN_BLACK_HOLE = register("kirin.black_hole");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_LIVING = register("sim_adventurer.living");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_HURT = register("sim_adventurer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_DEATH = register("sim_adventurer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_MELT = register("sim_adventurer.melt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_EXPLODE = register("sim_adventurer.explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_HEAD_LIVING = register("sim_adventurer_head.living");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_HEAD_HURT = register("sim_adventurer_head.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIM_ADVENTURER_HEAD_DEATH = register("sim_adventurer_head.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOVING_FLESH_LIVING = register("moving_flesh.living");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOVING_FLESH_HURT = register("moving_flesh.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOVING_FLESH_DEATH = register("moving_flesh.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOVING_FLESH_EAT = register("moving_flesh.eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOVING_FLESH_GROW = register("moving_flesh.grow");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOVING_FLESH_PRIMITIVE = register("moving_flesh.primitive");
    public static final DeferredHolder<SoundEvent, SoundEvent> MARAUDER_LIVING = register("marauder.living");
    public static final DeferredHolder<SoundEvent, SoundEvent> MARAUDER_HURT = register("marauder.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MARAUDER_DEATH = register("marauder.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCENT_WAVE = register("scent.wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCENT_MUSIC = register("scent.music");
    public static final DeferredHolder<SoundEvent, SoundEvent> RATHOL_BOOM = register("rathol.boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> NADE_IGNITE = register("nade.s");
    public static final DeferredHolder<SoundEvent, SoundEvent> DORPA_RANGE = register("dorpa.range");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARRIER_COLONY_LIVING = register("vesta.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARRIER_COLONY_HURT = register("vesta.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARRIER_COLONY_DEATH = register("vesta.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_MULTIPLE_STEP = register("step.heavy_multiple");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_EXPLOSION = register("mob.explotion");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORB_START = register("orb.s");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
