# Entity Porting Matrix

The authoritative scope is all 127 `CreateEntityMob` registrations in
`SRPEntities.java` from SRParasites 1.10.7. This includes the eight creatures
that are absent from the bestiary index.
Projectiles, clouds, summons, and transition entities are included with the
creature that requires them.

Registration/resource baseline: **127 / 127** registered creatures covered.

| Batch | IDs | Status |
| --- | --- | --- |
| Current | `buglin`, `gnat`, `rupter`, four carriers, `crux`, `crux_incomplete`, twelve `pri_*` forms | 21 ported |
| Crude | `airscrew`, `heed`, `dredge`, `thrall` | 4 ported |
| Early lifecycle | `lice`, `mangler`, `host`, `hostii`, two incomplete forms, `draconite`, `kirin`, `movingflesh`, `worker` | 10 ported |
| Assimilated | All registered `sim_*` forms, including the unindexed adventurer head | 23 ported |
| Hijacked and feral | `fer_bear`, `fer_cow`, `fer_enderman`, `fer_horse`, `fer_human`, `fer_pig`, `fer_sheep`, `fer_villager`, `fer_wolf`, `hi_blaze`, `hi_golem`, `hi_skeleton` | 12 ported |
| Marauderized | `mar_bear`, `mar_cow`, `mar_enderman`, `mar_human`, `mar_sheep`, `mar_villager`, `marauder` | 7 ported |
| Primitive | All twelve `pri_*` forms | 12 ported |
| Adapted | All twelve `ada_*` forms | 12 ported |
| Deterrent | `dispatcherten`, `kyphosis`, `seizer`, `sentry`, `worm` | 5 ported |
| Pure | `grunt`, `bomber_light`, `monarch`, `overseer`, `vigilante`, `warden`, `seeker`, `architect` | 8 ported |
| Preeminent | `bogle`, `carrier_colony`, `haunter`, `bomber_heavy`, `wraith`, `succor` | 6 ported |
| Ancient | `anc_dreadnaut`, `anc_overlord`, `anc_pod`, `anc_dreadnaut_ten` | 4 ported |
| Nexus and aberrant | Four Beckons, Dispatchers, and Rooters, plus `rooterball`, `abo_bodies`, `abo_head` | 15 ported |

The machine-readable 127-ID registration baseline lives in `scripts/entity-port-manifest.cjs`.
`scripts/verify-all-entities-port.cjs` checks registration, spawn eggs,
renderers, translations, GeckoLib resources, and loot tables. Passing this
aggregate check establishes the porting baseline only; it does not prove that
every legacy behavior, system interaction, animation, or visual detail has
been reproduced.

`carrier_worm` and `seeker` deliberately use no-op renderers because the
legacy client registers no renderer for `EntityQuac` or `EntitySoo`.
