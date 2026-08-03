package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Maps ported entity IDs to the ambient, hurt, and death events used by SRP 1.10.7. */
final class ParasiteSoundProfiles {
    private static final Map<String, Profile> PROFILES = new HashMap<>();

    static {
        register("infectedhuman", "sim_human", "sim_villager", "mar_human", "mar_sheep", "mar_villager",
                "fer_human", "fer_villager");
        register("assimadventurer", "sim_adventurer");
        register("infectedhead", "sim_adventurerhead", "sim_endermanhead", "sim_horsehead", "sim_humanhead",
                "sim_cowhead", "sim_pighead", "sim_sheephead", "sim_villagerhead", "sim_wolfhead");
        register("infectedpig", "sim_pig", "fer_pig");
        register("infectedcow", "sim_cow", "mar_cow", "fer_cow");
        register("infectedhorse", "sim_horse", "fer_horse");
        register("infectedsheep", "sim_sheep", "fer_sheep");
        register("infectedspider", "sim_bigspider");
        register("infectedbear", "sim_bear", "mar_bear", "fer_bear");
        register("infectedwolf", "sim_wolf", "fer_wolf");
        register("infectedsquid", "sim_squid");
        register("infectedenderman", "sim_enderman", "fer_enderman");
        register("assenderman", "mar_enderman");
        register("hiblaze", "hi_blaze");
        register("higolem", "hi_golem");

        register("carrier", "carrier_heavy", "carrier_light", "carrier_flying");
        register("lodo", "buglin");
        register("mudo", "rupter");
        register("flesh", "movingflesh", "crux_incomplete");
        register("nuuh", "mangler");
        register("inhoos", "incompleteform_small");
        register("inhoom", "incompleteform_medium");
        register("host", "host", "hostii");
        register("crux", "crux");
        register("mes", "thrall");
        register("done", "dredge");

        register("shyco", "pri_longarms");
        register("hull", "pri_manducater");
        register("nogla", "pri_reeker");
        register("emana", "pri_yelloweye");
        register("canra", "pri_summoner");
        register("zetmo", "pri_bolster");
        register("lum", "pri_devourer");
        register("iki", "pri_vermin");
        register("gim", "pri_viscera");
        register("ashyco", "ada_longarms");
        register("ahull", "ada_manducater");
        register("anogla", "ada_reeker");
        register("aemana", "ada_yelloweye");
        register("acanra", "ada_summoner");
        register("azetmo", "ada_bolster");
        register("awymo", "ada_tozoon");
        register("aranrac", "ada_arachnida");
        register("aiki", "ada_vermin");
        register("agim", "ada_viscera");

        register("tonro", "kyphosis");
        register("unvo", "sentry");
        register("alafha", "overseer", "architect");
        register("anged", "vigilante");
        register("ganro", "warden");
        register("omboo", "bomber_light");
        register("esor", "marauder");
        register("monarch", "monarch");
        register("jinjo", "bomber_heavy");
        register("elvia", "wraith");
        register("lencia", "bogle");
        register("vesta", "carrier_colony");
        register("oronco", "anc_dreadnaut");
        register("kirin", "kirin");
        register("heblu", "draconite");
        register("bodies", "abo_bodies");
        register("venkrolsi", "beckon_si");
        register("venkrolsii", "beckon_sii");
        register("venkrolsiii", "beckon_siii");
    }

    private ParasiteSoundProfiles() {
    }

    @Nullable
    static SoundEvent ambient(Entity entity) {
        Profile profile = profile(entity);
        return profile == null ? null : ModSounds.get(profile.ambient());
    }

    @Nullable
    static SoundEvent hurt(Entity entity) {
        Profile profile = profile(entity);
        return profile == null ? null : ModSounds.get(profile.hurt());
    }

    @Nullable
    static SoundEvent death(Entity entity) {
        Profile profile = profile(entity);
        return profile == null ? null : ModSounds.get(profile.death());
    }

    @Nullable
    private static Profile profile(Entity entity) {
        return PROFILES.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
    }

    private static void register(String prefix, String... entityIds) {
        Profile profile = new Profile(prefix + ".growl", prefix + ".hurt", prefix + ".death");
        for (String entityId : entityIds) {
            PROFILES.put(entityId, profile);
        }
    }

    private record Profile(String ambient, String hurt, String death) {
    }
}
