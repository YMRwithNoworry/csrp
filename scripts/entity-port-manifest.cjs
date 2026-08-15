const groups = {
  current: [
    "buglin", "gnat", "rupter", "carrier_flying", "carrier_heavy", "carrier_light",
    "carrier_worm", "crux", "crux_incomplete", "pri_longarms", "pri_summoner", "pri_vermin",
    "pri_viscera"
  ],
  crude: ["airscrew", "heed", "dredge", "thrall"],
  early_lifecycle: [
    "lice", "mangler", "host", "hostii", "incompleteform_medium", "incompleteform_small",
    "draconite", "kirin", "movingflesh", "worker"
  ],
  assimilated: [
    "sim_adventurer", "sim_adventurerhead", "sim_bear", "sim_bigspider", "sim_cow", "sim_cowhead",
    "sim_dragone", "sim_dragonehead", "sim_enderman", "sim_endermanhead", "sim_horse", "sim_horsehead",
    "sim_human", "sim_humanhead", "sim_pig", "sim_pighead", "sim_sheep", "sim_sheephead",
    "sim_squid", "sim_villager", "sim_villagerhead", "sim_wolf", "sim_wolfhead"
  ],
  hijacked_and_feral: [
    "hi_blaze", "hi_golem", "hi_skeleton", "fer_bear", "fer_cow", "fer_enderman", "fer_horse",
    "fer_human", "fer_pig", "fer_sheep", "fer_villager", "fer_wolf"
  ],
  marauderized: [
    "mar_bear", "mar_cow", "mar_enderman", "mar_human", "mar_sheep", "mar_villager", "marauder"
  ],
  primitive: [
    "pri_arachnida", "pri_bolster", "pri_burrower", "pri_devourer", "pri_manducater",
    "pri_reeker", "pri_tozoon", "pri_yelloweye"
  ],
  adapted: [
    "ada_arachnida", "ada_bolster", "ada_burrower", "ada_devourer", "ada_longarms",
    "ada_manducater", "ada_reeker", "ada_summoner", "ada_tozoon", "ada_vermin",
    "ada_viscera", "ada_yelloweye"
  ],
  pure_and_preeminent: [
    "dispatcherten", "kyphosis", "seizer", "sentry", "worm", "grunt", "bomber_light", "monarch",
    "overseer", "vigilante", "warden", "bogle", "carrier_colony", "haunter", "bomber_heavy",
    "wraith", "succor", "seeker", "architect"
  ],
  ancient: ["anc_dreadnaut", "anc_overlord", "anc_pod", "anc_dreadnaut_ten"],
  nexus_and_aberrant: [
    "beckon_si", "beckon_sii", "beckon_siii", "beckon_siv", "dispatcher_si", "dispatcher_sii",
    "dispatcher_siii", "dispatcher_siv", "rooter_si", "rooter_sii", "rooter_siii", "rooter_siv",
    "rooterball", "abo_bodies", "abo_head"
  ]
};

const all = Object.values(groups).flat();

const behaviorPorts = {
  architect: {
    originalClass: "EntityTenn",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/preeminent/EntityTenn.java",
    implementation: "src/main/java/alku/csrp/entity/ArchitectEntity.java",
    verifier: "scripts/verify-architect-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  },
  grunt: {
    originalClass: "EntityFlog",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityFlog.java",
    implementation: "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    verifier: "scripts/verify-grunt-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  },
  bomber_light: {
    originalClass: "EntityOmboo",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityOmboo.java",
    implementation: "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    verifier: "scripts/verify-bomber-light-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  },
  monarch: {
    originalClass: "EntityOrch",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityOrch.java",
    implementation: "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    verifier: "scripts/verify-monarch-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  },
  overseer: {
    originalClass: "EntityAlafha",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityAlafha.java",
    implementation: "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    verifier: "scripts/verify-overseer-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  },
  vigilante: {
    originalClass: "EntityAnged",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityAnged.java",
    implementation: "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    verifier: "scripts/verify-vigilante-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  },
  seeker: {
    originalClass: "EntitySoo",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/EntitySoo.java",
    implementation: "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    verifier: "scripts/verify-seeker-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  },
  warden: {
    originalClass: "EntityGanro",
    originalSource: "com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityGanro.java",
    implementation: "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    verifier: "scripts/verify-warden-port.cjs",
    status: "audited",
    auditScope: "entity-specific"
  }
};

module.exports = { groups, all, behaviorPorts };
