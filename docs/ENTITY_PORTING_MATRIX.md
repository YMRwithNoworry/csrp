# Entity Porting Matrix

The authoritative scope is the 119 IDs listed by
`assets/srparasites/bestiary/index.json` in `杂物/SRParasites-1.10.7.jar`.
Unindexed bestiary JSON files and standalone internal effects are excluded.
Projectiles, clouds, summons, and transition entities are included with the
creature that requires them.

| Batch | IDs | Initial Status |
| --- | --- | --- |
| Current | `buglin`, `gnat`, `rupter`, three carriers, `crux`, `crux_incomplete`, four existing `pri_*` | 12 ported |
| Crude | `airscrew`, `heed`, `dredge`, `thrall` | 4 pending |
| Early lifecycle | `lice`, `mangler`, `host`, `hostii`, two incomplete forms, `draconite`, `kirin` | 8 pending |
| Assimilated | All 22 indexed `sim_*` forms | 22 pending |
| Hijacked and feral | Three `hi_*` and nine `fer_*` forms | 12 pending |
| Marauderized | Six `mar_*` forms and `marauder` | 7 pending |
| Primitive | Remaining eight `pri_*` forms | 8 pending |
| Adapted | All twelve `ada_*` forms | 12 pending |
| Pure and preeminent | `dispatcherten` through `succor`, excluding `marauder` | 17 pending |
| Ancient | `anc_dreadnaut`, `anc_overlord` | 2 pending |
| Nexus and aberrant | Four Beckons, Dispatchers, and Rooters, plus `rooterball`, `abo_bodies`, `abo_head` | 15 pending |

The machine-readable IDs live in `scripts/entity-port-manifest.cjs`. A creature
is complete only when its legacy behavior and dependencies are implemented and
`scripts/verify-all-entities-port.cjs` can find its registration, spawn egg,
renderer, translations, GeckoLib resources, and loot table.
