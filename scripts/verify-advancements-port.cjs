const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const exists = (relative) => fs.existsSync(path.join(root, relative));
const failures = [];
const expect = (condition, message) => { if (!condition) failures.push(message); };

// Expected contract: the original SRP 1.10.9 advancement set, rewritten for 1.21.1.
const EXPECTED = {
  "brew_fear.json": {
    "parent": "csrp:secret_root",
    "icon": "csrp:ecstasy_icon",
    "title": {
      "translate": "advancements.csrp.brew_fear.title"
    },
    "description": {
      "translate": "advancements.csrp.brew_fear.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "brewed_fear": {
        "trigger": "minecraft:brewed_potion",
        "conditions": {
          "potion": "csrp:fear"
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "columbus.json": {
    "parent": "csrp:cut_roots",
    "icon": "csrp:potion_columbus_icon",
    "title": {
      "translate": "advancements.csrp.columbus.title"
    },
    "description": {
      "translate": "advancements.csrp.columbus.description"
    },
    "frame": "goal",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "unlock": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "controversial.json": {
    "parent": "csrp:secret_root",
    "icon": "csrp:pearl",
    "title": {
      "translate": "advancements.csrp.controversial.title"
    },
    "description": {
      "translate": "advancements.csrp.controversial.description"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_sim_enderman": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_enderman"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "cosmic_structural_failure.json": {
    "parent": "csrp:kill_deterrent",
    "icon": "csrp:cosmic_structural_failure_icon",
    "title": {
      "translate": "advancement.csrp.cosmic_structural_failure.title"
    },
    "description": {
      "translate": "advancement.csrp.cosmic_structural_failure.desc"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_kirin": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:kirin"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "cut_roots.json": {
    "parent": "csrp:sepeku",
    "icon": "minecraft:shears",
    "title": {
      "translate": "advancements.csrp.cut_roots.title"
    },
    "description": {
      "translate": "advancements.csrp.cut_roots.description"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "reached_1000_rupter_kills": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "dark_days.json": {
    "parent": "csrp:stolas",
    "icon": "csrp:dark_days_icon",
    "title": {
      "translate": "advancements.csrp.dark_days.title"
    },
    "description": {
      "translate": "advancements.csrp.dark_days.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "unlock": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "enemy_enemy.json": {
    "parent": "csrp:thornshade_self_destruct",
    "icon": "csrp:enemy_of_enemy_icon",
    "title": {
      "translate": "advancements.csrp.enemy_enemy.title"
    },
    "description": {
      "translate": "advancements.csrp.enemy_enemy.description"
    },
    "frame": "goal",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "creeper_killed_parasite": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "fermented_arrogance.json": {
    "parent": "csrp:brew_fear",
    "icon": "csrp:ada_summoner_drop",
    "title": {
      "translate": "advancements.csrp.fermented_arrogance.title"
    },
    "description": {
      "translate": "advancements.csrp.fermented_arrogance.description"
    },
    "frame": "goal",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_fer_enderman": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:fer_enderman"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "firmamentum_root.json": {
    "parent": "csrp:root",
    "icon": "csrp:greek_fire",
    "title": {
      "translate": "advancements.csrp.firmamentum_root.title"
    },
    "description": {
      "translate": "advancements.csrp.firmamentum_root.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": false,
    "announce_to_chat": false,
    "hidden": true,
    "criteria": {
      "tick": {
        "trigger": "minecraft:tick"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "fog_nullifier.json": {
    "parent": "csrp:firmamentum_root",
    "icon": "csrp:fog_nullifier_icon",
    "title": {
      "translate": "advancements.csrp.fog_nullifier.title"
    },
    "description": {
      "translate": "advancements.csrp.fog_nullifier.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "got_fog_nullifier": {
        "trigger": "minecraft:inventory_changed",
        "conditions": {
          "items": [
            {
              "items": "csrp:fog_nullifier"
            }
          ]
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "guerilla.json": {
    "parent": "csrp:controversial",
    "icon": "csrp:guerilla_icon",
    "title": {
      "translate": "advancements.csrp.guerilla.title"
    },
    "description": {
      "translate": "advancements.csrp.guerilla.description"
    },
    "frame": "goal",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_fear_splashed": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "hellfire_chemical_warfare.json": {
    "parent": "csrp:kill_deterrent",
    "icon": "csrp:hellfire_chemical_warfare_icon",
    "title": {
      "translate": "advancement.csrp.hellfire_chemical_warfare.title"
    },
    "description": {
      "translate": "advancement.csrp.hellfire_chemical_warfare.desc"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_draconite": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:draconite"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "hunt_season.json": {
    "parent": "csrp:enemy_enemy",
    "icon": "csrp:hunt_season_icon",
    "title": {
      "translate": "advancements.csrp.hunt_season.title"
    },
    "description": {
      "translate": "advancements.csrp.hunt_season.description"
    },
    "frame": "goal",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "fifty_in_day": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_adapted.json": {
    "parent": "csrp:kill_assimilated",
    "icon": "csrp:adapted_icon",
    "title": {
      "translate": "advancements.csrp.kill_adapted.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_adapted.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_ada_arachnida": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_arachnida"
          }
        }
      },
      "k_ada_bolster": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_bolster"
          }
        }
      },
      "k_ada_burrower": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_burrower"
          }
        }
      },
      "k_ada_devourer": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_devourer"
          }
        }
      },
      "k_ada_longarms": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_longarms"
          }
        }
      },
      "k_ada_manducater": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_manducater"
          }
        }
      },
      "k_ada_reeker": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_reeker"
          }
        }
      },
      "k_ada_summoner": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_summoner"
          }
        }
      },
      "k_ada_tozoon": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_tozoon"
          }
        }
      },
      "k_ada_viscera": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_viscera"
          }
        }
      },
      "k_ada_yelloweye": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:ada_yelloweye"
          }
        }
      }
    },
    "requirements": [
      [
        "k_ada_arachnida"
      ],
      [
        "k_ada_bolster"
      ],
      [
        "k_ada_burrower"
      ],
      [
        "k_ada_devourer"
      ],
      [
        "k_ada_longarms"
      ],
      [
        "k_ada_manducater"
      ],
      [
        "k_ada_reeker"
      ],
      [
        "k_ada_summoner"
      ],
      [
        "k_ada_tozoon"
      ],
      [
        "k_ada_viscera"
      ],
      [
        "k_ada_yelloweye"
      ]
    ],
    "rewards": null
  },
  "kill_assimilated.json": {
    "parent": "csrp:kill_inborn",
    "icon": "csrp:lurecomponent2",
    "title": {
      "translate": "advancements.csrp.kill_assimilated.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_assimilated.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_sim_adventurer": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_adventurer"
          }
        }
      },
      "k_sim_adventurerhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_adventurerhead"
          }
        }
      },
      "k_sim_bear": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_bear"
          }
        }
      },
      "k_sim_bigspider": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_bigspider"
          }
        }
      },
      "k_sim_cow": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_cow"
          }
        }
      },
      "k_sim_cowhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_cowhead"
          }
        }
      },
      "k_sim_dragone": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_dragone"
          }
        }
      },
      "k_sim_dragonehead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_dragonehead"
          }
        }
      },
      "k_sim_enderman": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_enderman"
          }
        }
      },
      "k_sim_endermanhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_endermanhead"
          }
        }
      },
      "k_sim_horse": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_horse"
          }
        }
      },
      "k_sim_horsehead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_horsehead"
          }
        }
      },
      "k_sim_human": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_human"
          }
        }
      },
      "k_sim_humanhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_humanhead"
          }
        }
      },
      "k_sim_pig": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_pig"
          }
        }
      },
      "k_sim_pighead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_pighead"
          }
        }
      },
      "k_sim_sheep": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_sheep"
          }
        }
      },
      "k_sim_sheephead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_sheephead"
          }
        }
      },
      "k_sim_squid": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_squid"
          }
        }
      },
      "k_sim_villager": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_villager"
          }
        }
      },
      "k_sim_villagerhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_villagerhead"
          }
        }
      },
      "k_sim_wolf": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_wolf"
          }
        }
      },
      "k_sim_wolfhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_wolfhead"
          }
        }
      }
    },
    "requirements": [
      [
        "k_sim_adventurer"
      ],
      [
        "k_sim_adventurerhead"
      ],
      [
        "k_sim_bear"
      ],
      [
        "k_sim_bigspider"
      ],
      [
        "k_sim_cow"
      ],
      [
        "k_sim_cowhead"
      ],
      [
        "k_sim_dragone"
      ],
      [
        "k_sim_dragonehead"
      ],
      [
        "k_sim_enderman"
      ],
      [
        "k_sim_endermanhead"
      ],
      [
        "k_sim_horse"
      ],
      [
        "k_sim_horsehead"
      ],
      [
        "k_sim_human"
      ],
      [
        "k_sim_humanhead"
      ],
      [
        "k_sim_pig"
      ],
      [
        "k_sim_pighead"
      ],
      [
        "k_sim_sheep"
      ],
      [
        "k_sim_sheephead"
      ],
      [
        "k_sim_squid"
      ],
      [
        "k_sim_villager"
      ],
      [
        "k_sim_villagerhead"
      ],
      [
        "k_sim_wolf"
      ],
      [
        "k_sim_wolfhead"
      ]
    ],
    "rewards": null
  },
  "kill_assimilated_head.json": {
    "parent": "csrp:kill_inborn",
    "icon": "csrp:assimilated_flesh",
    "title": {
      "translate": "advancements.csrp.beheaded.title"
    },
    "description": {
      "translate": "advancements.csrp.beheaded.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_sim_adventurerhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_adventurerhead"
          }
        }
      },
      "k_sim_cowhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_cowhead"
          }
        }
      },
      "k_sim_dragonehead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_dragonehead"
          }
        }
      },
      "k_sim_endermanhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_endermanhead"
          }
        }
      },
      "k_sim_horsehead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_horsehead"
          }
        }
      },
      "k_sim_humanhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_humanhead"
          }
        }
      },
      "k_sim_pighead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_pighead"
          }
        }
      },
      "k_sim_sheephead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_sheephead"
          }
        }
      },
      "k_sim_villagerhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_villagerhead"
          }
        }
      },
      "k_sim_wolfhead": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sim_wolfhead"
          }
        }
      }
    },
    "requirements": [
      [
        "k_sim_adventurerhead"
      ],
      [
        "k_sim_cowhead"
      ],
      [
        "k_sim_dragonehead"
      ],
      [
        "k_sim_endermanhead"
      ],
      [
        "k_sim_horsehead"
      ],
      [
        "k_sim_humanhead"
      ],
      [
        "k_sim_pighead"
      ],
      [
        "k_sim_sheephead"
      ],
      [
        "k_sim_villagerhead"
      ],
      [
        "k_sim_wolfhead"
      ]
    ],
    "rewards": null
  },
  "kill_beckon_s1.json": {
    "parent": "csrp:monument_root",
    "icon": "csrp:parasitebush",
    "title": {
      "translate": "advancements.csrp.kill_beckon_s1.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_beckon_s1.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_beckon_si": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:beckon_si"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_beckon_s2.json": {
    "parent": "csrp:kill_beckon_s1",
    "icon": "csrp:infestedbush",
    "title": {
      "translate": "advancements.csrp.kill_beckon_s2.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_beckon_s2.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_beckon_sii": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:beckon_sii"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_beckon_s3.json": {
    "parent": "csrp:kill_beckon_s2",
    "icon": "csrp:infestedbush",
    "title": {
      "translate": "advancements.csrp.kill_beckon_s3.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_beckon_s3.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_beckon_siii": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:beckon_siii"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_beckon_s4.json": {
    "parent": "csrp:kill_beckon_s3",
    "icon": "csrp:infestedbush",
    "title": {
      "translate": "advancements.csrp.kill_beckon_s4.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_beckon_s4.description"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_beckon_siv": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:beckon_siv"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_crude.json": {
    "parent": "csrp:kill_adapted",
    "icon": "csrp:crude_icon",
    "title": {
      "translate": "advancements.csrp.kill_crude.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_crude.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_crux": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:crux"
          }
        }
      },
      "k_heed": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:heed"
          }
        }
      },
      "k_incomplete_medium": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:incompleteform_medium"
          }
        }
      },
      "k_incomplete_small": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:incompleteform_small"
          }
        }
      },
      "k_host": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:host"
          }
        }
      },
      "k_hostii": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:hostii"
          }
        }
      },
      "k_thrall": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:thrall"
          }
        }
      },
      "k_movingflesh": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:movingflesh"
          }
        }
      }
    },
    "requirements": [
      [
        "k_crux"
      ],
      [
        "k_heed"
      ],
      [
        "k_incomplete_medium"
      ],
      [
        "k_incomplete_small"
      ],
      [
        "k_host"
      ],
      [
        "k_hostii"
      ],
      [
        "k_thrall"
      ],
      [
        "k_movingflesh"
      ]
    ],
    "rewards": null
  },
  "kill_deterrent.json": {
    "parent": "csrp:kill_adapted",
    "icon": "csrp:ada_reeker_drop",
    "title": {
      "translate": "advancements.csrp.kill_deterrent.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_deterrent.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_dispatcherten": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:dispatcherten"
          }
        }
      },
      "k_kyphosis": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:kyphosis"
          }
        }
      },
      "k_seizer": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:seizer"
          }
        }
      },
      "k_sentry": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:sentry"
          }
        }
      },
      "k_worm": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:worm"
          }
        }
      }
    },
    "requirements": [
      [
        "k_dispatcherten"
      ],
      [
        "k_kyphosis"
      ],
      [
        "k_seizer"
      ],
      [
        "k_sentry"
      ],
      [
        "k_worm"
      ]
    ],
    "rewards": null
  },
  "kill_dispatcher_s1.json": {
    "parent": "csrp:monument_root",
    "icon": "csrp:parasitebush",
    "title": {
      "translate": "advancements.csrp.kill_dispatcher_s1.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_dispatcher_s1.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_dispatcher_si": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:dispatcher_si"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_dispatcher_s2.json": {
    "parent": "csrp:kill_dispatcher_s1",
    "icon": "csrp:parasitesapling",
    "title": {
      "translate": "advancements.csrp.kill_dispatcher_s2.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_dispatcher_s2.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_dispatcher_sii": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:dispatcher_sii"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_dispatcher_s3.json": {
    "parent": "csrp:kill_dispatcher_s2",
    "icon": "csrp:parasitebush",
    "title": {
      "translate": "advancements.csrp.kill_dispatcher_s3.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_dispatcher_s3.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_dispatcher_siii": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:dispatcher_siii"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_dispatcher_s4.json": {
    "parent": "csrp:kill_dispatcher_s3",
    "icon": "csrp:dispatchern",
    "title": {
      "translate": "advancements.csrp.kill_dispatcher_s4.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_dispatcher_s4.description"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "kill_dispatcher_siv": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:dispatcher_siv"
          }
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "kill_inborn.json": {
    "parent": "csrp:kill_root",
    "icon": "csrp:lurecomponent1",
    "title": {
      "translate": "advancements.csrp.kill_inborn.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_inborn.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_buglin": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:buglin"
          }
        }
      },
      "k_rupter": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:rupter"
          }
        }
      },
      "k_mangler": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:mangler"
          }
        }
      },
      "k_gnat": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:gnat"
          }
        }
      },
      "k_carrier_flying": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:carrier_flying"
          }
        }
      },
      "k_carrier_heavy": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:carrier_heavy"
          }
        }
      },
      "k_carrier_light": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:carrier_light"
          }
        }
      }
    },
    "requirements": [
      [
        "k_buglin"
      ],
      [
        "k_rupter"
      ],
      [
        "k_mangler"
      ],
      [
        "k_gnat"
      ],
      [
        "k_carrier_flying"
      ],
      [
        "k_carrier_heavy"
      ],
      [
        "k_carrier_light"
      ]
    ],
    "rewards": null
  },
  "kill_primitive.json": {
    "parent": "csrp:kill_assimilated",
    "icon": "csrp:primitive_icon",
    "title": {
      "translate": "advancements.csrp.kill_primitive.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_primitive.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_pri_arachnida": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_arachnida"
          }
        }
      },
      "k_pri_bolster": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_bolster"
          }
        }
      },
      "k_pri_burrower": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_burrower"
          }
        }
      },
      "k_pri_devourer": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_devourer"
          }
        }
      },
      "k_pri_longarms": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_longarms"
          }
        }
      },
      "k_pri_manducater": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_manducater"
          }
        }
      },
      "k_pri_reeker": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_reeker"
          }
        }
      },
      "k_pri_summoner": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_summoner"
          }
        }
      },
      "k_pri_tozoon": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_tozoon"
          }
        }
      },
      "k_pri_vermin": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_vermin"
          }
        }
      },
      "k_pri_viscera": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_viscera"
          }
        }
      },
      "k_pri_yelloweye": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:pri_yelloweye"
          }
        }
      }
    },
    "requirements": [
      [
        "k_pri_arachnida"
      ],
      [
        "k_pri_bolster"
      ],
      [
        "k_pri_burrower"
      ],
      [
        "k_pri_devourer"
      ],
      [
        "k_pri_longarms"
      ],
      [
        "k_pri_manducater"
      ],
      [
        "k_pri_reeker"
      ],
      [
        "k_pri_summoner"
      ],
      [
        "k_pri_tozoon"
      ],
      [
        "k_pri_vermin"
      ],
      [
        "k_pri_viscera"
      ],
      [
        "k_pri_yelloweye"
      ]
    ],
    "rewards": null
  },
  "kill_pure.json": {
    "parent": "csrp:kill_root",
    "icon": "csrp:pure_icon",
    "title": {
      "translate": "advancements.csrp.kill_pure.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_pure.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "k_grunt": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:grunt"
          }
        }
      },
      "k_bomber_light": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:bomber_light"
          }
        }
      },
      "k_marauder": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:marauder"
          }
        }
      },
      "k_monarch": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:monarch"
          }
        }
      },
      "k_overseer": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:overseer"
          }
        }
      },
      "k_vigilante": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:vigilante"
          }
        }
      },
      "k_warden": {
        "trigger": "minecraft:player_killed_entity",
        "conditions": {
          "entity": {
            "type": "csrp:warden"
          }
        }
      }
    },
    "requirements": [
      [
        "k_grunt"
      ],
      [
        "k_bomber_light"
      ],
      [
        "k_marauder"
      ],
      [
        "k_monarch"
      ],
      [
        "k_overseer"
      ],
      [
        "k_vigilante"
      ],
      [
        "k_warden"
      ]
    ],
    "rewards": null
  },
  "kill_root.json": {
    "parent": "csrp:root",
    "icon": "csrp:srp_field_guide",
    "title": {
      "translate": "advancements.csrp.kill_root.title"
    },
    "description": {
      "translate": "advancements.csrp.kill_root.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": false,
    "announce_to_chat": false,
    "hidden": true,
    "criteria": {
      "tick": {
        "trigger": "minecraft:tick"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "monument.json": {
    "parent": "csrp:kill_beckon_s4",
    "icon": "csrp:beckon_drop",
    "title": {
      "translate": "advancements.csrp.monument.title"
    },
    "description": {
      "translate": "advancements.csrp.monument.description"
    },
    "frame": "goal",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "detected_beckon_siv": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "monument_root.json": {
    "parent": "csrp:root",
    "icon": "csrp:itemmobspawner_oronco",
    "title": {
      "translate": "advancements.csrp.monument_root.title"
    },
    "description": {
      "translate": "advancements.csrp.monument_root.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": false,
    "announce_to_chat": false,
    "hidden": true,
    "criteria": {
      "tick": {
        "trigger": "minecraft:tick"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "peacekeeper.json": {
    "parent": "csrp:firmamentum_root",
    "icon": "csrp:biomepurifier",
    "title": {
      "translate": "advancements.csrp.peacekeeper.title"
    },
    "description": {
      "translate": "advancements.csrp.peacekeeper.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "has_purifier": {
        "trigger": "minecraft:inventory_changed",
        "conditions": {
          "items": [
            {
              "items": "csrp:biomepurifier"
            }
          ]
        }
      }
    },
    "requirements": null,
    "rewards": null
  },
  "qliphoth_overload.json": {
    "parent": "csrp:kill_dispatcher_s4",
    "icon": "csrp:parasiterubbledense",
    "title": {
      "translate": "advancements.csrp.qliphoth_overload.title"
    },
    "description": {
      "translate": "advancements.csrp.qliphoth_overload.description"
    },
    "frame": "goal",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "detected_dispatcher_siv": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "recipes/misc/assimilated_jack_o_lantern.json": {
    "parent": "minecraft:recipes/root",
    "icon": null,
    "title": null,
    "description": null,
    "frame": null,
    "background": null,
    "show_toast": null,
    "announce_to_chat": null,
    "hidden": null,
    "criteria": {
      "has_ingredients": {
        "trigger": "minecraft:inventory_changed",
        "conditions": {
          "items": [
            {
              "items": "csrp:assimilated_pumpkin"
            },
            {
              "items": "minecraft:torch"
            }
          ]
        }
      }
    },
    "requirements": [
      [
        "has_ingredients"
      ]
    ],
    "rewards": {
      "recipes": [
        "csrp:assimilated_jack_o_lantern"
      ]
    }
  },
  "recipes/misc/gothshroom_to_purple_dye.json": {
    "parent": "minecraft:recipes/root",
    "icon": null,
    "title": null,
    "description": null,
    "frame": null,
    "background": null,
    "show_toast": null,
    "announce_to_chat": null,
    "hidden": null,
    "criteria": {
      "has_gothshroom": {
        "trigger": "minecraft:inventory_changed",
        "conditions": {
          "items": [
            {
              "items": "csrp:gothshroom"
            }
          ]
        }
      }
    },
    "requirements": [
      [
        "has_gothshroom"
      ]
    ],
    "rewards": {
      "recipes": [
        "csrp:gothshroom_to_purple_dye"
      ]
    }
  },
  "recipes/misc/srp_field_guide.json": {
    "parent": "minecraft:recipes/root",
    "icon": null,
    "title": null,
    "description": null,
    "frame": null,
    "background": null,
    "show_toast": null,
    "announce_to_chat": null,
    "hidden": null,
    "criteria": {
      "has_book": {
        "trigger": "minecraft:inventory_changed",
        "conditions": {
          "items": [
            {
              "items": "minecraft:book"
            }
          ]
        }
      },
      "has_flesh": {
        "trigger": "minecraft:inventory_changed",
        "conditions": {
          "items": [
            {
              "items": "csrp:assimilated_flesh"
            }
          ]
        }
      }
    },
    "requirements": [
      [
        "has_book",
        "has_flesh"
      ]
    ],
    "rewards": {
      "recipes": [
        "csrp:srp_field_guide"
      ]
    }
  },
  "root.json": {
    "parent": null,
    "icon": "csrp:roots_icon",
    "title": {
      "translate": "advancements.csrp.root.title"
    },
    "description": {
      "translate": "advancements.csrp.root.description"
    },
    "frame": "task",
    "background": "csrp:textures/gui/advancement_wallpaper.png",
    "show_toast": false,
    "announce_to_chat": false,
    "hidden": false,
    "criteria": {
      "tick": {
        "trigger": "minecraft:tick"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "secret_root.json": {
    "parent": "csrp:root",
    "icon": "csrp:the_sign_charm",
    "title": {
      "translate": "advancements.csrp.secret_root.title"
    },
    "description": {
      "translate": "advancements.csrp.secret_root.description"
    },
    "frame": "task",
    "background": null,
    "show_toast": false,
    "announce_to_chat": false,
    "hidden": true,
    "criteria": {
      "tick": {
        "trigger": "minecraft:tick"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "sepeku.json": {
    "parent": "csrp:fermented_arrogance",
    "icon": "csrp:bough",
    "title": {
      "translate": "advancement.csrp.sepeku.title"
    },
    "description": {
      "translate": "advancement.csrp.sepeku.desc"
    },
    "frame": "task",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "done": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "stolas.json": {
    "parent": "csrp:columbus",
    "icon": "csrp:potion_stolas_icon",
    "title": {
      "translate": "advancements.csrp.stolas.title"
    },
    "description": {
      "translate": "advancements.csrp.stolas.description"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "unlock": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "thornshade_self_destruct.json": {
    "parent": "csrp:guerilla",
    "icon": "csrp:self_destruct_icon",
    "title": {
      "translate": "advancement.csrp.thornshade_self_destruct.title"
    },
    "description": {
      "translate": "advancement.csrp.thornshade_self_destruct.desc"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "exploded": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  },
  "tricked_me_did_you.json": {
    "parent": "csrp:stolas",
    "icon": "csrp:book_of_vengeance",
    "title": {
      "translate": "advancements.csrp.tricked_me_did_you.title",
      "color": "red",
      "bold": true
    },
    "description": {
      "translate": "advancements.csrp.tricked_me_did_you.description"
    },
    "frame": "challenge",
    "background": null,
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": true,
    "criteria": {
      "unlock": {
        "trigger": "minecraft:impossible"
      }
    },
    "requirements": null,
    "rewards": null
  }
};;

const DIR = "src/main/resources/data/csrp/advancement";
// Advancements that only exist in this port and have no original counterpart.
const PORT_ONLY = ["again.json", "paojiao_wolf.json"];

const found = [];
const walk = (dir, prefix) => {
  for (const entry of fs.readdirSync(path.join(root, dir), { withFileTypes: true })) {
    const rel = prefix ? prefix + "/" + entry.name : entry.name;
    if (entry.isDirectory()) walk(path.join(dir, entry.name), rel);
    else if (entry.name.endsWith(".json")) found.push(rel);
  }
};
walk(DIR, "");

for (const [rel, expected] of Object.entries(EXPECTED)) {
  const file = DIR + "/" + rel;
  if (!exists(file)) { failures.push("missing advancement file " + rel); continue; }
  let actual;
  try { actual = JSON.parse(read(file)); }
  catch (error) { failures.push(rel + ": invalid JSON (" + error.message + ")"); continue; }

  expect(actual.parent === expected.parent || (!actual.parent && !expected.parent),
    rel + ": parent is " + JSON.stringify(actual.parent) + ", expected " + JSON.stringify(expected.parent));

  const display = actual.display || {};
  const icon = display.icon ? display.icon.id : null;
  expect(icon === expected.icon, rel + ": icon is " + JSON.stringify(icon) + ", expected " + JSON.stringify(expected.icon));
  expect(JSON.stringify(display.title ?? null) === JSON.stringify(expected.title),
    rel + ": title is " + JSON.stringify(display.title ?? null) + ", expected " + JSON.stringify(expected.title));
  expect(JSON.stringify(display.description ?? null) === JSON.stringify(expected.description),
    rel + ": description is " + JSON.stringify(display.description ?? null) + ", expected " + JSON.stringify(expected.description));
  expect((display.frame || null) === expected.frame, rel + ": frame is " + JSON.stringify(display.frame) + ", expected " + JSON.stringify(expected.frame));
  expect((display.background || null) === expected.background,
    rel + ": background is " + JSON.stringify(display.background) + ", expected " + JSON.stringify(expected.background));
  for (const flag of ["show_toast", "announce_to_chat", "hidden"]) {
    const value = display[flag] === undefined ? null : display[flag];
    expect(value === expected[flag], rel + ": " + flag + " is " + JSON.stringify(value) + ", expected " + JSON.stringify(expected[flag]));
  }

  const actualCriteria = actual.criteria || {};
  const expectedNames = Object.keys(expected.criteria);
  const actualNames = Object.keys(actualCriteria);
  expect(actualNames.length === expectedNames.length,
    rel + ": has " + actualNames.length + " criteria, expected " + expectedNames.length);
  for (const name of expectedNames) {
    const want = expected.criteria[name];
    const got = actualCriteria[name];
    if (!got) { failures.push(rel + ": missing criterion " + name); continue; }
    expect(got.trigger === want.trigger,
      rel + "/" + name + ": trigger is " + JSON.stringify(got.trigger) + ", expected " + JSON.stringify(want.trigger));
    const wantConditions = want.conditions === undefined ? null : want.conditions;
    const gotConditions = got.conditions === undefined ? null : got.conditions;
    expect(JSON.stringify(gotConditions) === JSON.stringify(wantConditions),
      rel + "/" + name + ": conditions are " + JSON.stringify(gotConditions) + ", expected " + JSON.stringify(wantConditions));
  }
  for (const name of actualNames) {
    expect(expectedNames.includes(name), rel + ": unexpected criterion " + name);
  }

  expect(JSON.stringify(actual.requirements || null) === JSON.stringify(expected.requirements),
    rel + ": requirements are " + JSON.stringify(actual.requirements || null) + ", expected " + JSON.stringify(expected.requirements));
  expect(JSON.stringify(actual.rewards || null) === JSON.stringify(expected.rewards),
    rel + ": rewards are " + JSON.stringify(actual.rewards || null) + ", expected " + JSON.stringify(expected.rewards));

  if (expected.icon) {
    expect(display.icon && typeof display.icon.id === "string",
      rel + ": icon must use the 1.21 item stack form with an id field");
  }
}

for (const rel of found) {
  expect(EXPECTED[rel] || PORT_ONLY.includes(rel), "unexpected advancement file " + rel);
}
for (const rel of Object.keys(EXPECTED)) {
  expect(found.includes(rel), "advancement file not discovered: " + rel);
}

// The 1.20-era duplicate set and the 1.12 assets copy must stay removed.
expect(!exists("src/main/resources/data/csrp/advancement/csrp"), "legacy duplicate advancement set is back");
expect(!exists("src/main/resources/assets/csrp/advancements"), "legacy assets advancement copy is back");

// Every title and description key must exist in the shipped language files.
for (const language of ["en_us.json", "zh_cn.json"]) {
  const keys = JSON.parse(read("src/main/resources/assets/csrp/lang/" + language));
  for (const [rel, expected] of Object.entries(EXPECTED)) {
    for (const component of [expected.title, expected.description]) {
      if (!component || !component.translate) continue;
      expect(Object.prototype.hasOwnProperty.call(keys, component.translate),
        rel + ": " + language + " is missing " + component.translate);
    }
  }
}

// Every entity type referenced by a kill condition must exist in the mod entity registry.
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const entityIds = new Set();
for (const [, id] of entities.matchAll(/"([a-z0-9_]+)"/g)) entityIds.add(id);
for (const [rel, expected] of Object.entries(EXPECTED)) {
  for (const [name, criterion] of Object.entries(expected.criteria)) {
    const type = criterion.conditions && criterion.conditions.entity && criterion.conditions.entity.type;
    if (!type || !type.startsWith("csrp:")) continue;
    expect(entityIds.has(type.slice(5)), rel + "/" + name + ": condition entity " + type + " is not registered");
  }
}

if (failures.length) {
  console.error("Advancement port verification failed (" + failures.length + "):");
  failures.forEach((failure) => console.error("- " + failure));
  process.exit(1);
}
console.log("Advancement port verification passed (" + Object.keys(EXPECTED).length + " original advancements).");
