const fs = require("fs");
const path = require("path");
const { all } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const animationKeys = new Set();
const shortKeys = new Set([
  "abo_head", "marauder_tendril",
  "inf_sheep", "inf_sheep_head", "inf_villager"
]);

const currentExpected = {
  buglin: ["func_78087_a.age_in_ticks", "get_floor_timer"],
  gnat: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_10"],
  rupter: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_10"],
  carrier_flying: ["func_78087_a.age_in_ticks"],
  carrier_heavy: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"],
  carrier_light: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"],
  crux: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing", "get_attack_timer_m", "get_attack_timer_r",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "get_attack_timer_m.get_parasite_status_1", "get_attack_timer_r.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "get_attack_timer_m.get_parasite_status_1.get_still_ani_1",
    "get_attack_timer_r.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  pri_longarms: ["func_78087_a.limb_swing", "get_attack_timer",
    "func_78087_a.age_in_ticks.get_still_ani_1", "get_attack_timer.get_still_ani_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "get_attack_timer.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "get_attack_timer.get_parasite_status_2"],
  pri_summoner: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_10"],
  pri_vermin: ["func_78087_a.age_in_ticks"],
  pri_viscera: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  pri_arachnida: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_3"],
  pri_bolster: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"],
  pri_burrower: ["func_78087_a.age_in_ticks", "get_dig_model.get_digging_1",
    "func_78087_a.age_in_ticks.get_body_number_0_5", "get_dig_model.get_body_number_0_5.get_digging_1"],
  pri_devourer: ["func_78087_a.age_in_ticks", "func_78087_a.age_in_ticks.get_parasite_status_1"],
  pri_manducater: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  pri_reeker: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_3",
    "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1"],
  pri_tozoon: ["func_78087_a.age_in_ticks", "get_attack_timer.get_body_number_neg_0_3",
    "get_dig_model.get_body_number_neg_0_3.get_digging_1", "get_attack_timer", "get_dig_model",
    "get_dig_model.get_digging_1", "func_78087_a.age_in_ticks.get_body_number_1",
    "get_attack_timer.get_body_number_1", "get_dig_model.get_body_number_1.get_digging_1"],
  pri_yelloweye: ["func_78087_a.age_in_ticks"]
};

const adaptedExpected = {
  ada_arachnida: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_3"],
  ada_bolster: ["func_78087_a.age_in_ticks", "get_attack_timer",
    "func_78087_a.age_in_ticks.get_parasite_status_3", "get_attack_timer.get_parasite_status_15",
    "func_78087_a.age_in_ticks.get_parasite_status_25", "get_attack_timer.get_parasite_status_25"],
  ada_burrower: ["func_78087_a.age_in_ticks", "get_dig_model.get_digging_1",
    "func_78087_a.age_in_ticks.get_body_number_0_2", "get_dig_model.get_body_number_0_2.get_digging_1"],
  ada_devourer: ["func_78087_a.age_in_ticks", "func_78087_a.age_in_ticks.get_parasite_status_1"],
  ada_longarms: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing", "get_attack_timer",
    "func_78087_a.age_in_ticks.get_still_ani_1", "get_attack_timer.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "get_attack_timer.get_parasite_status_1", "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "get_attack_timer.get_parasite_status_1.get_still_ani_1", "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_25", "get_attack_timer.get_parasite_status_25"],
  ada_manducater: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  ada_reeker: ["func_78087_a.limb_swing", "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_3",
    "func_78087_a.limb_swing.get_parasite_status_3", "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_25"],
  ada_summoner: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_10",
    "func_78087_a.age_in_ticks.get_parasite_status_25"],
  ada_tozoon: ["func_78087_a.age_in_ticks", "get_attack_timer.get_body_number_neg_0_1",
    "get_dig_model.get_body_number_neg_0_1.get_digging_1", "get_attack_timer", "get_dig_model",
    "get_dig_model.get_digging_1", "func_78087_a.age_in_ticks.get_body_number_1",
    "get_attack_timer.get_body_number_1", "get_dig_model.get_body_number_1.get_digging_1",
    "func_78087_a.age_in_ticks.get_body_number_2", "get_attack_timer.get_body_number_2",
    "get_dig_model.get_body_number_2.get_digging_1", "func_78087_a.age_in_ticks.get_body_number_3",
    "get_attack_timer.get_body_number_3", "get_dig_model.get_body_number_3.get_digging_1"],
  ada_vermin: ["func_78087_a.age_in_ticks"],
  ada_viscera: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  ada_yelloweye: ["func_78087_a.age_in_ticks", "func_78087_a.age_in_ticks.get_parasite_status_1"]
};

const assimilatedExpected = {
  sim_adventurer: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing", "helmet_slot",
    "func_78087_a.age_in_ticks.get_still_ani_1", "helmet_slot.get_still_ani_1"],
  sim_bear: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1"],
  sim_bigspider: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1"],
  sim_cow: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_3",
    "func_78087_a.limb_swing.get_parasite_status_3",
    "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_6", "get_theigh.get_parasite_status_6"],
  sim_cowhead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  sim_dragone: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_10",
    "func_78087_a.age_in_ticks.get_flying_state_1", "getaaa.get_flying_state_1"],
  sim_dragonhead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"],
  sim_enderman: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.is_screaming_1", "func_78087_a.limb_swing.is_screaming_1",
    "func_78087_a.age_in_ticks.is_crawling_1", "func_78087_a.limb_swing.is_crawling_1",
    "func_78087_a.age_in_ticks.is_crawling_1.is_screaming_1",
    "func_78087_a.limb_swing.is_crawling_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_still_ani_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1",
    "func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.is_crawling_1",
    "func_78087_a.limb_swing.get_parasite_status_1.is_crawling_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.is_crawling_1.is_screaming_1",
    "func_78087_a.limb_swing.get_parasite_status_1.is_crawling_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2.is_crawling_1",
    "func_78087_a.limb_swing.get_parasite_status_2.is_crawling_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2.is_crawling_1.is_screaming_1",
    "func_78087_a.limb_swing.get_parasite_status_2.is_crawling_1.is_screaming_1"],
  sim_endermanhead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.is_screaming_1", "func_78087_a.limb_swing.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1",
    "func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10",
    "func_78087_a.age_in_ticks.get_parasite_status_10.is_screaming_1"],
  sim_horse: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.limb_swing.get_parasite_status_3"],
  sim_horsehead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  sim_human: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  sim_humanhead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  sim_pig: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_6", "get_theigh.get_parasite_status_6"],
  sim_pighead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  sim_sheep: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_6", "get_theigh.get_parasite_status_6"],
  sim_sheephead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  sim_squid: ["func_78087_a.age_in_ticks"],
  sim_villager: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1"],
  sim_villagerhead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  sim_wolf: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_6", "get_theigh.get_parasite_status_6"],
  sim_wolfhead: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  sim_adventurerhead: ["func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_10"]
};

const hijackedAndFeralExpected = {
  hi_blaze: ["func_78087_a.age_in_ticks"],
  hi_golem: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_3", "func_78087_a.limb_swing.get_parasite_status_3"],
  hi_skeleton: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  fer_bear: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  fer_cow: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_3",
    "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1"],
  fer_enderman: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.is_screaming_1", "func_78087_a.limb_swing.is_screaming_1",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_still_ani_1.is_screaming_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.limb_swing.get_parasite_status_2.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1.is_screaming_1"],
  fer_horse: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_3"],
  fer_human: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  fer_pig: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  fer_sheep: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  fer_villager: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"],
  fer_wolf: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_2"]
};

const marauderizedExpected = {
  mar_bear: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_3", "func_78087_a.limb_swing.get_parasite_status_3"],
  mar_cow: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_3",
    "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1"],
  mar_enderman: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.is_screaming_1", "func_78087_a.limb_swing.is_screaming_1",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_still_ani_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1",
    "func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1.is_screaming_1"],
  mar_human: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_3"],
  mar_sheep: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_2"],
  mar_villager: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1"],
  marauder: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing", "get_attack_timer",
    "func_78087_a.age_in_ticks.get_still_ani_1", "get_attack_timer.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "get_attack_timer.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "get_attack_timer.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_2", "func_78087_a.limb_swing.get_parasite_status_2",
    "get_attack_timer.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1",
    "get_attack_timer.get_parasite_status_2.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_3",
    "func_78087_a.age_in_ticks.get_parasite_status_4", "get_attack_timer.get_parasite_status_4",
    "func_78087_a.age_in_ticks.get_parasite_status_10", "get_attack_timer.get_parasite_status_10",
    "func_78087_a.age_in_ticks.get_parasite_status_25", "get_attack_timer.get_parasite_status_25"]
};

const pureExpected = {
  dispatcherten: ["func_78087_a.age_in_ticks", "get_floor_timer"],
  kyphosis: ["func_78087_a.age_in_ticks", "get_attack_timer", "get_floor_timer",
    "func_78087_a.age_in_ticks.get_parasite_status_3", "get_attack_timer.get_parasite_status_3",
    "get_floor_timer.get_parasite_status_3"],
  seizer: ["func_78087_a.age_in_ticks", "get_floor_timer",
    "func_78087_a.age_in_ticks.get_targeted_entity_1", "get_floor_timer.get_targeted_entity_1"],
  sentry: ["func_78087_a.age_in_ticks", "get_floor_timer",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "get_floor_timer.get_parasite_status_1",
    "get_floor_timer.get_parasite_status_3"],
  worm: ["func_78087_a.age_in_ticks", "get_attack_timer", "get_floor_timer",
    "func_78087_a.age_in_ticks.get_parasite_status_3", "get_attack_timer.get_parasite_status_3",
    "get_floor_timer.get_parasite_status_3"],
  grunt: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2", "func_78087_a.age_in_ticks.get_parasite_status_10"],
  bomber_light: ["func_78087_a.age_in_ticks"],
  monarch: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_10"],
  overseer: ["func_78087_a.age_in_ticks"],
  vigilante: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_25"],
  warden: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing", "get_attack_timer",
    "func_78087_a.age_in_ticks.get_still_ani_1", "get_attack_timer.get_still_ani_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1", "func_78087_a.limb_swing.get_parasite_status_1",
    "get_attack_timer.get_parasite_status_1", "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "get_attack_timer.get_parasite_status_1.get_still_ani_1", "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_3", "func_78087_a.limb_swing.get_parasite_status_3",
    "get_attack_timer.get_parasite_status_3", "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1",
    "get_attack_timer.get_parasite_status_3.get_still_ani_1", "func_78087_a.age_in_ticks.get_parasite_status_10",
    "get_attack_timer.get_parasite_status_10"],
  bogle: ["func_78087_a.age_in_ticks", "func_78087_a.age_in_ticks.get_parasite_status_1"],
  carrier_colony: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"],
  haunter: ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"],
  bomber_heavy: ["func_78087_a.age_in_ticks"],
  wraith: ["func_78087_a.age_in_ticks", "func_78087_a.age_in_ticks.get_parasite_status_1"],
  succor: ["func_78087_a.age_in_ticks"]
};

function resolvedAnimationKey(id, requestedAction) {
  const resourceId = id === "sim_dragonhead" ? "sim_dragonehead"
    : id === "dispatcher_tentacle" ? "dispatcherten" : id;
  let action = ["run", "fly"].includes(requestedAction) ? "walk"
    : ["spawn", "throw", "smash", "swipe", "melee_attack", "ranged_attack", "burst"].includes(requestedAction)
      ? "attack"
      : requestedAction === "func_78087_a.getDigging" ? "get_dig_model.get_digging_1"
        : requestedAction === "animation" ? "idle" : requestedAction;

  if (action === "attack" && shortKeys.has(resourceId) && resourceId !== "abo_head") {
    return "walk";
  }
  if (action === "attack") {
    action = {
      pri_arachnida: "walk.get_parasite_status_2",
      pri_manducater: "idle.get_parasite_status_1",
      pri_reeker: "idle.get_parasite_status_1",
      sim_dragone: "walk.get_parasite_status_2",
      dispatcher_sii: "idle"
    }[resourceId] || action;
  }
  if (resourceId === "pri_summoner" && action === "summon") action = "run";
  else if (resourceId === "sim_cow" && action === "idle.get_parasite_status_3.get_still_ani_1") action = "idle";
  else if (resourceId === "sim_cow" && action === "walk.get_parasite_status_3") action = "run";
  else if (resourceId === "ada_arachnida" && action === "idle.get_parasite_status_11") action = "idle.get_parasite_status_3";
  else if (resourceId === "ada_summoner" && action === "idle.get_parasite_status_100") action = "idle.get_parasite_status_25";
  else if (resourceId === "ada_manducater" && action === "idle.get_parasite_status_10") action = "idle.get_parasite_status_3";
  else if (resourceId === "ada_manducater" && action === "idle.get_parasite_status_25") action = "walk.get_parasite_status_2";
  return shortKeys.has(resourceId) ? action : `animation.${resourceId}.${action}`;
}

for (const id of all) {
  const resourceId = id === "sim_dragonhead" ? "sim_dragonehead" : id;
  const file = `src/main/resources/assets/csrp/animations/${resourceId}.animation.json`;
  const animations = JSON.parse(read(file)).animations;
  Object.keys(animations).forEach((key) => animationKeys.add(key));

  const actionAliases = {
    pri_arachnida: "walk.get_parasite_status_2",
    pri_manducater: "idle.get_parasite_status_1",
    pri_reeker: "idle.get_parasite_status_1",
    sim_dragone: "walk.get_parasite_status_2",
    dispatcher_sii: "idle"
  };
  const baseActions = id === "dispatcher_sii" ? ["idle", "idle", "idle"] : ["idle", "walk", actionAliases[id] || "attack"];
  const expectedActions = currentExpected[id] || adaptedExpected[id]
    || assimilatedExpected[id] || hijackedAndFeralExpected[id] || marauderizedExpected[id] || pureExpected[id];
  const expected = expectedActions
    ? expectedActions.map((action) => `animation.${resourceId}.${action}`)
    : id === "crux_incomplete"
    ? ["animation.crux_incomplete.func_78087_a.age_in_ticks", "animation.crux_incomplete.func_78087_a.limb_swing"]
    : id === "sim_cow"
      ? ["animation.sim_cow.func_78087_a.age_in_ticks", "animation.sim_cow.func_78087_a.limb_swing",
        "animation.sim_cow.func_78087_a.age_in_ticks.get_parasite_status_1",
        "animation.sim_cow.func_78087_a.limb_swing.get_parasite_status_1",
        "animation.sim_cow.func_78087_a.limb_swing.get_parasite_status_2",
        "animation.sim_cow.func_78087_a.age_in_ticks.get_parasite_status_3",
        "animation.sim_cow.func_78087_a.limb_swing.get_parasite_status_3",
        "animation.sim_cow.func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1",
        "animation.sim_cow.func_78087_a.age_in_ticks.get_parasite_status_6",
        "animation.sim_cow.get_theigh.get_parasite_status_6"]
      : id === "sim_pig"
        ? ["animation.sim_pig.func_78087_a.age_in_ticks", "animation.sim_pig.func_78087_a.limb_swing",
          "animation.sim_pig.func_78087_a.age_in_ticks.get_parasite_status_1",
          "animation.sim_pig.func_78087_a.limb_swing.get_parasite_status_1",
          "animation.sim_pig.func_78087_a.limb_swing.get_parasite_status_2",
          "animation.sim_pig.func_78087_a.age_in_ticks.get_parasite_status_6",
          "animation.sim_pig.get_theigh.get_parasite_status_6"]
      : id === "sim_endermanhead"
        ? ["animation.sim_endermanhead.func_78087_a.age_in_ticks",
          "animation.sim_endermanhead.func_78087_a.limb_swing",
          "animation.sim_endermanhead.func_78087_a.age_in_ticks.is_screaming_1",
          "animation.sim_endermanhead.func_78087_a.limb_swing.is_screaming_1",
          "animation.sim_endermanhead.func_78087_a.age_in_ticks.get_parasite_status_1",
          "animation.sim_endermanhead.func_78087_a.limb_swing.get_parasite_status_1",
          "animation.sim_endermanhead.func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1",
          "animation.sim_endermanhead.func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1",
          "animation.sim_endermanhead.func_78087_a.age_in_ticks.get_parasite_status_10",
          "animation.sim_endermanhead.func_78087_a.age_in_ticks.get_parasite_status_10.is_screaming_1"]
      : id === "sim_adventurerhead"
        ? ["animation.sim_adventurerhead.func_78087_a.limb_swing",
          "animation.sim_adventurerhead.func_78087_a.limb_swing.get_parasite_status_1",
          "animation.sim_adventurerhead.func_78087_a.age_in_ticks.get_parasite_status_10"]
      : id === "airscrew"
        ? ["animation.airscrew.func_78087_a.age_in_ticks"]
      : id === "heed"
        ? ["animation.heed.func_78087_a.age_in_ticks", "animation.heed.func_78087_a.limb_swing",
          "animation.heed.func_78087_a.age_in_ticks.get_parasite_status_1",
          "animation.heed.func_78087_a.limb_swing.get_parasite_status_1"]
      : id === "dredge"
        ? ["animation.dredge.func_78087_a.age_in_ticks", "animation.dredge.func_78087_a.limb_swing",
          "animation.dredge.func_78087_a.age_in_ticks.get_still_ani_1",
          "animation.dredge.func_78087_a.age_in_ticks.get_parasite_status_1",
          "animation.dredge.func_78087_a.limb_swing.get_parasite_status_1",
          "animation.dredge.func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
          "animation.dredge.func_78087_a.age_in_ticks.get_parasite_status_2",
          "animation.dredge.func_78087_a.limb_swing.get_parasite_status_2",
          "animation.dredge.func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1"]
      : id === "thrall"
        ? ["animation.thrall.func_78087_a.age_in_ticks", "animation.thrall.func_78087_a.limb_swing"]
      : id === "lice"
        ? ["animation.lice.func_78087_a.age_in_ticks"]
      : id === "mangler"
        ? ["animation.mangler.func_78087_a.age_in_ticks", "animation.mangler.func_78087_a.limb_swing",
          "animation.mangler.func_78087_a.age_in_ticks.get_parasite_status_1",
          "animation.mangler.func_78087_a.limb_swing.get_parasite_status_1",
          "animation.mangler.func_78087_a.limb_swing.get_parasite_status_2",
          "animation.mangler.func_78087_a.age_in_ticks.get_parasite_status_10"]
      : id === "host"
        ? ["animation.host.func_78087_a.age_in_ticks", "animation.host.get_attack_timer",
          "animation.host.get_burrow_timer", "animation.host.func_78087_a.age_in_ticks.get_open_1",
          "animation.host.get_attack_timer.get_open_1", "animation.host.get_burrow_timer.get_open_1",
          "animation.host.func_78087_a.age_in_ticks.get_burrowed_1",
          "animation.host.get_attack_timer.get_burrowed_1", "animation.host.get_burrow_timer.get_burrowed_1",
          "animation.host.func_78087_a.age_in_ticks.get_burrowed_1.get_open_1",
          "animation.host.get_attack_timer.get_burrowed_1.get_open_1",
          "animation.host.get_burrow_timer.get_burrowed_1.get_open_1"]
      : id === "hostii"
        ? ["animation.hostii.func_78087_a.age_in_ticks", "animation.hostii.get_burrow_timer",
          "animation.hostii.func_78087_a.age_in_ticks.get_open_1",
          "animation.hostii.get_burrow_timer.get_open_1",
          "animation.hostii.func_78087_a.age_in_ticks.get_burrowed_1",
          "animation.hostii.get_burrow_timer.get_burrowed_1",
          "animation.hostii.func_78087_a.age_in_ticks.get_burrowed_1.get_open_1",
          "animation.hostii.get_burrow_timer.get_burrowed_1.get_open_1"]
      : id === "draconite"
        ? ["animation.draconite.func_78087_a.age_in_ticks", "animation.draconite.func_78087_a.limb_swing",
          "animation.draconite.func_78087_a.age_in_ticks.shaking_c_1",
          "animation.draconite.func_78087_a.limb_swing.shaking_c_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_parasite_status_1",
          "animation.draconite.func_78087_a.limb_swing.get_parasite_status_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_parasite_status_1.shaking_c_1",
          "animation.draconite.func_78087_a.limb_swing.get_parasite_status_1.shaking_c_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_parasite_status_10",
          "animation.draconite.func_78087_a.age_in_ticks.get_parasite_status_10.shaking_c_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_flying_state_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_clone_c_1",
          "animation.draconite.func_78087_a.limb_swing.get_clone_c_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_clone_c_1.shaking_c_1",
          "animation.draconite.func_78087_a.limb_swing.get_clone_c_1.shaking_c_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_clone_c_1.get_parasite_status_1",
          "animation.draconite.func_78087_a.limb_swing.get_clone_c_1.get_parasite_status_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_clone_c_1.get_parasite_status_1.shaking_c_1",
          "animation.draconite.func_78087_a.limb_swing.get_clone_c_1.get_parasite_status_1.shaking_c_1",
          "animation.draconite.func_78087_a.age_in_ticks.get_clone_c_1.get_parasite_status_10",
          "animation.draconite.func_78087_a.age_in_ticks.get_clone_c_1.get_parasite_status_10.shaking_c_1"]
      : id === "kirin"
        ? ["animation.kirin.func_78087_a.age_in_ticks",
          "animation.kirin.func_78087_a.age_in_ticks.shaking_c_1",
          "animation.kirin.func_78087_a.age_in_ticks.get_clone_c_1",
          "animation.kirin.func_78087_a.age_in_ticks.get_clone_c_1.shaking_c_1"]
      : ["incompleteform_small", "incompleteform_medium"].includes(id)
        ? [`animation.${id}.func_78087_a.age_in_ticks`]
      : ["sim_cowhead", "sim_horsehead", "sim_humanhead", "sim_pighead", "sim_sheephead", "sim_villagerhead", "sim_wolfhead"].includes(id)
    ? ["animation." + id + ".func_78087_a.age_in_ticks",
      "animation." + id + ".func_78087_a.limb_swing",
      ...(id === "sim_horsehead" ? [] : ["animation." + id + ".func_78087_a.age_in_ticks.get_parasite_status_1"]),
      "animation." + id + ".func_78087_a.limb_swing.get_parasite_status_1",
      "animation." + id + ".func_78087_a.age_in_ticks.get_parasite_status_10"]
      : shortKeys.has(id)
    ? ["idle", "walk", id === "marauder" ? "attack" : "walk"]
    : baseActions.map((action) => `animation.${resourceId}.${action}`);
  for (const key of expected) {
    if (!(key in animations)) failures.push(`${id}: missing base animation key ${key}`);
  }
  const exactExpectedActions = assimilatedExpected[id] || hijackedAndFeralExpected[id]
    || marauderizedExpected[id];
  if (exactExpectedActions) {
    const actualKeys = Object.keys(animations).sort();
    const exactKeys = exactExpectedActions
      .map((action) => `animation.${resourceId}.${action}`).sort();
    if (actualKeys.length !== exactKeys.length
        || actualKeys.some((key, index) => key !== exactKeys[index])) {
      failures.push(`${id}: animation keys differ from the extracted original function set`);
    }
  }
}

const registrations = read("src/main/java/alku/csrp/registry/ModEntities.java");
for (const match of registrations.matchAll(/monster\("([a-z0-9_]+)",\s*(\w+)::new/g)) {
  const [, id, className] = match;
  const source = read(`src/main/java/alku/csrp/entity/${className}.java`);
  const resourceId = id === "sim_dragonhead" ? "sim_dragonehead" : id;
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${resourceId}.animation.json`)).animations;
    for (const request of source.matchAll(/ParasiteAnimations\.(?:loop|play)\(this,\s*"([a-zA-Z0-9_.]+)"/g)) {
      if (className === "AssimilatedHeadEntity"
          && ["sim_adventurerhead", "sim_cowhead", "sim_endermanhead", "sim_horsehead", "sim_humanhead", "sim_pighead", "sim_sheephead",
            "sim_villagerhead", "sim_wolfhead"].includes(id)
          && (!request[1].startsWith("func_78087_a.")
            || id === "sim_horsehead" && request[1] === "func_78087_a.age_in_ticks.get_parasite_status_1")) continue;
      const key = resolvedAnimationKey(id, request[1]);
    if (!(key in animations)) failures.push(`${id}/${className}: unresolved requested animation ${key}`);
  }
}
for (const match of registrations.matchAll(
  /ENTITIES\.register\("([a-z0-9_]+)",[\s\S]{0,400}?EntityType\.Builder(?:\.<[^>]+>)?\.of\((\w+)::new/g
)) {
  const [, id, className] = match;
  const source = read(`src/main/java/alku/csrp/entity/${className}.java`);
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${id}.animation.json`)).animations;
  for (const request of source.matchAll(/ParasiteAnimations\.(?:loop|play)\(this,\s*"([a-zA-Z0-9_.]+)"/g)) {
    const key = resolvedAnimationKey(id, request[1]);
    if (!(key in animations)) failures.push(`${id}/${className}: unresolved requested animation ${key}`);
  }
}

const sharedVariantActions = {
  pri_arachnida: currentExpected.pri_arachnida,
  pri_bolster: currentExpected.pri_bolster,
  pri_burrower: currentExpected.pri_burrower,
  pri_devourer: currentExpected.pri_devourer,
  pri_manducater: currentExpected.pri_manducater,
  pri_reeker: currentExpected.pri_reeker,
  pri_tozoon: currentExpected.pri_tozoon,
  pri_yelloweye: currentExpected.pri_yelloweye,
  ada_arachnida: adaptedExpected.ada_arachnida,
  ada_bolster: adaptedExpected.ada_bolster,
  ada_burrower: adaptedExpected.ada_burrower,
  ada_devourer: adaptedExpected.ada_devourer,
  ada_longarms: adaptedExpected.ada_longarms,
  ada_manducater: adaptedExpected.ada_manducater,
  ada_reeker: adaptedExpected.ada_reeker,
  ada_summoner: adaptedExpected.ada_summoner,
  ada_tozoon: adaptedExpected.ada_tozoon,
  ada_vermin: adaptedExpected.ada_vermin,
  ada_viscera: adaptedExpected.ada_viscera,
  ada_yelloweye: adaptedExpected.ada_yelloweye
  ,sim_bear: assimilatedExpected.sim_bear
  ,sim_cow: assimilatedExpected.sim_cow
  ,sim_pig: assimilatedExpected.sim_pig
  ,sim_cowhead: assimilatedExpected.sim_cowhead
  ,sim_horsehead: assimilatedExpected.sim_horsehead
  ,sim_endermanhead: assimilatedExpected.sim_endermanhead
  ,sim_adventurerhead: assimilatedExpected.sim_adventurerhead
  ,sim_humanhead: assimilatedExpected.sim_humanhead
  ,sim_pighead: assimilatedExpected.sim_pighead
  ,sim_sheephead: assimilatedExpected.sim_sheephead
  ,sim_villagerhead: assimilatedExpected.sim_villagerhead
  ,sim_wolfhead: assimilatedExpected.sim_wolfhead
  ,sim_sheep: assimilatedExpected.sim_sheep
  ,sim_wolf: assimilatedExpected.sim_wolf
  ,sim_squid: assimilatedExpected.sim_squid
  ,sim_bigspider: assimilatedExpected.sim_bigspider
  ,sim_horse: assimilatedExpected.sim_horse
  ,sim_villager: assimilatedExpected.sim_villager
  ,grunt: pureExpected.grunt
  ,bomber_light: pureExpected.bomber_light
  ,monarch: pureExpected.monarch
  ,overseer: pureExpected.overseer
  ,vigilante: pureExpected.vigilante
  ,warden: pureExpected.warden
  ,anc_dreadnaut: ["idle", "walk", "attack", "idle.get_parasite_status_77"]
  ,anc_overlord: ["idle", "walk", "attack"]
  ,dispatcherten: pureExpected.dispatcherten
  ,kyphosis: pureExpected.kyphosis
  ,seizer: pureExpected.seizer
  ,sentry: pureExpected.sentry
  ,worm: pureExpected.worm
};
for (const [id, actions] of Object.entries(sharedVariantActions)) {
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${id}.animation.json`)).animations;
  for (const action of actions) {
    const key = resolvedAnimationKey(id, action);
    if (!(key in animations)) failures.push(`${id}/shared variant: unresolved requested animation ${key}`);
  }
}

const entityDirectory = path.join(root, "src/main/java/alku/csrp/entity");
const entitySources = new Map();
for (const name of fs.readdirSync(entityDirectory).filter((file) => file.endsWith(".java"))) {
  const source = fs.readFileSync(path.join(entityDirectory, name), "utf8");
  entitySources.set(name.replace(/\.java$/, ""), source);
  for (const match of source.matchAll(/["'](animation\.[a-z0-9_.]+)["']/g)) {
    if (!animationKeys.has(match[1])) failures.push(`${name}: unknown animation key ${match[1]}`);
  }
}

function inheritedControllerSource(className) {
  const visited = new Set();
  let current = className;
  while (current && !visited.has(current)) {
    visited.add(current);
    const source = entitySources.get(current);
    if (!source) return null;
    if (/void\s+registerControllers\s*\(/.test(source)) return { className: current, source };
    current = source.match(/class\s+\w+(?:<[^>{}]+>)?\s+extends\s+(\w+)/)?.[1];
  }
  return null;
}

function registeredTriggers(source) {
  const triggers = new Set();
  for (const block of source.matchAll(/controllers\.add\(([\s\S]*?)\)\);/g)) {
    const controller = block[1].match(/new AnimationController<>\(this,\s*"([^"]+)"/)?.[1];
    if (!controller) continue;
    for (const trigger of block[1].matchAll(/\.triggerableAnim\("([^"]+)"/g)) {
      triggers.add(`${controller}\0${trigger[1]}`);
    }
  }
  return triggers;
}

for (const [className, source] of entitySources) {
  const calls = [...source.matchAll(/triggerAnim\("([^"]+)",\s*"([^"]+)"\)/g)];
  if (!calls.length) continue;
  const owner = inheritedControllerSource(className);
  if (!owner) {
    failures.push(`${className}: triggers animations without a registerControllers implementation`);
    continue;
  }
  const triggers = registeredTriggers(owner.source);
  for (const call of calls) {
    const contract = `${call[1]}\0${call[2]}`;
    if (!triggers.has(contract)) {
      failures.push(`${className}: ${call[1]}/${call[2]} is not registered by ${owner.className}`);
    }
  }
}

const helper = read("src/main/java/alku/csrp/entity/ParasiteAnimations.java");
if (!helper.includes('case "func_78087_a.getDigging" -> "get_dig_model.get_digging_1";')) {
  failures.push("burrower digging animation is not mapped to the extracted key");
}
if (!helper.includes("entity.getX() - entity.xo") || !helper.includes("entity.getZ() - entity.zo")) {
  failures.push("shared movement animation gate does not measure actual tick displacement");
}
if (/isMoving\([^)]*\)[\s\S]{0,120}getDeltaMovement\(\)\.lengthSqr/.test(helper)) {
  failures.push("shared movement animation gate still trusts requested velocity while blocked");
}

const primitiveVariants = read("src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java");
if (/getTarget\(\) != null \|\| ParasiteAnimations\.isMoving/.test(primitiveVariants)) {
  failures.push("Primitive devourer still walks in place merely because it has a target");
}
const primitiveIds = ["pri_arachnida", "pri_bolster", "pri_burrower", "pri_devourer",
  "pri_manducater", "pri_reeker", "pri_tozoon", "pri_yelloweye"];
for (const action of new Set(primitiveIds.flatMap((id) => currentExpected[id]))) {
  if (!primitiveVariants.includes(`"${action}"`)) {
    failures.push(`PrimitiveVariantEntity: original runtime animation ${action} is not wired`);
  }
}
if (/ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:idle|walk|run|fly|attack)"/.test(primitiveVariants)
    || primitiveVariants.includes('triggerAnim("attack_controller", "attack")')) {
  failures.push("PrimitiveVariantEntity: still requests fabricated generic primitive animations");
}
const vigile = read("src/main/java/alku/csrp/entity/VigileEntity.java");
if (!vigile.includes("!ParasiteAnimations.isMoving(this, state.isMoving())")) {
  failures.push("Vigile movement controller bypasses the actual-displacement gate");
}

const kirin = read("src/main/java/alku/csrp/entity/KirinEntity.java");
for (const action of [
  "animation.kirin.func_78087_a.age_in_ticks",
  "animation.kirin.func_78087_a.age_in_ticks.shaking_c_1",
  "animation.kirin.func_78087_a.age_in_ticks.get_clone_c_1",
  "animation.kirin.func_78087_a.age_in_ticks.get_clone_c_1.shaking_c_1"
]) {
  if (!kirin.includes(`"${action}"`)) failures.push(`KirinEntity: original runtime animation ${action} is not wired`);
}
if (kirin.includes('triggerableAnim("attack"') || kirin.includes("animation.kirin.walk")) {
  failures.push("KirinEntity: still uses a fabricated movement or attack animation");
}

const draconite = read("src/main/java/alku/csrp/entity/DraconiteEntity.java");
for (const action of [
  "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
  "func_78087_a.age_in_ticks.get_parasite_status_1",
  "func_78087_a.limb_swing.get_parasite_status_1",
  "func_78087_a.age_in_ticks.get_parasite_status_10",
  "animation.draconite.func_78087_a.age_in_ticks.get_flying_state_1",
  "func_78087_a.age_in_ticks.get_clone_c_1",
  "func_78087_a.limb_swing.get_clone_c_1"
]) {
  if (!draconite.includes(`"${action}"`)) {
    failures.push(`DraconiteEntity: original runtime animation ${action} is not wired`);
  }
}
if (draconite.includes('triggerableAnim("attack"') || draconite.includes('ParasiteAnimations.play(this, "attack"')) {
  failures.push("DraconiteEntity: still uses a fabricated attack animation");
}

const heed = read("src/main/java/alku/csrp/entity/HeedEntity.java");
if (!heed.includes("COMBAT_STATUS") || !heed.includes("builder.define(COMBAT_STATUS, false)")
    || !heed.includes("entityData.set(COMBAT_STATUS, inCombat)")) {
  failures.push("HeedEntity: original combat status 1 is not synchronized from target state");
}
for (const file of ["AirscrewEntity.java", "HeedEntity.java", "DredgeEntity.java", "ThrallEntity.java"]) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  if (source.includes('triggerableAnim("attack"') || source.includes('ParasiteAnimations.play(this, "attack"')) {
    failures.push(`${file}: still uses a fabricated attack animation absent from its extracted model`);
  }
}

const lice = read("src/main/java/alku/csrp/entity/LiceEntity.java");
if (!lice.includes('"func_78087_a.age_in_ticks"') || lice.includes('triggerableAnim("attack"')) {
  failures.push("LiceEntity: animation controller is not limited to the original age function");
}
const mangler = read("src/main/java/alku/csrp/entity/ManglerEntity.java");
for (const action of [
  "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
  "func_78087_a.age_in_ticks.get_parasite_status_1",
  "func_78087_a.limb_swing.get_parasite_status_1",
  "func_78087_a.limb_swing.get_parasite_status_2",
  "func_78087_a.age_in_ticks.get_parasite_status_10"
]) {
  if (!mangler.includes(`"${action}"`)) failures.push(`ManglerEntity: original runtime animation ${action} is not wired`);
}
if (mangler.includes('triggerableAnim("attack"')) {
  failures.push("ManglerEntity: still uses a fabricated attack animation controller");
}

const earlyLifecycleHost = read("src/main/java/alku/csrp/entity/HostEntity.java");
if (!earlyLifecycleHost.includes('"func_78087_a.age_in_ticks"') || earlyLifecycleHost.includes('"walk"')) {
  failures.push("HostEntity: base animation does not use the original age function exclusively");
}
const earlyLifecycleHostII = read("src/main/java/alku/csrp/entity/HostIIEntity.java");
if (!earlyLifecycleHostII.includes('"func_78087_a.age_in_ticks"') || earlyLifecycleHostII.includes('"walk"')
    || earlyLifecycleHostII.includes('triggerableAnim("attack"')
    || earlyLifecycleHostII.includes('ParasiteAnimations.play(this, "attack"')) {
  failures.push("HostIIEntity: still uses a fabricated movement or attack function");
}

const assimilatedDragon = read("src/main/java/alku/csrp/entity/AssimilatedDragonEntity.java");
for (const action of assimilatedExpected.sim_dragone) {
  if (!assimilatedDragon.includes(`"${action}"`)) {
    failures.push(`AssimilatedDragonEntity: original runtime animation ${action} is not wired`);
  }
}
if (assimilatedDragon.includes('triggerableAnim("attack"')
    || assimilatedDragon.includes('ParasiteAnimations.play(this, "attack"')) {
  failures.push("AssimilatedDragonEntity: still uses a fabricated generic attack animation");
}

const assimilatedEnderman = read("src/main/java/alku/csrp/entity/AssimilatedEndermanEntity.java");
const endermanFunctions = [
  ...assimilatedExpected.sim_enderman
];
for (const action of endermanFunctions) {
  if (!assimilatedEnderman.includes(`"${action}"`)) {
    failures.push(`AssimilatedEndermanEntity: original runtime animation ${action} is not wired`);
  }
}
if (assimilatedEnderman.includes('triggerableAnim("attack"')
    || assimilatedEnderman.includes('triggerableAnim("teleport"')) {
  failures.push("AssimilatedEndermanEntity: still uses a fabricated action animation controller");
}
if (assimilatedEnderman.includes("PULLING") || assimilatedEnderman.includes("pullingCounter")) {
  failures.push("AssimilatedEndermanEntity: still treats original melee statuses as a pulling state");
}
if (!assimilatedEnderman.includes("class EndermanMeleeGoal extends MeleeAttackGoal")
    || !assimilatedEnderman.includes("setParasiteStatus(2)")
    || !assimilatedEnderman.includes("setParasiteStatus(distanceToSqr(target)")) {
  failures.push("AssimilatedEndermanEntity: melee AI does not drive original parasite statuses 1 and 2");
}
if (!assimilatedEnderman.includes("stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS")) {
  failures.push("AssimilatedEndermanEntity: legacy stillAni delay is not represented");
}
if (!assimilatedEnderman.includes("Config.variantSpawnChance()")
    || !assimilatedEnderman.includes("EntityDimensions.scalable(0.95F, 1.25F)")) {
  failures.push("AssimilatedEndermanEntity: original crawling variant spawn or dimensions are missing");
}

const assimilatedHeads = read("src/main/java/alku/csrp/entity/AssimilatedHeadEntity.java");
for (const action of [
  "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
  "func_78087_a.age_in_ticks.get_parasite_status_1",
  "func_78087_a.limb_swing.get_parasite_status_1",
  "func_78087_a.age_in_ticks.get_parasite_status_10",
  "func_78087_a.age_in_ticks.is_screaming_1",
  "func_78087_a.limb_swing.is_screaming_1",
  "func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1",
  "func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1",
  "func_78087_a.age_in_ticks.get_parasite_status_10.is_screaming_1"
]) {
  if (!assimilatedHeads.includes(`"${action}"`)) {
    failures.push(`AssimilatedHeadEntity: original head function ${action} is not wired`);
  }
}
if (!assimilatedHeads.includes('"age_controller"')
    || !assimilatedHeads.includes("entityData.set(SCREAMING, target != null)")) {
  failures.push("AssimilatedHeadEntity: original function controllers or enderman screaming state are missing");
}
if (!assimilatedHeads.includes("class HeadMeleeGoal extends MeleeAttackGoal")
    || !assimilatedHeads.includes("setParasiteStatus(1)")
    || !assimilatedHeads.includes("setParasiteStatus(10)")) {
  failures.push("AssimilatedHeadEntity: melee and leap AI do not drive original statuses 1 and 10");
}
if (!assimilatedHeads.includes("ParasiteAnimations.isMoving(this, state.isMoving())")
    || !assimilatedHeads.includes("remainingTicks <= 22 && onGround()")) {
  failures.push("AssimilatedHeadEntity: original still-animation gate or leap landing reset is missing");
}
if (assimilatedHeads.includes('triggerableAnim("attack"') || assimilatedHeads.includes("triggerAnim(")) {
  failures.push("AssimilatedHeadEntity: shared heads still use fabricated generic attack animations");
}

const adventurerHead = read("src/main/java/alku/csrp/entity/SimAdventurerHeadEntity.java");
for (const action of ["func_78087_a.limb_swing",
  "func_78087_a.limb_swing.get_parasite_status_1",
  "func_78087_a.age_in_ticks.get_parasite_status_10"]) {
  if (!adventurerHead.includes(`"${action}"`)) {
    failures.push(`SimAdventurerHeadEntity: original function ${action} is not wired`);
  }
}
if (!adventurerHead.includes("class HeadMeleeGoal extends MeleeAttackGoal")
    || !adventurerHead.includes("setParasiteStatus(1)")
    || !adventurerHead.includes("setParasiteStatus(10)")) {
  failures.push("SimAdventurerHeadEntity: pursuit and leap do not drive original statuses 1 and 10");
}
if (adventurerHead.includes('triggerableAnim("attack"') || adventurerHead.includes("triggerAnim(")) {
  failures.push("SimAdventurerHeadEntity: still uses a fabricated generic attack animation");
}

const incompleteSmall = read("src/main/java/alku/csrp/entity/IncompleteFormSmallEntity.java");
const incompleteMedium = read("src/main/java/alku/csrp/entity/IncompleteFormMediumEntity.java");
if (!incompleteSmall.includes('"func_78087_a.age_in_ticks"')
    || !incompleteSmall.includes('"age_controller"')) {
  failures.push("Incomplete forms: extracted age-in-ticks function is not continuously wired");
}
if (incompleteSmall.includes('triggerableAnim("attack"') || incompleteSmall.includes("triggerAnim(")
    || incompleteMedium.includes('triggerableAnim("attack"') || incompleteMedium.includes("triggerAnim(")) {
  failures.push("Incomplete forms: still use fabricated movement or attack animations");
}

const crux = read("src/main/java/alku/csrp/entity/CruxEntity.java");
for (const action of [
  "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
  "func_78087_a.age_in_ticks.get_parasite_status_1",
  "func_78087_a.limb_swing.get_parasite_status_1",
  "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
  "func_78087_a.limb_swing.get_parasite_status_2", "get_attack_timer_m",
  "get_attack_timer_m.get_parasite_status_1",
  "get_attack_timer_m.get_parasite_status_1.get_still_ani_1",
  "get_attack_timer_r",
  "get_attack_timer_r.get_parasite_status_1",
  "get_attack_timer_r.get_parasite_status_1.get_still_ani_1"
]) {
  if (!crux.includes(`"${action}"`)) {
    failures.push(`CruxEntity: original runtime animation ${action} is not wired`);
  }
}
if (/"(?:idle|walk|idle\.get_parasite_status_3|get_attack_timer_[mr]\.get_parasite_status_[23])"/.test(crux)) {
  failures.push("CruxEntity: still requests fabricated functions absent from ModelCruxA");
}
if (!crux.includes("stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS")) {
  failures.push("CruxEntity: legacy stillAni delay is not represented");
}
if (!crux.includes("setSprinting(true)") || !crux.includes("setSprinting(false)")) {
  failures.push("CruxEntity: pursuit does not expose the original sprinting animation status");
}

const movingFlesh = read("src/main/java/alku/csrp/entity/MovingFleshEntity.java");
for (const action of ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"]) {
  if (!movingFlesh.includes(`"${action}"`)) {
    failures.push(`MovingFleshEntity: original ModelLesh function ${action} is not wired`);
  }
}
if (movingFlesh.includes('ParasiteAnimations.loop(this, "idle"')
    || movingFlesh.includes('ParasiteAnimations.loop(this, "walk"')
    || movingFlesh.includes('triggerableAnim("attack"')) {
  failures.push("MovingFleshEntity: still uses animations from an unrelated model");
}
const movingFleshAnimation = JSON.parse(read(
  "src/main/resources/assets/csrp/animations/movingflesh.animation.json"));
const expectedMovingFleshKeys = [
  "animation.movingflesh.func_78087_a.age_in_ticks",
  "animation.movingflesh.func_78087_a.limb_swing"
].sort();
const actualMovingFleshKeys = Object.keys(movingFleshAnimation.animations).sort();
if (actualMovingFleshKeys.length !== expectedMovingFleshKeys.length
    || actualMovingFleshKeys.some((key, index) => key !== expectedMovingFleshKeys[index])) {
  failures.push("Moving Flesh animation keys differ from the extracted ModelLesh functions");
}

const assimilatedMeltSystem = read("src/main/java/alku/csrp/entity/AssimilatedMeltSystem.java");
const assimilatedAnimal = read("src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java");
const assimilatedVariant = read("src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java");
const assimilatedHuman = read("src/main/java/alku/csrp/entity/SimHumanEntity.java");
const assimilatedAdventurer = read("src/main/java/alku/csrp/entity/SimAdventurerEntity.java");
if (!assimilatedMeltSystem.includes("KILL_THRESHOLD = 10")
    || !assimilatedMeltSystem.includes("movingFleshCount >= 1 && movingFleshCount <= 3")
    || !assimilatedMeltSystem.includes("REQUIRED_NEARBY_ASSIMILATED = 3")
    || !assimilatedMeltSystem.includes("Config.evolutionPhase(serverLevel) < MINIMUM_MERGE_PHASE")) {
  failures.push("AssimilatedMeltSystem: original EntityAIInfectedSearch conditions are incomplete");
}
for (const [className, source] of [
  ["AssimilatedParasiteEntity", assimilatedAnimal],
  ["AssimilatedVariantEntity", assimilatedVariant],
  ["SimHumanEntity", assimilatedHuman],
  ["SimAdventurerEntity", assimilatedAdventurer]
]) {
  if (!source.includes("MeltableAssimilated")
      || !source.includes("AssimilatedMeltSystem.tryStartGroup")
      || !source.includes("AssimilatedMeltSystem.spawnMovingFlesh")) {
    failures.push(`${className}: registered assimilated melt path is incomplete`);
  }
}
if (!movingFlesh.includes("EntityDataAccessor<Integer> MERGE_VALUE")
    || !movingFlesh.includes("setMergeValue(getMergeValue() + other.getMergeValue())")
    || !movingFlesh.includes("random.nextInt(9)")
    || !movingFlesh.includes("SPAWN_HEALTH_FRACTION = 0.5F")) {
  failures.push("MovingFleshEntity: legacy merge value, mob table, or spawn health is incomplete");
}
if (!assimilatedVariant.includes("HOST_SKELETON_KILLS = 5")
    || !assimilatedVariant.includes("transformToHost(level)")
    || !assimilatedHuman.includes("HOST_SKELETON_KILLS = 5")
    || !assimilatedHuman.includes("transformToHost(level)")) {
  failures.push("Assimilated Human/Villager skeleton-kill Host conversion is incomplete");
}
for (const obsolete of [
  "AssimilatedChickenEntity.java", "AssimilatedCowEntity.java", "AssimilatedPigEntity.java",
  "AssimilatedSheepEntity.java", "AssimilatedVillagerEntity.java"
]) {
  if (fs.existsSync(path.join(root, "src/main/java/alku/csrp/entity", obsolete))) {
    failures.push(`${obsolete}: unregistered duplicate implementation still masks the runtime class`);
  }
}

const incompleteCrux = read("src/main/java/alku/csrp/entity/IncompleteCruxEntity.java");
for (const action of ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"]) {
  if (!incompleteCrux.includes(`"${action}"`)) {
    failures.push(`IncompleteCruxEntity: original runtime animation ${action} is not wired`);
  }
}
if (/ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:melee_attack|ranged_attack|burst)"/.test(incompleteCrux)) {
  failures.push("IncompleteCruxEntity: requests a fabricated CruxA action animation");
}

const longarms = read("src/main/java/alku/csrp/entity/LongarmsEntity.java");
for (const action of [
  "func_78087_a.limb_swing", "get_attack_timer",
  "func_78087_a.age_in_ticks.get_still_ani_1", "get_attack_timer.get_still_ani_1",
  "func_78087_a.limb_swing.get_parasite_status_1",
  "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
  "get_attack_timer.get_parasite_status_1.get_still_ani_1",
  "func_78087_a.limb_swing.get_parasite_status_2",
  "get_attack_timer.get_parasite_status_2"
]) {
  if (!longarms.includes(`"${action}"`)) {
    failures.push(`LongarmsEntity: original runtime animation ${action} is not wired`);
  }
}
if (/ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:idle|walk|attack|idle\.get_parasite_status_[23]|get_attack_timer\.get_parasite_status_[13])"/.test(longarms)) {
  failures.push("LongarmsEntity: still requests fabricated functions absent from ModelShyco");
}
if (!longarms.includes("stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS")) {
  failures.push("LongarmsEntity: legacy stillAni delay is not represented");
}

for (const [className, actions] of [
  ["BuglinEntity", ["func_78087_a.age_in_ticks", "get_floor_timer"]],
  ["GnatEntity", [
    "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_10"
  ]],
  ["RupterEntity", [
    "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_10"
  ]],
  ["CarrierFlyingEntity", ["func_78087_a.age_in_ticks"]],
  ["CarrierHeavyEntity", ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"]],
  ["CarrierLightEntity", ["func_78087_a.age_in_ticks", "func_78087_a.limb_swing"]],
  ["SummonerEntity", [
    "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_10"
  ]],
  ["VerminEntity", ["func_78087_a.age_in_ticks"]],
  ["VisceraEntity", [
    "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2"
  ]]
]) {
  const source = read(`src/main/java/alku/csrp/entity/${className}.java`);
  for (const action of actions) {
    if (!source.includes(`"${action}"`)) {
      failures.push(`${className}: original runtime animation ${action} is not wired`);
    }
  }
  if (/ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:idle|walk|run|attack|spawn|summon)"/.test(source)) {
    failures.push(`${className}: still requests a fabricated generic animation`);
  }
}

const carrierBase = read("src/main/java/alku/csrp/entity/CarrierEntity.java");
if (carrierBase.includes('triggerAnim("attack_controller"')
    || /ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:walk|attack)"/.test(carrierBase)) {
  failures.push("CarrierEntity: shared carrier logic still requests fabricated movement or attack clips");
}

const dredge = read("src/main/java/alku/csrp/entity/DredgeEntity.java");
for (const action of [
  "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
  "func_78087_a.age_in_ticks.get_still_ani_1",
  "func_78087_a.age_in_ticks.get_parasite_status_1",
  "func_78087_a.limb_swing.get_parasite_status_1",
  "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
  "func_78087_a.age_in_ticks.get_parasite_status_2",
  "func_78087_a.limb_swing.get_parasite_status_2",
  "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1"
]) {
  if (!dredge.includes(`"${action}"`)) {
    failures.push(`DredgeEntity: original runtime animation ${action} is not wired`);
  }
}
if (!dredge.includes("setParasiteStatus(STATUS_PULLING)")) {
  failures.push("DredgeEntity: legacy pulling state 3 is not synchronized");
}
if (!dredge.includes("case STATUS_PULLING -> state.setAndContinue(IDLE)")
    || dredge.includes("get_parasite_status_3")) {
  failures.push("DredgeEntity: status 3 must use the unchanged base pose from ModelDredge");
}

const host = read("src/main/java/alku/csrp/entity/HostEntity.java");
for (const action of [
  "func_78087_a.age_in_ticks", "get_attack_timer", "get_burrow_timer",
  "func_78087_a.age_in_ticks.get_open_1", "get_attack_timer.get_open_1", "get_burrow_timer.get_open_1",
  "func_78087_a.age_in_ticks.get_burrowed_1", "get_attack_timer.get_burrowed_1",
  "get_burrow_timer.get_burrowed_1", "func_78087_a.age_in_ticks.get_burrowed_1.get_open_1",
  "get_attack_timer.get_burrowed_1.get_open_1",
  "get_burrow_timer.get_burrowed_1.get_open_1"
]) {
  if (!host.includes(`"${action}"`)) {
    failures.push(`HostEntity: original runtime animation ${action} is not wired`);
  }
}
if (!host.includes("MAX_BURIED_TIMER = 4.8F") || !host.includes("BURIED_TIMER_STEP = 0.08F")) {
  failures.push("HostEntity: legacy burrow transition timing is not represented");
}

const hostII = read("src/main/java/alku/csrp/entity/HostIIEntity.java");
for (const action of [
  "func_78087_a.age_in_ticks", "get_burrow_timer",
  "func_78087_a.age_in_ticks.get_open_1", "get_burrow_timer.get_open_1",
  "func_78087_a.age_in_ticks.get_burrowed_1", "get_burrow_timer.get_burrowed_1",
  "func_78087_a.age_in_ticks.get_burrowed_1.get_open_1",
  "get_burrow_timer.get_burrowed_1.get_open_1"
]) {
  if (!hostII.includes(`"${action}"`)) {
    failures.push(`HostIIEntity: original runtime animation ${action} is not wired`);
  }
}
if (hostII.includes('triggerableAnim("attack"') || hostII.includes('triggerAnim("attack_controller"')) {
  failures.push("HostIIEntity: still fabricates an attack clip absent from ModelHostII");
}

for (const [className, actions] of [
  ["AssimilatedParasiteEntity", [...new Set([
    ...assimilatedExpected.sim_bear, ...assimilatedExpected.sim_cow,
    ...assimilatedExpected.sim_pig, ...assimilatedExpected.sim_sheep,
    ...assimilatedExpected.sim_wolf, ...assimilatedExpected.sim_squid
  ])]],
  ["AssimilatedVariantEntity", [...new Set([
    ...assimilatedExpected.sim_bigspider, ...assimilatedExpected.sim_horse,
    ...assimilatedExpected.sim_villager
  ])]],
  ["SimHumanEntity", assimilatedExpected.sim_human],
  ["SimAdventurerEntity", assimilatedExpected.sim_adventurer],
  ["AssimilatedDragonHeadEntity", assimilatedExpected.sim_dragonhead]
]) {
  const source = read(`src/main/java/alku/csrp/entity/${className}.java`);
  for (const action of actions) {
    if (!source.includes(`"${action}"`)) {
      failures.push(`${className}: original assimilated function ${action} is not wired`);
    }
  }
  if (/ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:idle|walk|run|attack)"/.test(source)
      || source.includes('triggerableAnim("attack"') || source.includes('triggerAnim("attack_controller"')) {
    failures.push(`${className}: still uses a fabricated generic assimilated animation`);
  }
  if (!source.includes('"age_controller"')
      || !source.includes('ParasiteAnimations.isMoving(this, state.isMoving())')) {
    failures.push(`${className}: age and actual-displacement function controllers are incomplete`);
  }
}

for (const [className, actions] of [
  ["HiBlazeEntity", hijackedAndFeralExpected.hi_blaze],
  ["HiGolemEntity", hijackedAndFeralExpected.hi_golem],
  ["HiSkeletonEntity", hijackedAndFeralExpected.hi_skeleton],
  ["FeralParasiteEntity", [...new Set([
    ...hijackedAndFeralExpected.fer_bear,
    ...hijackedAndFeralExpected.fer_cow,
    ...hijackedAndFeralExpected.fer_horse,
    ...hijackedAndFeralExpected.fer_human,
    ...hijackedAndFeralExpected.fer_pig,
    ...hijackedAndFeralExpected.fer_sheep,
    ...hijackedAndFeralExpected.fer_villager,
    ...hijackedAndFeralExpected.fer_wolf
  ])]],
  ["FeralEndermanEntity", hijackedAndFeralExpected.fer_enderman]
]) {
  const source = read(`src/main/java/alku/csrp/entity/${className}.java`);
  for (const action of actions) {
    if (!source.includes(`"${action}"`)) {
      failures.push(`${className}: original hijacked/feral function ${action} is not wired`);
    }
  }
  if (/ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:idle|walk|run|attack)"/.test(source)
      || source.includes('triggerableAnim("attack"')
      || source.includes('triggerAnim("attack_controller"')) {
    failures.push(`${className}: still uses a fabricated generic hijacked/feral animation`);
  }
  if (!source.includes('"age_controller"')
      || (className !== "HiBlazeEntity"
        && (!source.includes('"movement_controller"')
          || !source.includes("ParasiteAnimations.isMoving(this, state.isMoving())")))) {
    failures.push(`${className}: age and actual-displacement function controllers are incomplete`);
  }
}

const hijackedBase = read("src/main/java/alku/csrp/entity/HijackedParasiteEntity.java");
if (hijackedBase.includes("registerControllers") || hijackedBase.includes("triggerAttackAnimation")
    || hijackedBase.includes("RawAnimation")) {
  failures.push("HijackedParasiteEntity: shared base still fabricates animations absent from individual models");
}

const feral = read("src/main/java/alku/csrp/entity/FeralParasiteEntity.java");
if (!feral.includes("EntityDataAccessor<Integer> PARASITE_STATUS")
    || !feral.includes("EntityDataAccessor<Boolean> STILL_ANI")
    || !feral.includes("ParasiteAnimations.isMoving(this, state.isMoving())")
    || !feral.includes("status = distanceToSqr(target) > attackReachSqr ? 2 : 1")) {
  failures.push("FeralParasiteEntity: legacy status 1/2, still state, or actual-displacement controller is incomplete");
}

const marauderizedBase = read("src/main/java/alku/csrp/entity/MarauderizedParasiteEntity.java");
for (const [className, id] of [
  ["MarauderizedBearEntity", "mar_bear"],
  ["MarauderizedCowEntity", "mar_cow"],
  ["MarauderizedEndermanEntity", "mar_enderman"],
  ["MarauderizedHumanEntity", "mar_human"],
  ["MarauderizedSheepEntity", "mar_sheep"],
  ["MarauderizedVillagerEntity", "mar_villager"]
]) {
  const entitySource = read(`src/main/java/alku/csrp/entity/${className}.java`);
  const source = marauderizedBase + entitySource;
  for (const action of marauderizedExpected[id]) {
    if (!source.includes(`"${action}"`)) {
      failures.push(`${className}: original Marauderized function ${action} is not wired`);
    }
  }
  if (/ParasiteAnimations\.(?:loop|play)\(this,\s*"(?:idle|walk|run|attack)"/.test(source)
      || source.includes('triggerableAnim("attack"')
      || source.includes('triggerAnim("attack_controller"')) {
    failures.push(`${className}: still uses a fabricated generic Marauderized animation`);
  }
  if (!source.includes('"age_controller"')
      || !source.includes('ParasiteAnimations.isMoving(this, state.isMoving())')
      || !source.includes("forcedStatusTicks")
      || !source.includes("startAttackAnimation()")) {
    failures.push(`${className}: attack-priority, status, or actual-displacement routing is incomplete`);
  }
}

const marauder = read("src/main/java/alku/csrp/entity/MarauderEntity.java");
for (const action of marauderizedExpected.marauder) {
  if (!marauder.includes(`"${action}"`)) {
    failures.push(`MarauderEntity: original function ${action} is not wired`);
  }
}
if (!marauder.includes("EntityDataAccessor<Integer> PARASITE_STATUS")
    || !marauder.includes("EntityDataAccessor<Boolean> STILL_ANI")
    || !marauder.includes("if (getAttackTicks() > 0)")
    || !marauder.includes('"age_controller"')
    || !marauder.includes("ParasiteAnimations.isMoving(this, state.isMoving())")
    || marauder.includes('triggerAnim("attack_controller"')
    || marauder.includes('triggerableAnim("swipe"')) {
  failures.push("MarauderEntity: original status/attack-timer priority routing is incomplete");
}

const hiSkeleton = read("src/main/java/alku/csrp/entity/HiSkeletonEntity.java");
if (!hiSkeleton.includes("EntityDataAccessor<Integer> PARASITE_STATUS")
    || !hiSkeleton.includes("? 2 : 0")) {
  failures.push("HiSkeletonEntity: ranged status 2 is not synchronized");
}

const triggeredFamilies = [
  ["AdaptedVariantEntity.java", "bolster_attack_controller"],
  ["AncientParasiteEntity.java", "attack_controller"],
];
for (const [file, controller] of triggeredFamilies) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  if (!source.includes(`triggerableAnim("attack"`)) {
    failures.push(`${file}: attack animation is not registered`);
  }
  if (!source.includes(`triggerAnim("${controller}", "attack")`)) {
    failures.push(`${file}: server attacks do not trigger the client animation`);
  }
}

const animationRouter = read("src/main/java/alku/csrp/entity/ParasiteAnimations.java");
if (!animationRouter.includes("!animationMoving || isAttacking(entity)")
    || !animationRouter.includes("living.swinging || living.getAttackAnim(1.0F) > 0.0F")) {
  failures.push("ParasiteAnimations: attack state does not take priority over locomotion");
}

const primitiveBase = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
if (!primitiveBase.includes("if (!swinging)")
    || !primitiveBase.includes("swing(InteractionHand.MAIN_HAND)")) {
  failures.push("PrimitiveParasiteEntity: successful custom melee hits do not synchronize the attack window");
}

if (failures.length) {
  console.error(`Entity animation contract verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(`Verified extracted animation contracts for all ${all.length} legacy bestiary entities.`);
console.log("Verified registered entity requests, shared-family state functions, and server attack triggers.");
