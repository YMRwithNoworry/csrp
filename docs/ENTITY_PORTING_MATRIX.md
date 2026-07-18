# Entity Porting Matrix

The authoritative scope is the 119 IDs listed by
`assets/srparasites/bestiary/index.json` in `杂物/SRParasites-1.10.7.jar`.
Unindexed bestiary JSON files and standalone internal effects are excluded.
Projectiles, clouds, summons, and transition entities are included with the
creature that requires them.

Progress: **50 / 119** indexed creatures ported.

| Batch | IDs | Status |
| --- | --- | --- |
| Current | `buglin`, `gnat`, `rupter`, three carriers, `crux`, `crux_incomplete`, four existing `pri_*` | 12 ported |
| Crude | `airscrew`, `heed`, `dredge`, `thrall` | 4 ported |
| Early lifecycle | `lice`, `mangler`, `host`, `hostii`, two incomplete forms, `draconite`, `kirin` | 8 ported |
| Assimilated | `sim_adventurer` (with `sim_adventurerhead` and `movingflesh` dependencies), `sim_bear`, `sim_cow`, `sim_pig`, `sim_sheep`, `sim_squid`, `sim_wolf` | 7 ported; 15 pending |
| Hijacked and feral | `fer_bear`, `fer_cow`, `fer_enderman`, `fer_horse`, `fer_human`, `fer_pig`, `fer_sheep`, `fer_villager`, `fer_wolf`, `hi_blaze`, `hi_golem`, `hi_skeleton` | 12 ported |
| Marauderized | `mar_bear`, `mar_cow`, `mar_enderman`, `mar_human`, `mar_sheep`, `mar_villager`, `marauder` | 7 ported |
| Primitive | Remaining eight `pri_*` forms | 8 pending |
| Adapted | All twelve `ada_*` forms | 12 pending |
| Pure and preeminent | `dispatcherten` through `succor`, excluding `marauder` | 17 pending |
| Ancient | `anc_dreadnaut`, `anc_overlord` | 2 pending |
| Nexus and aberrant | Four Beckons, Dispatchers, and Rooters, plus `rooterball`, `abo_bodies`, `abo_head` | 15 pending |

The machine-readable IDs live in `scripts/entity-port-manifest.cjs`. A creature
is complete only when its legacy behavior and dependencies are implemented and
`scripts/verify-all-entities-port.cjs` can find its registration, spawn egg,
renderer, translations, GeckoLib resources, and loot table.
