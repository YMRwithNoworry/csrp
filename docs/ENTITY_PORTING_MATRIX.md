# Entity Porting Matrix

The authoritative scope is the 119 IDs listed by
`assets/srparasites/bestiary/index.json` in `杂物/SRParasites-1.10.7.jar`.
Unindexed bestiary JSON files and standalone internal effects are excluded.
Projectiles, clouds, summons, and transition entities are included with the
creature that requires them.

Registration/resource baseline: **119 / 119** indexed creatures covered.

| Batch | IDs | Status |
| --- | --- | --- |
| Current | `buglin`, `gnat`, `rupter`, three carriers, `crux`, `crux_incomplete`, twelve `pri_*` forms | 20 ported |
| Crude | `airscrew`, `heed`, `dredge`, `thrall` | 4 ported |
| Early lifecycle | `lice`, `mangler`, `host`, `hostii`, two incomplete forms, `draconite`, `kirin` | 8 ported |
| Assimilated | All listed `sim_*` forms, including walking heads and `movingflesh` dependencies | 22 ported |
| Hijacked and feral | `fer_bear`, `fer_cow`, `fer_enderman`, `fer_horse`, `fer_human`, `fer_pig`, `fer_sheep`, `fer_villager`, `fer_wolf`, `hi_blaze`, `hi_golem`, `hi_skeleton` | 12 ported |
| Marauderized | `mar_bear`, `mar_cow`, `mar_enderman`, `mar_human`, `mar_sheep`, `mar_villager`, `marauder` | 7 ported |
| Primitive | All twelve `pri_*` forms | 12 ported |
| Adapted | All twelve `ada_*` forms | 12 ported |
| Deterrent | `dispatcherten`, `kyphosis`, `seizer`, `sentry`, `worm` | 5 ported |
| Pure | `grunt`, `bomber_light`, `monarch`, `overseer`, `vigilante`, `warden` | 6 ported |
| Preeminent | `bogle`, `carrier_colony`, `haunter`, `bomber_heavy`, `wraith`, `succor` | 6 ported |
| Ancient | `anc_dreadnaut`, `anc_overlord` | 2 ported |
| Nexus and aberrant | Four Beckons, Dispatchers, and Rooters, plus `rooterball`, `abo_bodies`, `abo_head` | 15 ported |

The machine-readable IDs live in `scripts/entity-port-manifest.cjs`.
`scripts/verify-all-entities-port.cjs` checks registration, spawn eggs,
renderers, translations, GeckoLib resources, and loot tables. Passing this
aggregate check establishes the porting baseline only; it does not prove that
every legacy behavior, system interaction, animation, or visual detail has
been reproduced.
