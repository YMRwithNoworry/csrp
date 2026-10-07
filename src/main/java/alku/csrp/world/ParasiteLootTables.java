package alku.csrp.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Original SRPConfigMobs per-mob loot tables (out109 {@code util/config/SRPConfigMobs.java},
 * the arrays referenced by {@code SRPEventHandlerBus#setLoot}'s switch on
 * {@code EntityParasiteBase#getParasiteIDRegister()}).
 *
 * <p>Direct transcription of the 1.10.9 defaults, keyed by the registered entity path.
 * {@link alku.csrp.event.ParasiteLootEvents} reproduces the original
 * {@code loot(LivingDropsEvent, String[])} algorithm over these entries.
 */
public final class ParasiteLootTables {
    /** One {@code item;chance;count;always} row. {@code count} is the inclusive maximum. */
    public record Entry(String item, int chance, int count, boolean always) {
    }

    private static final Map<String, List<Entry>> BY_ENTITY = new HashMap<>();

    private ParasiteLootTables() {
    }

    public static List<Entry> forEntity(String path) {
        return BY_ENTITY.get(path);
    }

    private static void put(String entity, Entry... entries) {
        BY_ENTITY.put(entity, List.of(entries));
    }

    private static Entry e(String item, int chance, int count, boolean always) {
        return new Entry(item, chance, count, always);
    }

    static {
        put("abo_head", e("srparasites:lurecomponent1", 60, 2, true));
        put("ada_arachnida", e("srparasites:lurecomponent4", 60, 3, false), e("srparasites:ada_arachnida_drop", 80, 3, true), e("srparasites:bone", 20, 2, true));
        put("ada_bolster", e("srparasites:lurecomponent4", 60, 3, false), e("srparasites:ada_bolster_drop", 80, 1, true), e("srparasites:bone", 20, 3, true));
        put("ada_burrower", e("srparasites:lurecomponent3", 60, 2, false), e("srparasites:ada_longarms_drop", 40, 2, true), e("srparasites:bone", 10, 1, true));
        put("ada_devourer", e("srparasites:ada_devourer_drop", 40, 4, true), e("srparasites:lurecomponent4", 30, 3, false), e("srparasites:bone", 10, 2, true), e("srpmeshi:devourercala", 50, 1, true));
        put("ada_longarms", e("srparasites:ada_longarms_drop", 60, 2, true), e("srparasites:lurecomponent4", 80, 1, false), e("srparasites:bone", 20, 2, true));
        put("ada_manducater", e("srparasites:ada_manducater_drop", 60, 1, false), e("srparasites:lurecomponent4", 80, 1, false), e("srparasites:bone", 20, 2, true));
        put("ada_reeker", e("srparasites:ada_reeker_drop", 60, 3, false), e("srparasites:lurecomponent4", 80, 1, false), e("srparasites:bone", 20, 2, true));
        put("ada_summoner", e("srparasites:ada_summoner_drop", 80, 1, false), e("srparasites:lurecomponent4", 60, 1, false), e("srparasites:bone", 20, 2, true));
        put("ada_tozoon", e("srparasites:lurecomponent3", 60, 2, false), e("srparasites:ada_longarms_drop", 40, 2, true), e("srparasites:bone", 10, 1, true));
        put("ada_yelloweye", e("srparasites:ada_yelloweye_drop", 80, 1, true), e("srparasites:lurecomponent4", 80, 1, false), e("srparasites:bone", 20, 1, true));
        put("anc_dreadnaut");
        put("anc_overlord");
        put("anc_pod");
        put("beckon_si", e("srparasites:beckon_drop", 20, 3, true));
        put("beckon_sii", e("srparasites:beckon_drop", 60, 7, true));
        put("beckon_siii", e("srparasites:beckon_drop", 80, 10, true));
        put("beckon_siv", e("srparasites:beckon_drop", 90, 30, true));
        put("bogle", e("srparasites:lurecomponent5", 70, 1, false), e("srparasites:hive_scrap", 80, 7, false));
        put("bomber_heavy", e("srparasites:lurecomponent5", 80, 1, false), e("srparasites:hive_scrap", 80, 7, false));
        put("bomber_light", e("srparasites:lurecomponent5", 80, 1, false));
        put("buglin", e("srpmeshi:raw_buglin", 50, 1, true));
        put("carrier_colony", e("srparasites:lurecomponent5", 80, 1, false), e("srparasites:hive_scrap", 80, 7, false));
        put("carrier_flying");
        put("carrier_heavy");
        put("crux");
        put("dispatcher_si", e("srparasites:dispatcher_drop", 20, 7, true));
        put("dispatcher_sii", e("srparasites:dispatcher_drop", 65, 15, true));
        put("dispatcher_siii", e("srparasites:dispatcher_drop", 90, 30, true));
        put("dispatcher_siv", e("srparasites:dispatcher_drop", 100, 70, true));
        put("dispatcherten");
        put("draconite", e("srparasites:false_apple", 100, 1, true), e("srparasites:trophy_boom_orb", 100, 1, true));
        put("fer_bear", e("srparasites:fishlin", 20, 3, true));
        put("fer_cow", e("srpmeshi:vilebeefr", 40, 1, true));
        put("fer_enderman");
        put("fer_horse");
        put("fer_human");
        put("fer_pig");
        put("fer_sheep");
        put("fer_villager");
        put("fer_wolf");
        put("gnat", e("srparasites:lurecomponent1", 30, 2, true), e("srparasites:hijacked_drop", 30, 2, true));
        put("grunt", e("srparasites:lurecomponent5", 70, 1, false));
        put("haunter", e("srparasites:lurecomponent5", 80, 1, false), e("srparasites:hive_scrap", 80, 7, false));
        put("heed");
        put("hi_blaze", e("srparasites:bloody_rod", 20, 3, true));
        put("hi_golem", e("srparasites:hijacked_drop", 20, 1, true), e("srparasites:bloody_iron_ingot", 20, 3, true));
        put("hi_skeleton", e("srparasites:bloody_bone", 20, 3, true));
        put("host", e("srparasites:assimilated_flesh", 80, 7, false), e("srparasites:lurecomponent2", 10, 4, true));
        put("hostii");
        put("incompleteform_medium");
        put("incompleteform_small");
        put("kirin", e("srparasites:trophy_void_orb", 100, 1, true));
        put("kyphosis");
        put("lice", e("srparasites:lurecomponent1", 30, 2, true), e("srparasites:hijacked_drop", 30, 2, true));
        put("mangler", e("srparasites:lurecomponent1", 60, 2, true));
        put("mar_bear", e("srparasites:fishlin", 20, 3, true));
        put("mar_cow");
        put("mar_enderman");
        put("mar_human");
        put("mar_sheep");
        put("mar_villager", e("srparasites:false_apple", 10, 1, true));
        put("marauder", e("srparasites:lurecomponent5", 80, 1, false));
        put("movingflesh");
        put("overseer", e("srparasites:lurecomponent5", 80, 1, false));
        put("pri_arachnida", e("srparasites:lurecomponent3", 40, 1, false), e("srparasites:ada_arachnida_drop", 40, 2, true), e("srparasites:bone", 10, 1, true));
        put("pri_bolster", e("srparasites:lurecomponent3", 40, 2, false), e("srparasites:ada_bolster_drop", 40, 1, true), e("srparasites:bone", 10, 1, true));
        put("pri_devourer", e("srparasites:ada_devourer_drop", 40, 2, true), e("srparasites:lurecomponent3", 40, 2, false), e("srparasites:bone", 10, 1, true), e("srpmeshi:devourercala", 30, 1, true));
        put("pri_longarms", e("srparasites:lurecomponent3", 60, 2, false), e("srparasites:ada_longarms_drop", 40, 2, true), e("srparasites:bone", 10, 1, true));
        put("pri_manducater", e("srparasites:lurecomponent3", 40, 2, false), e("srparasites:ada_manducater_drop", 40, 1, false), e("srparasites:bone", 10, 1, true));
        put("pri_reeker", e("srparasites:lurecomponent3", 40, 2, false), e("srparasites:ada_reeker_drop", 40, 1, false), e("srparasites:bone", 10, 1, true));
        put("pri_summoner", e("srparasites:lurecomponent3", 60, 2, false), e("srparasites:ada_summoner_drop", 40, 1, false), e("srparasites:bone", 10, 1, true));
        put("pri_tozoon", e("srparasites:lurecomponent3", 60, 2, false), e("srparasites:ada_longarms_drop", 40, 2, true), e("srparasites:bone", 10, 1, true));
        put("pri_yelloweye", e("srparasites:lurecomponent3", 60, 2, false), e("srparasites:ada_yelloweye_drop", 40, 1, true), e("srparasites:bone", 4, 1, true));
        put("rupter", e("srparasites:lurecomponent1", 70, 2, true));
        put("seeker", e("srparasites:lurecomponent5", 80, 1, false));
        put("seizer");
        put("sentry");
        put("sim_adventurer", e("srparasites:assimilated_flesh", 40, 1, false), e("srparasites:lurecomponent2", 5, 1, true));
        put("sim_adventurerhead");
        put("sim_bear", e("srparasites:assimilated_flesh", 80, 1, false), e("srparasites:lurecomponent2", 10, 1, true), e("srparasites:fishlin", 20, 3, true));
        put("sim_bigspider");
        put("sim_cow", e("srparasites:assimilated_flesh", 80, 2, false), e("srparasites:lurecomponent2", 10, 1, true), e("srpmeshi:vilebeefr", 30, 1, true));
        put("sim_cowhead");
        put("sim_dragone");
        put("sim_dragonehead");
        put("sim_enderman", e("srparasites:pearl", 10, 1, false));
        put("sim_endermanhead");
        put("sim_horse", e("srparasites:assimilated_flesh", 80, 2, false), e("srparasites:lurecomponent2", 10, 1, true));
        put("sim_horsehead");
        put("sim_human", e("srparasites:assimilated_flesh", 80, 1, false), e("srparasites:lurecomponent2", 10, 1, true));
        put("sim_humanhead");
        put("sim_pig", e("srparasites:assimilated_flesh", 80, 2, false), e("srparasites:lurecomponent2", 10, 1, true));
        put("sim_pighead");
        put("sim_sheep", e("srparasites:assimilated_flesh", 80, 1, false), e("srparasites:lurecomponent2", 10, 1, true));
        put("sim_sheephead");
        put("sim_villager", e("srparasites:assimilated_flesh", 80, 1, false), e("srparasites:lurecomponent2", 10, 1, true), e("srparasites:false_apple", 5, 1, true));
        put("sim_villagerhead");
        put("sim_wolf", e("srparasites:assimilated_flesh", 20, 1, false), e("srparasites:lurecomponent2", 10, 1, true));
        put("sim_wolfhead");
        put("thrall", e("srparasites:assimilated_flesh", 80, 1, false), e("srparasites:lurecomponent3", 10, 1, true), e("srparasites:bone", 10, 1, true));
        put("vigilante", e("srparasites:lurecomponent5", 80, 1, false));
        put("warden", e("srparasites:lurecomponent5", 80, 1, false));
        put("worker");
        put("wraith", e("srparasites:lurecomponent5", 80, 1, false), e("srparasites:hive_scrap", 80, 7, false));
    }
}
