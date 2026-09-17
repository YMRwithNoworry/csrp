# csrp 1.20.1 Forge — 原模组 100% 移植差距报告

由 `_scratch/parity/mkgap.py` 生成，数据源：
- 目标工程 `csrp-1.20.1-forge`（Forge 47.4.23 / MC 1.20.1 / Java 17 release，GraalVM 21 编译）
- 捐赠工程 `csrp-26.3`（同一 `alku.csrp` 血统、移植进度更靠前的分支）
- 原模组 SRParasites 1.10.9 jar（1.12.2 Forge）

## 1. 注册内容差距（相对捐赠分支）

### 方块 `block.csrp.` — 目标 167 / 捐赠 215，缺 48

```
brusewood_door                      brusewood_trapdoor                  canisteractive                      consumed_door                       consumed_fence                      consumed_trapdoor
consumed_workbench                  cooked_flesh_door                   cooked_flesh_trapdoor               deadblood                           deadhead_fence                      flesh_door
flesh_fence                         flesh_trapdoor                      goth_door                           goth_fence                          goth_stem                           goth_trapdoor
infested_door                       infested_fence                      infested_trapdoor                   infested_workbench                  parasiterubble_bone                 parasiterubble_flesh
parasiterubble_stone                parasiterubble_weathb               parasiterubble_weathbc              parasiterubble_weathfs              parasiterubbledense                 parasiterubbledense_biome
parasiterubbledense_colony          parasiterubbledense_heart           parasitesapling_flowertall          parasitesapling_tree                parasitesapling_treethin            parasitestain_dirt
parasitestain_feeler                parasitestain_flesh                 parasitestain_mud                   parasitestain_red                   parasitestain_sackflesh             parasitestain_spore
parasitethin                        parasitethin_treebase               parasitethin_treenesw               parasitetrunk                       parasitetrunk_ball                  parasitetrunk_plant
```

### 物品 `item.csrp.` — 目标 295 / 捐赠 441，缺 146

```
axe                                 axe_sentient                        boots                               boots_sentient                      bow                                 bow_sentient
chest                               chest_sentient                      cleaver                             cleaver_sentient                    helm                                helm_sentient
itemmobspawner_abobodies            itemmobspawner_abohead              itemmobspawner_alafha               itemmobspawner_anged                itemmobspawner_ata                  itemmobspawner_bano
itemmobspawner_banoadapted          itemmobspawner_buthol               itemmobspawner_canra                itemmobspawner_canraadapted         itemmobspawner_cruxa                itemmobspawner_cruxb
itemmobspawner_dod                  itemmobspawner_dodsii               itemmobspawner_dodsiii              itemmobspawner_dodsiv               itemmobspawner_done                 itemmobspawner_dorpa
itemmobspawner_elvia                itemmobspawner_emana                itemmobspawner_emanaadapted         itemmobspawner_esor                 itemmobspawner_ferbear              itemmobspawner_fercow
itemmobspawner_ferenderman          itemmobspawner_ferhorse             itemmobspawner_ferhuman             itemmobspawner_ferpig               itemmobspawner_fersheep             itemmobspawner_fervillager
itemmobspawner_ferwolf              itemmobspawner_flog                 itemmobspawner_ganro                itemmobspawner_gim                  itemmobspawner_gimadapted           itemmobspawner_gothol
itemmobspawner_heblu                itemmobspawner_heed                 itemmobspawner_hiblaze              itemmobspawner_higolem              itemmobspawner_hiskeleton           itemmobspawner_host
itemmobspawner_hostii               itemmobspawner_hull                 itemmobspawner_hulladapted          itemmobspawner_iki                  itemmobspawner_ikiadapted           itemmobspawner_infbear
itemmobspawner_infcow               itemmobspawner_infcowhead           itemmobspawner_infdragone           itemmobspawner_infdragonehead       itemmobspawner_infenderman          itemmobspawner_infendermanhead
itemmobspawner_infhorse             itemmobspawner_infhorsehead         itemmobspawner_infhuman             itemmobspawner_infhumanhead         itemmobspawner_infpig               itemmobspawner_infpighead
itemmobspawner_infplayer            itemmobspawner_infplayerhead        itemmobspawner_infsheep             itemmobspawner_infsheephead         itemmobspawner_infsquid             itemmobspawner_infvillager
itemmobspawner_infvillagerhead      itemmobspawner_infwolf              itemmobspawner_infwolfhead          itemmobspawner_inhoom               itemmobspawner_inhoos               itemmobspawner_jinjo
itemmobspawner_kirin                itemmobspawner_leem                 itemmobspawner_leemsii              itemmobspawner_leemsiii             itemmobspawner_leemsiv              itemmobspawner_leer
itemmobspawner_lencia               itemmobspawner_lesh                 itemmobspawner_lodo                 itemmobspawner_lum                  itemmobspawner_lumadapted           itemmobspawner_mar
itemmobspawner_marbear              itemmobspawner_marcow               itemmobspawner_marenderman          itemmobspawner_marhuman             itemmobspawner_marsheep             itemmobspawner_marvillager
itemmobspawner_mes                  itemmobspawner_mudo                 itemmobspawner_nak                  itemmobspawner_nogla                itemmobspawner_noglaadapted         itemmobspawner_nuuh
itemmobspawner_omboo                itemmobspawner_orch                 itemmobspawner_oronco               itemmobspawner_pheon                itemmobspawner_pod                  itemmobspawner_quac
itemmobspawner_ranrac               itemmobspawner_ranracadapted        itemmobspawner_rathol               itemmobspawner_shyco                itemmobspawner_shycoadapted         itemmobspawner_terla
itemmobspawner_tonro                itemmobspawner_unvo                 itemmobspawner_venkrol              itemmobspawner_venkrolsii           itemmobspawner_venkrolsiii          itemmobspawner_venkrolsiv
itemmobspawner_venkrolsv            itemmobspawner_vesta                itemmobspawner_wymo                 itemmobspawner_wymoadapted          itemmobspawner_zaa                  itemmobspawner_zaaadapted
lance                               lance_sentient                      lurecomponent10                     lurecomponent7                      lurecomponent8                      lurecomponent9
maul                                maul_sentient                       pants                               pants_sentient                      scythe                              scythe_sentient
sword                               sword_sentient
```

### 实体 `entity.csrp.` — 目标 140 / 捐赠 140，缺 0


### 效果 `effect.csrp.` — 目标 44 / 捐赠 44，缺 0


### 生物群系 `biome.csrp.` — 目标 0 / 捐赠 4，缺 4

```
srp_boils                           srp_demen                           srp_harlequinn                      srp_shrouded
```

## 2. 方块保真度：目标工程有 174 个"近似占位"方块，捐赠分支已实现其中 65 个

### 需要按捐赠分支重写为真实实现的 65 个 id

```
brusewood_door                        brusewood_trapdoor                    canisteractive                        consumed_door
consumed_fence                        consumed_trapdoor                     consumed_workbench                    deadhead_fence
flesh_fence                           goth_door                             goth_fence                            goth_stem
infested_cobblestone_slab             infested_dirt_slab                    infested_fence                        infested_plank_slab
infested_polished_stone_bricks_stairs  infested_sandstone_slab               infested_sandstone_stairs             infested_stone_brick_slab
infested_stone_bricks_stairs          infested_stone_slab                   infested_stone_stairs                 infested_terracotta_slab
infested_workbench                    node_relay                            parasiterubble_bone                   parasiterubble_bonestairs
parasiterubble_bricks                 parasiterubble_bricks_wall            parasiterubble_bricksstairs           parasiterubble_flesh
parasiterubble_flesh_wall             parasiterubble_fleshstairs            parasiterubble_fungus                 parasiterubble_fungusstairs
parasiterubble_metal                  parasiterubble_metal_wall             parasiterubble_metalstairs            parasiterubble_obsidian
parasiterubble_obsidianstairs         parasiterubble_stone                  parasiterubble_stonedebrisstairs      parasiterubble_stonestairs
parasiterubble_weathb_wall            parasiterubble_weathbc_wall           parasiterubble_weathfs_wall           parasiterubble_wood
parasiterubble_woodstairs             parasiterubbledense                   parasiterubbledense_biome_wall        parasiterubbledense_biomestairs
parasiterubbledense_colony_wall       parasiterubbledense_colonystairs      parasiterubbledense_wallstairs        parasitestain_dirt
parasitestain_flesh                   parasitethin                          parasitetrunk                         parasitetrunk_ballstairs
parasitetrunk_plantstairs             parasitetrunk_treestairs              polished_infested_stone_slab          residue_brick_slab
residue_stairs
```

### 两个分支都仍是占位实现（105 个，需要回到原模组源码实现）

```
assimilated_blossom                 bloodyice                           bruisewood_fence                    bruisewood_plank_slab               bruisewood_plank_slab_double        bruisewood_plank_stairs
bruisewood_plank_wall               colonyoutpost                       consumed_plank_slab                 consumed_plank_slab_double          consumed_plank_wall                 consumed_planks_stairs
consumed_pot                        cooked_flesh_slab_double            dead_head_plank_slab                dead_head_plank_slab_double         deadhead_plank_stairs               dermoid_cyst
dispatchern                         epitome_infestation_warp_diffuser   flesh_slab                          flesh_slab_double                   flesh_stairs                        frost_weathered_stone_slab
frost_weathered_stone_slab_double   frost_weathered_stone_stairs        goreada                             gorefer                             goremar                             gorepri
gorepur                             goresim                             goth_plank_slab                     goth_plank_slab_double              goth_plank_wall                     goth_planks_stairs
harlequinn_grass                    harleskinn_fence                    harleskinn_slab                     harleskinn_slab_double              harleskinn_stairs                   hirsute_hair
infested_cactus                     infested_cobblestone_slab_double    infested_dirt_slab_double           infested_furnace                    infested_furnace_lit                infested_leaves
infested_leaves_fast                infested_plank_slab_double          infested_pot                        infested_sandstone_slab_double      infested_stone_brick_slab_double    infested_stone_slab_double
infested_terracotta_slab_double     infestedremain                      infestedrubblestairs                infestedstainstairs                 infestedtrunkstairs                 lipoma_mass
locs_block_slab                     locs_block_slab_double              parasite_barrier                    parasitebush                        parasitecanister                    parasitecanister_bag_wall
parasitefog                         parasiteplank                       parasiteplank_deadhead_wall         parasiterubble                      parasiterubbleslabdouble            parasiterubbleslabhalf
parasitesapling                     parasitestain                       parasitestain_dirtstairs            parasitestain_feelerstairs          parasitestain_flesh_wall            parasitestain_fleshstairs
parasitestain_mudstairs             parasitestainslabdouble             parasitestainslabhalf               parasitetendril                     parasitic_colony_core_slab          parasitic_colony_core_slab_double
parasitic_compressed_colony_stone_slab  parasitic_compressed_colony_stone_slab_double  poland_skin_slab                    poland_skin_slab_double             polished_infested_stone_slab_double  potted_assimilated_blossom
potted_consumed_assimilated_blossom  reinforced_hivestone_slab           reinforced_hivestone_slab_double    relay_controller_dummy              relaycontroller                     residue_brick_slab_double
sac_of_flesh_slab                   sac_of_flesh_slab_double            tresses_hair                        weathered_bricks_slab               weathered_bricks_slab_double        weathered_cobblestone_slab
weathered_cobblestone_slab_double   wheathered_bricks_stairs            wheathered_cobblestone_stairs
```

## 3. 资源差距

| 类别 | 目标 1.20.1 | 捐赠 26.3 | 差距 |
| --- | --- | --- | --- |
| GeckoLib 模型 `assets/csrp/geo` | 127 | 136 | 9 |
| GeckoLib 动画 `assets/csrp/animations` | 130 | 139 | 9 |
| 模型 JSON `assets/csrp/models` | 783 | 1719 | 936 |
| 纹理 `assets/csrp/textures` | 1247 | 2313 | 1066 |
| 音效 `assets/csrp/sounds` | 1007 | 1009 | 2 |
| 语言文件 `assets/csrp/lang` | 4 | 37 | 33 |
| 配方 `data/csrp/recipes` | 199 | 0 | -199 |
| 配方 `data/csrp/recipe` | 0 | 329 | 329 |
| 进度 `data/csrp/advancement` | 43 | 83 | 40 |
| 进度 `data/csrp/advancements` | 0 | 0 | 0 |
| 战利品表 `data/csrp/loot_tables` | 272 | 0 | -272 |
| 战利品表 `data/csrp/loot_table` | 0 | 315 | 315 |
| 结构 NBT `data/csrp/structures` | 10 | 51 | 41 |
| 世界生成 `data/csrp/worldgen` | 0 | 4 | 4 |

### 结构 NBT：原模组 55 个，目标工程 10 个，缺 45

```
ball.nbt                          ballbig.nbt                       beckon_2x2_1.nbt                  beckon_3x3_1.nbt                  beckon_3x3_2.nbt
beckon_3x3_3.nbt                  beckon_3x3_4.nbt                  beckon_3x3_5.nbt                  beckon_3x4_1.nbt                  beckon_4x4_1.nbt
beckon_4x4_2.nbt                  beckon_5x5_1.nbt                  box.nbt                           deadhead_tree_large_1.nbt         deadhead_tree_large_2.nbt
deadhead_tree_large_3.nbt         deadhead_tree_large_4.nbt         dh_village_blacksmith.nbt         dh_village_church.nbt             dh_village_farm.nbt
dh_village_medium_house.nbt       dh_village_medium_house1.nbt      dh_village_small1.nbt             dh_village_small2.nbt             dh_village_well.nbt
harlequin_bush_00.nbt             harlequin_bush_01.nbt             harlequin_bush_02.nbt             harlequin_rock_01.nbt             harlequin_rock_02.nbt
harlequin_rock_03.nbt             harlequin_rock_04.nbt             harlequin_rock_05.nbt             harlequin_rock_06.nbt             harlequin_rock_07.nbt
harlequin_ruin_01.nbt             harlequin_ruin_02.nbt             harlequin_ruin_03.nbt             harlequin_tree_01.nbt             harlequin_tree_02.nbt
harlequin_tree_03.nbt             harlequin_tree_04.nbt             harlequin_tree_05.nbt             harlequin_tree_06.nbt             harlequin_tree_07.nbt
```

### 配方：目标 199，捐赠 329，缺 130

```
ada_vemin_to_slimeball                    ada_viscera_to_string                     assimilated_blossom                       assimilated_blossom_potted
assimilated_blossom_potted_infested       assimilated_reed_to_paper                 assimilated_reed_to_sticks                assimilated_reed_to_sugar
bowls_from_parasite_wood                  bruisewood_fence                          bruisewood_plank_slab_from_brusewood_planks  bruisewood_plank_stairs
bruisewood_plank_wall                     brusewood_door                            brusewood_planks_from_parasitetrunk       brusewood_trapdoor
canisterbag                               chest_from_parasite_wood                  consumed_door                             consumed_fence
consumed_plank_slab_from_consumed_planks  consumed_plank_wall                       consumed_planks_from_parasiterubble       consumed_planks_stairs
consumed_pot                              consumed_trapdoor                         consumed_workbench                        cooked_flesh_fence_from_cooked_flesh_planks
cooked_flesh_planks_from_cooked_flesh     cooked_flesh_slab_from_cooked_flesh_planks  cooked_flesh_stairs_from_cooked_flesh_planks  dead_head_plank_slab_from_parasiteplank
deadhead_fence                            deadhead_plank_stairs                     dermoid_cyst                              flesh_fence_from_flesh_planks
flesh_planks_from_parasitestain           flesh_slab_from_flesh_planks              flesh_stairs_from_flesh_planks            fortified_bone_block
frost_weathered_stone_slab_from_parasiterubble_13  frost_weathered_stone_stairs              goth_door                                 goth_fence
goth_plank_slab_from_goth_planks          goth_plank_wall                           goth_planks_stairs                        gothshroom_to_purple_dye
harleskinn_fence                          harleskinn_slab_from_harleskinn_block     harleskinn_stairs                         infested_fence
infested_furnace                          infested_furnace_cobblestone              infested_pot                              infested_trunk_to_planks
infested_workbench                        locks_block_slab_from_locs_block          lureblock                                 lureblock2
lureblock3                                lureblock4                                lureblock5                                lureblock6
module_vector                             parasitebush_from_glass_and_planks        parasitecanister_bag_wall                 parasiteplank_deadhead_wall
parasiteplank_from_parasitetrunk_variant4  parasiterubble_13_to_9_bricks             parasiterubble_bonestairs                 parasiterubble_bricks_wall
parasiterubble_bricksstairs               parasiterubble_flesh_wall                 parasiterubble_fleshstairs                parasiterubble_fungusstairs
parasiterubble_metal_wall                 parasiterubble_metalstairs                parasiterubble_obsidianstairs             parasiterubble_stonedebrisstairs
parasiterubble_stonestairs                parasiterubble_weathb_wall                parasiterubble_weathbc_wall               parasiterubble_weathfs_wall
parasiterubble_woodstairs                 parasiterubbledense_biome_wall            parasiterubbledense_biomestairs           parasiterubbledense_colony_wall
parasiterubbledense_colonystairs          parasiterubbledense_wallstairs            parasitestain_dirtstairs                  parasitestain_feelerstairs
parasitestain_flesh_wall                  parasitestain_fleshstairs                 parasitestain_from_parasitetrunk_2        parasitestain_mudstairs
parasitetrunk_ballstairs                  parasitetrunk_plantstairs                 parasitetrunk_treestairs                  parasitic_colony_core_slab_from_colonyheart
parasitic_compressed_colony_stone_slab_from_parasiterubbledense_2  poland_skin_slab_from_poland_skin_block   purifierblock                             reinforced_hivestone_slab_from_parasiterubbledense_1
sac_of_flesh_slab_from_parasitecanister   slab_parasiterubble_0                     slab_parasiterubble_1                     slab_parasiterubble_2
slab_parasiterubble_3                     slab_parasiterubble_4                     slab_parasiterubble_5                     slab_parasiterubble_6
slab_parasiterubble_7                     slab_parasiterubble_8                     slab_parasitestain_0                      slab_parasitestain_1
slab_parasitestain_2                      slab_parasitestain_3                      slab_parasitestain_4                      slab_parasitestain_5
slab_parasitestain_6                      smelt_bloody_bone                         smelt_bloody_iron_ingot                   smelt_bloody_rod
smelt_infested_cobblestone                sticks_from_parasite_wood                 weathered_bricks_slab_from_parasiterubble_9  weathered_cobblestone_slab_from_parasiterubble_11
wheathered_bricks_stairs                  wheathered_cobblestone_stairs
```

### 进度：目标 43，捐赠 83，缺 40

```
brew_fear                                 columbus                                  controversial                             cosmic_structural_failure
dark_days                                 enemy_enemy                               fermented_arrogance                       firmamentum_root
fog_nullifier                             guerilla                                  hellfire_chemical_warfare                 hunt_season
kill_adapted                              kill_assimilated                          kill_assimilated_head                     kill_beckon_s1
kill_beckon_s2                            kill_beckon_s3                            kill_beckon_s4                            kill_crude
kill_deterrent                            kill_dispatcher_s1                        kill_dispatcher_s2                        kill_dispatcher_s3
kill_dispatcher_s4                        kill_inborn                               kill_primitive                            kill_pure
kill_root                                 monument                                  monument_root                             peacekeeper
qliphoth_overload                         recipes/misc/assimilated_jack_o_lantern   recipes/misc/gothshroom_to_purple_dye     recipes/misc/srp_field_guide
root                                      secret_root                               stolas                                    tricked_me_did_you
```

### 纹理：目标 1247，捐赠 2313，缺 1068

```
block/brusewood_door_bottom.png             block/brusewood_door_top.png                block/brusewood_trapdoor.png                block/consumed_door_bottom.png              block/consumed_door_top.png
block/consumed_trapdoor.png                 block/consumed_workbench_front.png          block/consumed_workbench_side.png           block/consumed_workbench_top.png            block/cooked_flesh_door_bottom.png
block/cooked_flesh_door_top.png             block/cooked_flesh_trapdoor.png             block/flesh_door_bottom.png                 block/flesh_door_top.png                    block/flesh_trapdoor.png
block/goth_door_bottom.png                  block/goth_door_top.png                     block/goth_stem.png                         block/goth_stem_top.png                     block/goth_trapdoor.png
block/infested_door_bottom.png              block/infested_door_top.png                 block/infested_trapdoor.png                 block/infested_workbench_front.png          block/infested_workbench_side.png
block/infested_workbench_top.png            block/parasiterubble_bone.png               block/parasiterubble_flesh.png              block/parasiterubble_stone.png              block/parasiterubble_weathb.png
block/parasiterubble_weathbc.png            block/parasiterubble_weathfs.png            block/parasiterubbledense.png               block/parasiterubbledense_biome.png         block/parasiterubbledense_colony.png
block/parasitesapling_flowertall.png        block/parasitesapling_tree.png              block/parasitesapling_treethin.png          block/parasitestain_dirt.png                block/parasitestain_feeler.png
block/parasitestain_mud.png                 block/parasitethin_treebase.png             block/parasitethin_treenesw.png             block/parasitetrunk.png                     block/parasitetrunk_ball.png
block/parasitetrunk_ball_top.png            block/parasitetrunk_plant.png               block/parasitetrunk_plant_top.png           block/parasitetrunk_top.png                 blocks/alveoli_block.png
blocks/alveoli_growth.png                   blocks/arraytowertexture.png                blocks/arraytowertexture.png.mcmeta         blocks/arraytowertextureon.png              blocks/arraytowertextureon.png.mcmeta
blocks/ashen_glass.png                      blocks/ashen_glass_pane.png                 blocks/assimilated_flower_pot.png           blocks/assimilated_fungus.png               blocks/assimilated_pumpkin_front.png
blocks/assimilated_pumpkin_front_lit.png    blocks/assimilated_pumpkin_side.png         blocks/assimilated_pumpkin_top.png          blocks/assimilated_reed.png                 blocks/biomass_block.png
blocks/biomeheart.png                       blocks/biomeheart.png.mcmeta                blocks/biomepurifier_down.png               blocks/biomepurifier_side.png               blocks/biomepurifier_up.png
blocks/bleeding_obsidian.png                blocks/bloody_glass.png                     blocks/bloody_glass_pane.png                blocks/bloody_ice.png                       blocks/bloody_snow.png
blocks/bone_double.png                      blocks/border.png                           blocks/border_0.png                         blocks/border_1.png                         blocks/border_2.png
blocks/border_3.png                         blocks/border_4.png                         blocks/border_5.png                         blocks/bricks.png                           blocks/brucewood_planks.png
blocks/bruisewood_bookshelf.png             blocks/bruisewood_door_bottom.png           blocks/bruisewood_door_top.png              blocks/bruisewood_ladder.png                blocks/brusewood_trapdoor.png
blocks/colony_bricks_chiseled.png           blocks/consumed_bookshelf.png               blocks/consumed_crafting_table_front.png    blocks/consumed_crafting_table_side.png     blocks/consumed_crafting_table_top.png
blocks/consumed_door_bottom.png             blocks/consumed_door_top.png                blocks/consumed_flower_pot.png              blocks/consumed_ladder.png                  blocks/consumed_planks.png
blocks/consumed_trapdoor.png                blocks/consumed_wood.png                    blocks/consumed_wood_side.png               blocks/cooked_flesh_bookshelf.png           blocks/cooked_flesh_ladder.png
blocks/cooked_flesh_planks.png              blocks/cookedflesh.png                      blocks/cookedflesh_door_bottom.png          blocks/cookedflesh_door_top.png             blocks/cookedflesh_door_top_eyed.png
blocks/cookedflesh_trapdoor.png             blocks/dead_ts.png                          blocks/dead_ts_snow.png                     blocks/deadblood_flowing.png                blocks/deadblood_flowing.png.mcmeta
blocks/deadblood_still.png                  blocks/deadblood_still.png.mcmeta           blocks/deadhead_bookshelf.png               blocks/deadhead_ladder.png                  blocks/deadhead_leaves.png
blocks/deadhead_leaves_snow.png             blocks/deadhead_leaves_snow_top.png         blocks/dermoid_cyst_back.png                blocks/dermoid_cyst_bottom.png              blocks/dermoid_cyst_front.png
blocks/dermoid_cyst_front_happy.png         blocks/dermoid_cyst_front_open.png          blocks/dermoid_cyst_front_open_happy.png    blocks/dermoid_cyst_side.png                blocks/dermoid_cyst_side_alt.png
blocks/dermoid_cyst_side_alt_happy.png      blocks/dermoid_cyst_side_happy.png          blocks/dermoid_cyst_top.png                 blocks/diseased_sponge.png                  blocks/dodn_down.png
blocks/dodn_side.png                        blocks/dodn_up.png                          blocks/epitome_infestation_warp_diffuser.png  blocks/esca_bulb.png                        blocks/esca_bulb.png.mcmeta
blocks/esca_bulb_black.png                  blocks/esca_bulb_black.png.mcmeta           blocks/esca_bulb_blue.png                   blocks/esca_bulb_blue.png.mcmeta            blocks/esca_bulb_brown.png
blocks/esca_bulb_brown.png.mcmeta           blocks/esca_bulb_cyan.png                   blocks/esca_bulb_cyan.png.mcmeta            blocks/esca_bulb_gray.png                   blocks/esca_bulb_gray.png.mcmeta
blocks/esca_bulb_green.png                  blocks/esca_bulb_green.png.mcmeta           blocks/esca_bulb_light_blue.png             blocks/esca_bulb_light_blue.png.mcmeta      blocks/esca_bulb_light_gray.png
blocks/esca_bulb_light_gray.png.mcmeta      blocks/esca_bulb_lime.png                   blocks/esca_bulb_lime.png.mcmeta            blocks/esca_bulb_magenta.png                blocks/esca_bulb_magenta.png.mcmeta
blocks/esca_bulb_orange.png                 blocks/esca_bulb_orange.png.mcmeta          blocks/esca_bulb_pink.png                   blocks/esca_bulb_pink.png.mcmeta            blocks/esca_bulb_purple.png
blocks/esca_bulb_purple.png.mcmeta          blocks/esca_bulb_red.png                    blocks/esca_bulb_red.png.mcmeta             blocks/esca_bulb_white.png                  blocks/esca_bulb_white.png.mcmeta
blocks/esca_bulb_yellow.png                 blocks/esca_bulb_yellow.png.mcmeta          blocks/flesh_bookshelf.png                  blocks/flesh_door_bottom.png                blocks/flesh_door_top.png
blocks/flesh_door_top_eyed.png              blocks/flesh_ladder.png                     blocks/flesh_planks.png                     blocks/flesh_trapdoor.png                   blocks/flowers/assimilated_blossom.png
blocks/flowers/parasitebush_flower1.png     blocks/fog_nullifier.png                    blocks/goth_bookshelf.png                   blocks/goth_door_bottom.png                 blocks/goth_door_top.png
blocks/goth_ladder.png                      blocks/goth_planks.png                      blocks/goth_stem.png                        blocks/goth_stem_top.png                    blocks/goth_trapdoor.png
blocks/gothshroom.png                       blocks/group_shroom.png                     blocks/hair.png                             blocks/hair_block.png                       blocks/hair_block_snowy.png
blocks/hair_follicle.png                    blocks/hair_follicle_side.png               blocks/hairy_skin_block.png                 blocks/hairy_skin_block_side.png            blocks/halflife3.png
blocks/harlequinn_glass.png                 blocks/harlequinn_glass_pane.png            blocks/harleskinn_block.png                 blocks/hivesteel.png                        blocks/hivestone_debris.png
blocks/hivestone_debris_side.png            blocks/inf_ss.png                           blocks/inf_ss_chiseled.png                  blocks/inf_ss_cut.png                       blocks/inf_ss_down.png
blocks/inf_ss_top.png                       blocks/infested_bookshelf.png               blocks/infested_bush.png                    blocks/infested_bush_end.png                blocks/infested_cactus.png
blocks/infested_cactus_bottom.png           blocks/infested_cactus_maw.png              blocks/infested_cactus_top.png              blocks/infested_cactus_top_maw.png          blocks/infested_cobblestone.png
blocks/infested_cobblestone_snow.png        blocks/infested_cobblestone_stairs.png      blocks/infested_column_side.png             blocks/infested_column_top.png              blocks/infested_crafting_table_front.png
blocks/infested_crafting_table_side.png     blocks/infested_crafting_table_top.png      blocks/infested_door_bottom.png             blocks/infested_door_top.png                blocks/infested_furnace_front.png
blocks/infested_furnace_front_lit.png       blocks/infested_furnace_side.png            blocks/infested_furnace_top.png             blocks/infested_glass.png                   blocks/infested_glass.png.mcmeta
blocks/infested_glass_pane.png              blocks/infested_glass_pane.png.mcmeta       blocks/infested_ladder.png                  blocks/infested_leaves.png                  blocks/infested_leaves_opaque.png
blocks/infested_leaves_opaque_snowy.png     blocks/infested_leaves_snowy.png            blocks/infested_planks.png                  blocks/infested_stone.png                   blocks/infested_stone_bricks.png
blocks/infested_stone_polished.png          blocks/infested_terracotta.png              blocks/infested_trapdoor.png                blocks/infestedore_co.png                   blocks/infestedore_dia.png
blocks/infestedore_eme.png                  blocks/infestedore_gol.png                  blocks/infestedore_iro.png                  blocks/infestedore_lap.png                  blocks/infestedore_red.png
blocks/infestedore_un.png                   blocks/infestedrubble.png                   blocks/infestedsand.png                     blocks/infestedsand_down.png                blocks/infestedsand_snow.png
blocks/infestedstain.png                    blocks/infestedstain_snow.png               blocks/infestedtrunk.png                    blocks/infestedtrunk_side.png               blocks/infestremain.png
blocks/infestremain_infestedvariant.png     blocks/infuser_furnace_bottom.png           blocks/infuser_furnace_front.png            blocks/infuser_furnace_front_lit.png        blocks/infuser_furnace_side.png
blocks/infuser_furnace_top.png              blocks/lure1.png                            blocks/lure10.png                           blocks/lure10.png.mcmeta                    blocks/lure2.png
blocks/lure3.png                            blocks/lure4.png                            blocks/lure5.png                            blocks/lure6.png                            blocks/lure7.png
blocks/lure8.png                            blocks/lure9.png                            blocks/moody_glass.png                      blocks/moody_glass_pane.png                 blocks/node_lamp_on_1.png
blocks/node_lamp_on_2.png                   blocks/node_lamp_on_3.png                   blocks/node_lamp_on_4.png                   blocks/node_lamp_on_5.png                   blocks/node_lamp_on_5.png.mcmeta
blocks/node_redstone_lamp.png               blocks/oldbiomepurifier_down.png            blocks/oldbiomepurifier_side.png            blocks/oldbiomepurifier_up.png              blocks/parasite_barrier.png
blocks/parasite_fog.png                     blocks/parasite_fog.png.mcmeta              blocks/parasite_lump_new.png                blocks/parasite_sac_new.png                 blocks/parasite_snow.png
blocks/parasitebush_arc.png                 blocks/parasitebush_arc_node.png            blocks/parasitebush_bine.png                blocks/parasitebush_bine_end.png            blocks/parasitebush_bine_top.png
blocks/parasitebush_decanter.png            blocks/parasitebush_decanterempty.png       blocks/parasitebush_eye.png                 blocks/parasitebush_eye.png.mcmeta          blocks/parasitebush_flower1.png
blocks/parasitebush_flower1_node.png        blocks/parasitebush_frostg1.png             blocks/parasitebush_frostg2.png             blocks/parasitebush_frostg3.png             blocks/parasitebush_frostg4.png
blocks/parasitebush_frostgt1.png            blocks/parasitebush_frostgt2.png            blocks/parasitebush_frostgt3.png            blocks/parasitebush_grass1.png              blocks/parasitebush_grass12.png
blocks/parasitebush_grass12_node.png        blocks/parasitebush_grass12_node_node.png   blocks/parasitebush_grass1_node.png         blocks/parasitebush_grass1_node_node.png    blocks/parasitebush_infected.png
blocks/parasitebush_infected_node.png       blocks/parasitebush_pop.png                 blocks/parasitebush_spine.png               blocks/parasitebush_spine2.png              blocks/parasitebush_spine2_end.png
blocks/parasitebush_spine2_end_node.png     blocks/parasitebush_spine2_node.png         blocks/parasitebush_spine_end.png           blocks/parasitebush_spine_end_node.png      blocks/parasitebush_spine_node.png
blocks/parasitebush_tendril.png             blocks/parasitebush_tendril_bottom.png      blocks/parasitebush_tendril_end.png         blocks/parasitebush_thorn.png               blocks/parasitebush_thorn_snow.png
blocks/parasitebush_thorndead.png           blocks/parasitebush_thorndead_snow.png      blocks/parasitebush_thorndormat.png         blocks/parasitebush_thorndormat_snow.png    blocks/parasitebush_thorntwo.png
blocks/parasitebush_thorntwo_snow.png       blocks/parasitebush_tooh.png                blocks/parasitebush_tooh_node.png           blocks/parasitebush_vine.png                blocks/parasitebush_vine2.png
blocks/parasitebush_vine2_end.png           blocks/parasitebush_vine2_end_node.png      blocks/parasitebush_vine2_node.png          blocks/parasitebush_vine_end.png            blocks/parasitebush_vine_end_node.png
blocks/parasitebush_vine_node.png           blocks/parasitecanister_bag_down.png        blocks/parasitecanister_bag_side.png        blocks/parasitecanister_bag_up.png          blocks/parasitecanister_cyst.png
blocks/parasitecanister_cyst_down.png       blocks/parasitecanister_cyst_side.png       blocks/parasitecanister_cyst_up.png         blocks/parasitecanister_cysta.png           blocks/parasitecanister_sac.png
blocks/parasitegore_ada_big.png             blocks/parasitegore_ada_flat.png            blocks/parasitegore_ada_small.png           blocks/parasitegore_fer_big.png             blocks/parasitegore_fer_flat.png
blocks/parasitegore_fer_small.png           blocks/parasitegore_mar_big.png             blocks/parasitegore_mar_flat.png            blocks/parasitegore_mar_small.png           blocks/parasitegore_pri_big.png
blocks/parasitegore_pri_flat.png            blocks/parasitegore_pri_small.png           blocks/parasitegore_pure_big.png            blocks/parasitegore_pure_flat.png           blocks/parasitegore_pure_small.png
blocks/parasitegore_sim_big.png             blocks/parasitegore_sim_flat.png            blocks/parasitegore_sim_small.png           blocks/parasiteloot_common.png              blocks/parasiteloot_rare.png
blocks/parasiteloot_uncommon.png            blocks/parasitemouth.png                    blocks/parasitemouth_side.png               blocks/parasiteplank_deadhead.png           blocks/parasiteplank_deadhead_snow.png
blocks/parasiterubble_bone.png              blocks/parasiterubble_flesh.png             blocks/parasiterubble_stone.png             blocks/parasiterubble_weathb.png            blocks/parasiterubble_weathb_snow.png
blocks/parasiterubble_weathbc.png           blocks/parasiterubble_weathbc_snow.png      blocks/parasiterubble_weathfs.png           blocks/parasiterubble_weathfs_snow.png      blocks/parasiterubbledense_biome.png
blocks/parasiterubbledense_colony.png       blocks/parasiterubbledense_heart.png        blocks/parasiterubbledense_wall.png         blocks/parasiterubbledense_wall_side.png    blocks/parasitesapling_flowertall.png
blocks/parasitesapling_tree.png             blocks/parasitesapling_treethin.png         blocks/parasitestain_dirt.png               blocks/parasitestain_feeler.png             blocks/parasitestain_feeler_side.png
blocks/parasitestain_flesh.png              blocks/parasitestain_mud.png                blocks/parasitestain_red.png                blocks/parasitestain_sackofflesh_down.png   blocks/parasitestain_sackofflesh_side.png
blocks/parasitestain_sackofflesh_up.png     blocks/parasitestain_spore.png              blocks/parasitethin_treebase.png            blocks/parasitethin_treenesw.png            blocks/parasitetrunk_ball.png
blocks/parasitetrunk_ball_side.png          blocks/parasitetrunk_deadhead.png           blocks/parasitetrunk_deadhead_side.png      blocks/parasitetrunk_deadhead_snow.png      blocks/parasitetrunk_plant.png
blocks/parasitetrunk_plant_side.png         blocks/parasitetrunk_tree.png               blocks/parasitetrunk_tree_side.png          blocks/relay_base.png                       blocks/relay_bottom_bottom.png
blocks/relay_bottom_side.png                blocks/relay_bottom_top.png                 blocks/relay_middle.png                     blocks/relay_middle_bottom.png              blocks/relay_middle_side.png
blocks/relay_middle_top.png                 blocks/relay_roof.png                       blocks/relay_top_bottom.png                 blocks/relay_top_side.png                   blocks/relay_top_top.png
blocks/relaycontroller_dummy.png            blocks/residue_bricks.png                   blocks/residue_sprout_1.png                 blocks/residue_sprout_2.png                 blocks/residue_sprout_3.png
blocks/sapling_consumed.png                 blocks/sapling_deadhead.png                 blocks/sapling_infested.png                 blocks/semiorganic_block.png                blocks/sepia_glass.png
blocks/sepia_glass_pane.png                 blocks/shade_glass.png                      blocks/shade_glass_pane.png                 blocks/shrouded_glass.png                   blocks/shrouded_glass_pane.png
blocks/sick_alveoli_block.png               blocks/sick_skin_block.png                  blocks/skin_block.png                       blocks/solid_alveoli_block.png              blocks/spore_sack.png
blocks/stage0_ts.png                        blocks/stage0_ts_snow.png                   blocks/stage1_ts.png                        blocks/stage1_ts_snow.png                   blocks/stage2_ts.png
blocks/stage2_ts_noberry.png                blocks/stage2_ts_noberry_snow.png           blocks/stage2_ts_snow.png                   blocks/tall_hair_bottom.png                 blocks/tall_hair_top.png
blocks/trophy_boom_orb.png                  blocks/trophy_void_orb.png                  blocks/tunnel.png                           blocks/tunnel.png.mcmeta                    blocks/underside_growth.png
blocks/web_one.png                          blocks/web_three.png                        blocks/web_two.png                          effect/dither.png                           entity/monster/abohead.png
entity/monster/ata.png                      entity/monster/banof.png                    entity/monster/biomasspod.png               entity/monster/biomassvenkrol.png           entity/monster/dodsivh.png
entity/monster/emana_glow.png               entity/monster/emanah_glow.png              entity/monster/gotholone.png                entity/monster/hull_old.png                 entity/monster/hullh_old.png
entity/monster/humanfrozen.png              entity/monster/humankim.png                 entity/monster/humansoundeater.png          entity/monster/orbboom.png                  entity/monster/orbboom_armor.png
entity/monster/orbscary.png                 entity/monster/orbscary_armor.png           entity/monster/orbvoid.png                  entity/monster/orbvoid_armor.png            entity/monster/quac.png
entity/monster/shycof.png                   entity/monster/sky_flash.png                entity/monster/spe_villager_weeping.png     entity/monster/specow1.png                  entity/monster/venkrol_glow.png
entity/monster/venkrolsii_glow.png          entity/monster/venkrolsiii_glow.png         entity/monster/venkrolsiv_glow.png          entity/monster/venkrolsv.png                entity/monster/venkrolsvbase.png
entity/monster/venkrolsvmid.png             entity/monster/venkrolsvtop.png             entity/monster/vermina.png                  entity/monster/vesta1.png                   entity/painting/rupture.png
gui/abhorrence.png                          gui/advancement_wallpaper.png               gui/assimilated_pumpkin_overlay.png         gui/bestiary/background.png                 gui/bestiary/background_alt.png
gui/bestiary/bestiary_slot_old.png          gui/bestiary/blocks_background.png          gui/bestiary/drop_background.png            gui/bestiary/label_background.png           gui/bestiary/lore_background.png
gui/bestiary/model_background.png           gui/bestiary/name_background.png            gui/bestiary/slot.png                       gui/bestiary/stats_background.png           gui/bestiary/warning_sheet.png
gui/brew_fear.png                           gui/current_progress_gui.png                gui/dislo_report_fg_gui.png                 gui/dislo_report_gui.png                    gui/dislo_report_row_gui.png
gui/dislo_report_row_gui_one.png            gui/dislo_report_row_gui_three.png          gui/dislo_report_row_gui_two.png            gui/dislo_report_row_gui_unknown.png        gui/guerilla.png
gui/infuser_furnace.png                     gui/potion_adapted.png                      gui/potion_antimall.png                     gui/potion_bleed.png                        gui/potion_braining.png
gui/potion_conta.png                        gui/potion_corrosive.png                    gui/potion_coth.png                         gui/potion_crude.png                        gui/potion_cysticercosis.png
gui/potion_debar.png                        gui/potion_distorted_enlightenment.png      gui/potion_dod_smoke_trail.png              gui/potion_effectneg.png                    gui/potion_effectpos.png
gui/potion_fear.png                         gui/potion_feral.png                        gui/potion_foster.png                       gui/potion_indeaf.png                       gui/potion_jugg.png
gui/potion_link.png                         gui/potion_muscleout.png                    gui/potion_needler.png                      gui/potion_nexus.png                        gui/potion_novision.png
gui/potion_overheating.png                  gui/potion_parate.png                       gui/potion_pitted.png                       gui/potion_pivot.png                        gui/potion_prey.png
gui/potion_primitive.png                    gui/potion_pure.png                         gui/potion_rage.png                         gui/potion_repel.png                        gui/potion_senses.png
gui/potion_spotted.png                      gui/potion_the_sign.png                     gui/potion_thornshade_thorns.png            gui/potion_viral.png                        gui/potion_vomit.png
gui/question_mark_small.png                 gui/scanner_gui.png                         gui/source.png                              gui/systems/collective_consciousness.png    gui/systems/colonies.png
gui/systems/derived_distortion.png          gui/systems/dislodgment.png                 gui/systems/evolution.png                   gui/systems/generations.png                 gui/systems/hives.png
gui/systems/merge.png                       gui/systems/nests.png                       gui/systems/nodes.png                       gui/systems/reinforcement.png               gui/systems/scent.png
gui/systems/status_effects.png              gui/systems/ubiquitous_development.png      gui/systems/variants.png                    gui/systems/vectors.png                     gui/unused/arrow_down.png
gui/unused/arrow_up.png                     gui/unused/potion_1.png                     gui/unused/potion_2.png                     gui/unused/potion_antimall2.png             gui/unused/potion_carcinogenic.png
gui/unused/potion_cold_utr.png              gui/unused/potion_coth2.png                 gui/unused/potion_frostbite.png             gui/unused/potion_predation.png             gui/unused/potion_river_blind.png
gui/unused/potion_water_predation.png       gui/vector_far.png                          gui/vector_gui.png                          gui/vector_gui_result_bg.png                gui/vector_origin.png
gui/vector_pinpoint.png                     items/ada_arachnida_drop.png                items/ada_bolster_drop.png                  items/ada_devourer_drop.png                 items/ada_longarms_drop.png
items/ada_manducater_drop.png               items/ada_manducater_drop.png.mcmeta        items/ada_reeker_drop.png                   items/ada_summoner_drop.png                 items/ada_summoner_drop.png.mcmeta
items/ada_vermin_drop.png                   items/ada_viscera_drop.png                  items/ada_yelloweye_drop.png                items/alveolar_fluid.png                    items/alveoli.png
items/armor_boots.png                       items/armor_boots_sentient.png              items/armor_chest.png                       items/armor_chest_sentient.png              items/armor_helm.png
items/armor_helm_sentient.png               items/armor_pants.png                       items/armor_pants_sentient.png              items/assimilated_flesh.png                 items/assimilated_flower_pot_item.png
items/assimilated_sugar_cane_item.png       items/baby_shrimp.png                       items/bad_apple.png                         items/beckon_drop.png                       items/biomass.png
items/bone.png                              items/book_of_vengeance.png                 items/bough.png                             items/brusewood_door.png                    items/compass_00.png
items/compass_01.png                        items/compass_02.png                        items/compass_03.png                        items/compass_04.png                        items/compass_05.png
items/compass_06.png                        items/compass_07.png                        items/compass_08.png                        items/compass_09.png                        items/compass_10.png
items/compass_11.png                        items/compass_12.png                        items/compass_13.png                        items/compass_14.png                        items/compass_15.png
items/compass_16.png                        items/compass_17.png                        items/compass_18.png                        items/compass_19.png                        items/compass_20.png
items/compass_21.png                        items/compass_22.png                        items/compass_23.png                        items/compass_24.png                        items/compass_25.png
items/compass_26.png                        items/compass_27.png                        items/compass_28.png                        items/compass_29.png                        items/compass_30.png
items/compass_31.png                        items/compendium.png                        items/consumed_door.png                     items/consumed_flower_pot_item.png          items/deadblood_bottle.png
items/deadhead_door.png                     items/disc1.png                             items/disc1.png.mcmeta                      items/disc2.png                             items/disc3.png
items/dislodgement_report.png               items/dispatcher_drop.png                   items/dried_tendons.png                     items/evclock0.png                          items/evclock1.png
items/evclock10.png                         items/evclock2.png                          items/evclock3.png                          items/evclock4.png                          items/evclock5.png
items/evclock6.png                          items/evclock7.png                          items/evclock8.png                          items/evclock9.png                          items/evclockm1.png
items/evclockm2.png                         items/evclockoff.png                        items/fishlin.png                           items/fog_bottle.png                        items/goth_door.png
items/goth_shroom.png                       items/greek_fire.png                        items/greek_fire.png.mcmeta                 items/hardened_bone_handle.png              items/hijacked_blaze_rod.png
items/hijacked_bone.png                     items/hijacked_growth.png                   items/hijacked_iron_axe.png                 items/hijacked_iron_boots.png               items/hijacked_iron_chestplate.png
items/hijacked_iron_helmet.png              items/hijacked_iron_hoe.png                 items/hijacked_iron_ingot.png               items/hijacked_iron_leggings.png            items/hijacked_iron_pickaxe.png
items/hijacked_iron_shovel.png              items/hijacked_iron_sword.png               items/hivescrap.png                         items/infectious_blade_fragment.png         items/infested_bonemeal.png
items/infested_door.png                     items/itema.png                             items/itema.png.mcmeta                      items/itemd.png                             items/itemd.png.mcmeta
items/iteme.png                             items/iteme.png.mcmeta                      items/itemmobspawner_.png                   items/itemmobspawner_abobodies.png          items/itemmobspawner_abohead.png
items/itemmobspawner_alafha.png             items/itemmobspawner_anged.png              items/itemmobspawner_ata.png                items/itemmobspawner_buthol.png             items/itemmobspawner_canra.png
items/itemmobspawner_canraadapted.png       items/itemmobspawner_cruxa.png              items/itemmobspawner_cruxb.png              items/itemmobspawner_dod.png                items/itemmobspawner_dodsii.png
items/itemmobspawner_dodsiii.png            items/itemmobspawner_dodsiv.png             items/itemmobspawner_done.png               items/itemmobspawner_dorpa.png              items/itemmobspawner_dorpahead.png
items/itemmobspawner_elvia.png              items/itemmobspawner_emana.png              items/itemmobspawner_emanaadapted.png       items/itemmobspawner_esor.png               items/itemmobspawner_ferbear.png
items/itemmobspawner_fercow.png             items/itemmobspawner_ferenderman.png        items/itemmobspawner_ferhorse.png           items/itemmobspawner_ferhuman.png           items/itemmobspawner_ferpig.png
items/itemmobspawner_fersheep.png           items/itemmobspawner_fervillager.png        items/itemmobspawner_ferwolf.png            items/itemmobspawner_flog.png               items/itemmobspawner_ganro.png
items/itemmobspawner_gim.png                items/itemmobspawner_gimadapted.png         items/itemmobspawner_gothol.png             items/itemmobspawner_heblu.png              items/itemmobspawner_heed.png
items/itemmobspawner_hiblaze.png            items/itemmobspawner_higolem.png            items/itemmobspawner_hiskeleton.png         items/itemmobspawner_host.png               items/itemmobspawner_hostii.png
items/itemmobspawner_hull.png               items/itemmobspawner_hulladapted.png        items/itemmobspawner_iki.png                items/itemmobspawner_ikiadapted.png         items/itemmobspawner_infbear.png
items/itemmobspawner_infbearhead.png        items/itemmobspawner_infcow.png             items/itemmobspawner_infcowhead.png         items/itemmobspawner_infdragone.png         items/itemmobspawner_infdragonehead.png
items/itemmobspawner_infenderman.png        items/itemmobspawner_infendermanhead.png    items/itemmobspawner_infhorse.png           items/itemmobspawner_infhorsehead.png       items/itemmobspawner_infhuman.png
items/itemmobspawner_infhumanhead.png       items/itemmobspawner_infpig.png             items/itemmobspawner_infpighead.png         items/itemmobspawner_infplayer.png          items/itemmobspawner_infplayerhead.png
items/itemmobspawner_infsheep.png           items/itemmobspawner_infsheephead.png       items/itemmobspawner_infsquid.png           items/itemmobspawner_infvillager.png        items/itemmobspawner_infvillagerhead.png
items/itemmobspawner_infwolf.png            items/itemmobspawner_infwolfhead.png        items/itemmobspawner_inhoom.png             items/itemmobspawner_inhoos.png             items/itemmobspawner_jinjo.png
items/itemmobspawner_kirin.png              items/itemmobspawner_kol.png                items/itemmobspawner_leem.png               items/itemmobspawner_leemsii.png            items/itemmobspawner_leemsiii.png
items/itemmobspawner_leemsiv.png            items/itemmobspawner_leer.png               items/itemmobspawner_lencia.png             items/itemmobspawner_lesh.png               items/itemmobspawner_lodo.png
items/itemmobspawner_lum.png                items/itemmobspawner_lumadapted.png         items/itemmobspawner_mar.png                items/itemmobspawner_marbear.png            items/itemmobspawner_marcow.png
items/itemmobspawner_marenderman.png        items/itemmobspawner_marhuman.png           items/itemmobspawner_marsheep.png           items/itemmobspawner_marvillager.png        items/itemmobspawner_marwolf.png
items/itemmobspawner_mes.png                items/itemmobspawner_mudo.png               items/itemmobspawner_nak.png                items/itemmobspawner_nogla.png              items/itemmobspawner_noglaadapted.png
items/itemmobspawner_nuuh.png               items/itemmobspawner_omboo.png              items/itemmobspawner_orch.png               items/itemmobspawner_oronco.png             items/itemmobspawner_pheon.png
items/itemmobspawner_pod.png                items/itemmobspawner_quac.png               items/itemmobspawner_ranrac.png             items/itemmobspawner_ranracadapted.png      items/itemmobspawner_rathol.png
items/itemmobspawner_shyco.png              items/itemmobspawner_shycoadapted.png       items/itemmobspawner_soo.png                items/itemmobspawner_tenn.png               items/itemmobspawner_terla.png
items/itemmobspawner_tonro.png              items/itemmobspawner_unvo.png               items/itemmobspawner_venkrol.png            items/itemmobspawner_venkrolsii.png         items/itemmobspawner_venkrolsiii.png
items/itemmobspawner_venkrolsiv.png         items/itemmobspawner_vesta.png              items/itemmobspawner_viin.png               items/itemmobspawner_wymo.png               items/itemmobspawner_wymoadapted.png
items/itemmobspawner_zaa.png                items/itemmobspawner_zaaadapted.png         items/itemmobspawner_zetmo.png              items/itemmobspawner_zetmoadapted.png       items/itemtab.png
items/itemv.png                             items/itemv.png.mcmeta                      items/levelclock0.png                       items/levelclock1.png                       items/levelclock2.png
items/levelclock3.png                       items/levelclock4.png                       items/living_core.png                       items/lurecomponent1.png                    items/lurecomponent10.png
items/lurecomponent2.png                    items/lurecomponent3.png                    items/lurecomponent4.png                    items/lurecomponent5.png                    items/lurecomponent6.png
items/lurecomponent7.png                    items/lurecomponent8.png                    items/lurecomponent9.png                    items/mobility_armor_boots.png              items/mobility_armor_chestpiece.png
items/mobility_armor_helmet.png             items/mobility_armor_leggings.png           items/module_adapted.png                    items/module_ancient.png                    items/module_assimara.png
items/module_assimilated.png                items/module_base.png                       items/module_base_tissue_spike.png          items/module_crude.png                      items/module_derived.png
items/module_desmoid.png                    items/module_deterrent.png                  items/module_dislodgement.png               items/module_eschar.png                     items/module_feral.png
items/module_hijacked.png                   items/module_ideal.png                      items/module_inborn.png                     items/module_nexus.png                      items/module_organ_synth.png
items/module_origin.png                     items/module_phase.png                      items/module_preeminent.png                 items/module_primitive.png                  items/module_pure.png
items/module_resistance.png                 items/module_vectors.png                    items/parasitebush_decanter.png             items/parasitebush_pop.png                  items/pearl_assimara.png
items/pearl_assimara.png.mcmeta             items/pearl_assimilated.png                 items/pearl_assimilated.png.mcmeta          items/pearl_feral.png                       items/pearl_feral.png.mcmeta
items/pearl_idle.png                        items/phase_report.png                      items/relaycontroller.png                   items/residue_item.png                      items/residue_sprout.png
items/semiorganic_ingot.png                 items/shrimp.png                            items/test.png                              items/the_sign.png                          items/the_sign.png.mcmeta
items/thornshade_berry.png                  items/throwingball.png                      items/trophy_boom_orb.png                   items/trophy_void_orb.png                   items/tunnel.png
items/tunnel.png.mcmeta                     items/vector_map_report.png                 items/venkrol_boots.png                     items/vile_shell.png                        items/weapon_axe.png
items/weapon_axe_sentient.png               items/weapon_axe_sentient.png.mcmeta        items/weapon_axe_v.png                      items/weapon_bow_pulling_0.png              items/weapon_bow_pulling_1.png
items/weapon_bow_pulling_2.png              items/weapon_bow_sentient_pulling_0.png     items/weapon_bow_sentient_pulling_1.png     items/weapon_bow_sentient_pulling_2.png     items/weapon_bow_sentient_standby.png
items/weapon_bow_standby.png                items/weapon_cleaver.png                    items/weapon_cleaver_sentient.png           items/weapon_cleaver_sentient.png.mcmeta    items/weapon_dagger.png
items/weapon_dagger_sentient.png            items/weapon_dagger_sentient.png.mcmeta     items/weapon_lance.png                      items/weapon_lance_sentient.png             items/weapon_lance_sentient.png.mcmeta
items/weapon_maul.png                       items/weapon_maul_sentient.png              items/weapon_maul_sentient.png.mcmeta       items/weapon_scythe.png                     items/weapon_scythe_sentient.png
items/weapon_scythe_sentient.png.mcmeta     items/weapon_sword.png                      items/weapon_sword_sentient.png             items/weapon_sword_sentient.png.mcmeta      models/armor/livings_v_layer_1.png
models/armor/livings_v_layer_2.png          models/armor/mobility_armor.png             models/armor/sentients_v_layer_1.png        models/armor/sentients_v_layer_2.png        parasitebush_flower1.png
parasitebush_grass12_node.png               parasitebush_grass1_node.png                parasitebush_infected.png                   particle/arc_flash.png                      particle/arc_flash_tail.png
particle/biomass1.png                       particle/biomass2.png                       particle/biomass3.png                       particle/biomass4.png                       particle/blood1.png
particle/blood2.png                         particle/blood_land.png                     particle/dot.png                            particle/flash.png                          particle/fog1.png
particle/fog1_outer.png                     particle/gore/flesh_adapted1.png            particle/gore/flesh_adapted2.png            particle/gore/flesh_adapted3.png            particle/gore/flesh_assimilated1.png
particle/gore/flesh_assimilated2.png        particle/gore/flesh_assimilated3.png        particle/gore/flesh_primitive1.png          particle/gore/flesh_primitive2.png          particle/gore/flesh_primitive3.png
particle/gore/flesh_pure1.png               particle/gore/flesh_pure2.png               particle/gore/flesh_pure3.png               particle/infested_leaves.png                particle/infested_leaves2.png
particle/infested_leaves3.png               particle/infested_leaves4.png               particle/john.png                           particle/lure1.png                          particle/lure2.png
particle/orb.png                            particle/pure1.png                          particle/pure2.png                          particle/rage1.png                          particle/rage2.png
particle/rage3.png                          particle/rhappy.png                         particle/spore.png                          particle/spore/spore_green1.png             particle/spore/spore_green2.png
particle/spore/spore_green3.png             particle/spore/spore_green4.png             particle/spore/spore_red1.png               particle/spore/spore_red2.png               particle/spore/spore_red3.png
particle/spore/spore_red4.png               particle/spore/spore_white1.png             particle/spore/spore_white2.png             particle/spore/spore_white3.png             particle/spore/spore_white4.png
particle/spore/spore_yellow1.png            particle/spore/spore_yellow2.png            particle/spore/spore_yellow3.png            particle/spore/spore_yellow4.png            particle/upd_particles(unused)/biomass.png
particle/upd_particles(unused)/biomass2.png  particle/upd_particles(unused)/biomass3.png  particle/upd_particles(unused)/bright_spark1.png  particle/upd_particles(unused)/bright_spark2.png  particle/upd_particles(unused)/rage1.png
particle/upd_particles(unused)/rage2.png    particle/upd_particles(unused)/rage3.png    particle/viral1.png                         particle/viral2.png                         particle/viral3.png
particle/viral4.png                         particle/vomit/vomit1.png                   particle/vomit/vomit2.png                   particle/vomit/vomit3.png                   particle/vomit/vomit4.png
particle/vomit/vomit5.png                   particle/vomit/vomit6.png                   particle/vomit/vomit_land1.png              particle/vomit/vomit_land2.png              particle/vomit/vomit_land3.png
particle/wind/wind1.png                     particle/wind/wind10.png                    particle/wind/wind11.png                    particle/wind/wind12.png                    particle/wind/wind13.png
particle/wind/wind2.png                     particle/wind/wind3.png                     particle/wind/wind4.png                     particle/wind/wind5.png                     particle/wind/wind6.png
particle/wind/wind7.png                     particle/wind/wind8.png                     particle/wind/wind9.png
```

### 模型 JSON：目标 783，捐赠 1719，缺 936

```
block/arraytowerlarge.mtl                   block/arraytowerlarge.obj                   block/ashen_glass_pane_post_ends.json       block/assimilated_blossom.json              block/bloody_glass_pane_post_ends.json
block/bloodyice.json                        block/bruisewood/door_bottom.json           block/bruisewood/door_bottom_rh.json        block/bruisewood/door_top.json              block/bruisewood/door_top_rh.json
block/bruisewood_fence_post.json            block/bruisewood_fence_side.json            block/bruisewood_plank_slab.json            block/bruisewood_plank_slab_double.json     block/bruisewood_plank_slab_top.json
block/bruisewood_plank_stairs.json          block/bruisewood_plank_stairs_inner.json    block/bruisewood_plank_stairs_outer.json    block/bruisewood_plank_wall_inventory.json  block/bruisewood_plank_wall_post.json
block/bruisewood_plank_wall_side.json       block/bruisewood_plank_wall_side_tall.json  block/brusewood_door_bottom_left.json       block/brusewood_door_bottom_left_open.json  block/brusewood_door_bottom_right.json
block/brusewood_door_bottom_right_open.json  block/brusewood_door_top_left.json          block/brusewood_door_top_left_open.json     block/brusewood_door_top_right.json         block/brusewood_door_top_right_open.json
block/brusewood_pressure_plate_up.json      block/brusewood_trapdoor_bottom.json        block/brusewood_trapdoor_open.json          block/brusewood_trapdoor_top.json           block/colonyoutpost.json
block/consumed_door/door_bottom.json        block/consumed_door/door_bottom_rh.json     block/consumed_door/door_top.json           block/consumed_door/door_top_rh.json        block/consumed_door_bottom_left.json
block/consumed_door_bottom_left_open.json   block/consumed_door_bottom_right.json       block/consumed_door_bottom_right_open.json  block/consumed_door_top_left.json           block/consumed_door_top_left_open.json
block/consumed_door_top_right.json          block/consumed_door_top_right_open.json     block/consumed_fence_post.json              block/consumed_fence_side.json              block/consumed_plank_slab.json
block/consumed_plank_slab_double.json       block/consumed_plank_slab_top.json          block/consumed_plank_wall_inventory.json    block/consumed_plank_wall_post.json         block/consumed_plank_wall_side.json
block/consumed_plank_wall_side_tall.json    block/consumed_planks_stairs.json           block/consumed_planks_stairs_inner.json     block/consumed_planks_stairs_outer.json     block/consumed_pot.json
block/consumed_pressure_plate_up.json       block/consumed_trapdoor_bottom.json         block/consumed_trapdoor_open.json           block/consumed_trapdoor_top.json            block/consumed_workbench.json
block/cooked_flesh_door_bottom_left.json    block/cooked_flesh_door_bottom_left_open.json  block/cooked_flesh_door_bottom_right.json   block/cooked_flesh_door_bottom_right_open.json  block/cooked_flesh_door_top_left.json
block/cooked_flesh_door_top_left_open.json  block/cooked_flesh_door_top_right.json      block/cooked_flesh_door_top_right_open.json  block/cooked_flesh_pressure_plate_up.json   block/cooked_flesh_slab_double.json
block/cooked_flesh_trapdoor_bottom.json     block/cooked_flesh_trapdoor_open.json       block/cooked_flesh_trapdoor_top.json        block/dead_head_plank_slab.json             block/dead_head_plank_slab_double.json
block/dead_head_plank_slab_top.json         block/deadhead_fence_post.json              block/deadhead_fence_side.json              block/deadhead_plank_stairs.json            block/deadhead_plank_stairs_inner.json
block/deadhead_plank_stairs_outer.json      block/deadhead_pressure_plate_up.json       block/dermoid_cyst_base.json                block/dermoid_cyst_east.json                block/dermoid_cyst_north.json
block/dermoid_cyst_south.json               block/dermoid_cyst_west.json                block/dodn.json                             block/epitome_infestation_warp_diffuser.json  block/flesh_door_bottom_left.json
block/flesh_door_bottom_left_open.json      block/flesh_door_bottom_right.json          block/flesh_door_bottom_right_open.json     block/flesh_door_top_left.json              block/flesh_door_top_left_open.json
block/flesh_door_top_right.json             block/flesh_door_top_right_open.json        block/flesh_fence_post.json                 block/flesh_fence_side.json                 block/flesh_pressure_plate_up.json
block/flesh_slab.json                       block/flesh_slab_double.json                block/flesh_slab_top.json                   block/flesh_stairs.json                     block/flesh_stairs_inner.json
block/flesh_stairs_outer.json               block/flesh_trapdoor_bottom.json            block/flesh_trapdoor_open.json              block/flesh_trapdoor_top.json               block/frost_weathered_stone_slab.json
block/frost_weathered_stone_slab_double.json  block/frost_weathered_stone_slab_top.json   block/frost_weathered_stone_stairs.json     block/frost_weathered_stone_stairs_inner.json  block/frost_weathered_stone_stairs_outer.json
block/gore_ada_big.json                     block/gore_ada_flat.json                    block/gore_ada_small.json                   block/gore_fer_big.json                     block/gore_fer_flat.json
block/gore_fer_small.json                   block/gore_mar_big.json                     block/gore_mar_flat.json                    block/gore_mar_small.json                   block/gore_pri_big.json
block/gore_pri_flat.json                    block/gore_pri_small.json                   block/gore_pure_big.json                    block/gore_pure_flat.json                   block/gore_pure_small.json
block/gore_sim_big.json                     block/gore_sim_flat.json                    block/gore_sim_small.json                   block/goth_door/door_bottom.json            block/goth_door/door_bottom_rh.json
block/goth_door/door_top.json               block/goth_door/door_top_rh.json            block/goth_door_bottom.json                 block/goth_door_bottom_left.json            block/goth_door_bottom_left_open.json
block/goth_door_bottom_right.json           block/goth_door_bottom_right_open.json      block/goth_door_top.json                    block/goth_door_top_left.json               block/goth_door_top_left_open.json
block/goth_door_top_right.json              block/goth_door_top_right_open.json         block/goth_fence_post.json                  block/goth_fence_side.json                  block/goth_plank_slab.json
block/goth_plank_slab_double.json           block/goth_plank_slab_top.json              block/goth_plank_wall_inventory.json        block/goth_plank_wall_post.json             block/goth_plank_wall_side.json
block/goth_plank_wall_side_tall.json        block/goth_planks_stairs.json               block/goth_planks_stairs_inner.json         block/goth_planks_stairs_outer.json         block/goth_pressure_plate_up.json
block/goth_stem.json                        block/goth_trapdoor_bottom.json             block/goth_trapdoor_open.json               block/goth_trapdoor_top.json                block/gothshroom_ceiling.json
block/gothshroom_floor.json                 block/gothshroom_group.json                 block/gothshroom_wall.json                  block/hair_follicle_block_horizontal.json   block/harlequinn_glass_pane_post_ends.json
block/harlequinn_grass.json                 block/harleskinn_fence_post.json            block/harleskinn_fence_side.json            block/harleskinn_slab.json                  block/harleskinn_slab_double.json
block/harleskinn_slab_top.json              block/harleskinn_stairs.json                block/harleskinn_stairs_inner.json          block/harleskinn_stairs_outer.json          block/hirsute_hair.json
block/infested_cactus.json                  block/infested_cactus_maw.json              block/infested_cobblestone_slab_double.json  block/infested_cobblestone_snow.json        block/infested_dirt_slab_double.json
block/infested_door_bottom_left.json        block/infested_door_bottom_left_open.json   block/infested_door_bottom_right.json       block/infested_door_bottom_right_open.json  block/infested_door_top_left.json
block/infested_door_top_left_open.json      block/infested_door_top_right.json          block/infested_door_top_right_open.json     block/infested_fence_post.json              block/infested_fence_side.json
block/infested_furnace.json                 block/infested_furnace_lit.json             block/infested_glass_pane_post_ends.json    block/infested_leaves.json                  block/infested_plank_slab_double.json
block/infested_pot.json                     block/infested_pressure_plate_up.json       block/infested_sandstone_slab_double.json   block/infested_stone_brick_slab_double.json  block/infested_stone_slab_double.json
block/infested_terracotta_slab_double.json  block/infested_trapdoor_bottom.json         block/infested_trapdoor_open.json           block/infested_trapdoor_top.json            block/infested_workbench.json
block/infestedbush_arc.json                 block/infestedbush_flower1.json             block/infestedbush_grass1.json              block/infestedbush_grass12.json             block/infestedbush_grass12_node.json
block/infestedbush_grass1_node.json         block/infestedbush_infected.json            block/infestedbush_spine.json               block/infestedbush_spine2.json              block/infestedbush_spine2_node.json
block/infestedbush_spine_end.json           block/infestedbush_spine_end_node.json      block/infestedbush_spine_node.json          block/infestedbush_vine.json                block/infestedbush_vine2.json
block/infestedore_co.json                   block/infestedore_dia.json                  block/infestedore_eme.json                  block/infestedore_gol.json                  block/infestedore_iro.json
block/infestedore_lap.json                  block/infestedore_red.json                  block/infestedore_un.json                   block/infestedremain_base.json              block/infestedremain_base_infested.json
block/infestedrubblestairs.json             block/infestedrubblestairs_inner.json       block/infestedrubblestairs_outer.json       block/infestedsand_snow.json                block/infestedstain_snow.json
block/infestedstainstairs.json              block/infestedstainstairs_inner.json        block/infestedstainstairs_outer.json        block/infestedtrunkstairs.json              block/infestedtrunkstairs_inner.json
block/infestedtrunkstairs_outer.json        block/infestremain_infested.json            block/lipoma_mass.json                      block/locs_block_slab.json                  block/locs_block_slab_double.json
block/locs_block_slab_top.json              block/locs_block_snowy.json                 block/moody_glass_pane_post_ends.json       block/node_lamp_on_1.json                   block/node_lamp_on_2.json
block/node_lamp_on_3.json                   block/node_lamp_on_4.json                   block/node_lamp_on_5.json                   block/parasite_barrier.json                 block/parasite_fog.json
block/parasitebush_bine.json                block/parasitebush_bine_end.json            block/parasitebush_bine_top.json            block/parasitebush_decanter.json            block/parasitebush_decanterempty.json
block/parasitebush_eye.json                 block/parasitebush_frostg1.json             block/parasitebush_frostg2.json             block/parasitebush_frostg3.json             block/parasitebush_frostg4.json
block/parasitebush_frostgt1.json            block/parasitebush_frostgt2.json            block/parasitebush_frostgt3.json            block/parasitebush_pop.json                 block/parasitebush_tendril.json
block/parasitebush_tendril_bottom.json      block/parasitebush_tendril_end.json         block/parasitebush_thorn.json               block/parasitebush_thorndead.json           block/parasitebush_thorndormat.json
block/parasitebush_thorndormats.json        block/parasitebush_thorntwo.json            block/parasitebush_thorntwos.json           block/parasitebush_tooh.json                block/parasitebush_tooh_node.json
block/parasitecanister_bag.json             block/parasitecanister_bag_wall_post.json   block/parasitecanister_bag_wall_side.json   block/parasitecanister_bag_wall_side_tall.json  block/parasitecanister_cyst.json
block/parasitecanister_cysta.json           block/parasitecanister_lump.json            block/parasitecanister_sac.json             block/parasitelog_base.json                 block/parasiteplank_deadhead_wall_post.json
block/parasiteplank_deadhead_wall_side.json  block/parasiteplank_deadhead_wall_side_tall.json  block/parasiteplank_deadheads.json          block/parasiterubble_bone.json              block/parasiterubble_bonestairs.json
block/parasiterubble_bonestairs_inner.json  block/parasiterubble_bonestairs_outer.json  block/parasiterubble_bricks.json            block/parasiterubble_bricks_wall_post.json  block/parasiterubble_bricks_wall_side.json
block/parasiterubble_bricks_wall_side_tall.json  block/parasiterubble_bricksstairs.json      block/parasiterubble_bricksstairs_inner.json  block/parasiterubble_bricksstairs_outer.json  block/parasiterubble_flesh.json
block/parasiterubble_flesh_wall.json        block/parasiterubble_flesh_wall_inventory.json  block/parasiterubble_flesh_wall_post.json   block/parasiterubble_flesh_wall_side.json   block/parasiterubble_flesh_wall_side_tall.json
block/parasiterubble_fleshstairs.json       block/parasiterubble_fleshstairs_inner.json  block/parasiterubble_fleshstairs_outer.json  block/parasiterubble_fungus.json            block/parasiterubble_fungusstairs.json
block/parasiterubble_fungusstairs_inner.json  block/parasiterubble_fungusstairs_outer.json  block/parasiterubble_metal.json             block/parasiterubble_metal_wall_post.json   block/parasiterubble_metal_wall_side.json
block/parasiterubble_metal_wall_side_tall.json  block/parasiterubble_metalstairs.json       block/parasiterubble_metalstairs_inner.json  block/parasiterubble_metalstairs_outer.json  block/parasiterubble_obsidian.json
block/parasiterubble_obsidianstairs.json    block/parasiterubble_obsidianstairs_inner.json  block/parasiterubble_obsidianstairs_outer.json  block/parasiterubble_stone.json             block/parasiterubble_stonedebrisstairs.json
block/parasiterubble_stonedebrisstairs_inner.json  block/parasiterubble_stonedebrisstairs_outer.json  block/parasiterubble_stonestairs.json       block/parasiterubble_stonestairs_inner.json  block/parasiterubble_stonestairs_outer.json
block/parasiterubble_weathb.json            block/parasiterubble_weathb_wall_post.json  block/parasiterubble_weathb_wall_side.json  block/parasiterubble_weathb_wall_side_tall.json  block/parasiterubble_weathbc.json
block/parasiterubble_weathbc_wall_post.json  block/parasiterubble_weathbc_wall_side.json  block/parasiterubble_weathbc_wall_side_tall.json  block/parasiterubble_weathbcs.json          block/parasiterubble_weathbs.json
block/parasiterubble_weathfs.json           block/parasiterubble_weathfs_wall_post.json  block/parasiterubble_weathfs_wall_side.json  block/parasiterubble_weathfs_wall_side_tall.json  block/parasiterubble_weathfss.json
block/parasiterubble_wood.json              block/parasiterubble_woodstairs.json        block/parasiterubble_woodstairs_inner.json  block/parasiterubble_woodstairs_outer.json  block/parasiterubbledense.json
block/parasiterubbledense_biome.json        block/parasiterubbledense_biome_wall_post.json  block/parasiterubbledense_biome_wall_side.json  block/parasiterubbledense_biome_wall_side_tall.json  block/parasiterubbledense_biomestairs.json
block/parasiterubbledense_biomestairs_inner.json  block/parasiterubbledense_biomestairs_outer.json  block/parasiterubbledense_colony.json       block/parasiterubbledense_colony_wall_post.json  block/parasiterubbledense_colony_wall_side.json
block/parasiterubbledense_colony_wall_side_tall.json  block/parasiterubbledense_colonystairs.json  block/parasiterubbledense_colonystairs_inner.json  block/parasiterubbledense_colonystairs_outer.json  block/parasiterubbledense_heart.json
block/parasiterubbledense_wall.json         block/parasiterubbledense_wallstairs.json   block/parasiterubbledense_wallstairs_inner.json  block/parasiterubbledense_wallstairs_outer.json  block/parasitesapling_consumed.json
block/parasitesapling_deadhead.json         block/parasitesapling_flowertall.json       block/parasitesapling_infested.json         block/parasitesapling_tree.json             block/parasitesapling_treethin.json
block/parasitestain_dirt.json               block/parasitestain_dirtstairs.json         block/parasitestain_dirtstairs_inner.json   block/parasitestain_dirtstairs_outer.json   block/parasitestain_feeler.json
block/parasitestain_feelerstairs.json       block/parasitestain_feelerstairs_inner.json  block/parasitestain_feelerstairs_outer.json  block/parasitestain_flesh.json              block/parasitestain_flesh_wall_post.json
block/parasitestain_flesh_wall_side.json    block/parasitestain_flesh_wall_side_tall.json  block/parasitestain_fleshstairs.json        block/parasitestain_fleshstairs_inner.json  block/parasitestain_fleshstairs_outer.json
block/parasitestain_mud.json                block/parasitestain_mudstairs.json          block/parasitestain_mudstairs_inner.json    block/parasitestain_mudstairs_outer.json    block/parasitestain_red.json
block/parasitestain_sackflesh.json          block/parasitestain_spore.json              block/parasitethin_base.json                block/parasitethin_nesw.json                block/parasitethin_treebase.json
block/parasitethin_treenesw.json            block/parasitetrunk.json                    block/parasitetrunk_ball.json               block/parasitetrunk_ballstairs.json         block/parasitetrunk_ballstairs_inner.json
block/parasitetrunk_ballstairs_outer.json   block/parasitetrunk_circle.json             block/parasitetrunk_deadhead.json           block/parasitetrunk_deadheads.json          block/parasitetrunk_plant.json
block/parasitetrunk_plantstairs.json        block/parasitetrunk_plantstairs_inner.json  block/parasitetrunk_plantstairs_outer.json  block/parasitetrunk_tree.json               block/parasitetrunk_treestairs.json
block/parasitetrunk_treestairs_inner.json   block/parasitetrunk_treestairs_outer.json   block/parasitic_colony_core_slab.json       block/parasitic_colony_core_slab_double.json  block/parasitic_colony_core_slab_top.json
block/parasitic_compressed_colony_stone_slab.json  block/parasitic_compressed_colony_stone_slab_double.json  block/parasitic_compressed_colony_stone_slab_top.json  block/pasted.png                            block/poland_skin_slab.json
block/poland_skin_slab_double.json          block/poland_skin_slab_top.json             block/polished_infested_stone_slab_double.json  block/potted_assimilated_blossom.json       block/potted_consumed_assimilated_blossom.json
block/reinforced_hivestone_slab.json        block/reinforced_hivestone_slab_double.json  block/reinforced_hivestone_slab_top.json    block/relaycontroller.json                  block/relaycontroller_dummy.json
block/residue_brick_slab_double.json        block/sac_of_flesh_slab.json                block/sac_of_flesh_slab_double.json         block/sac_of_flesh_slab_top.json            block/sepia_glass_pane_post_ends.json
block/shrouded_glass_pane_post_ends.json    block/slab_bottom.json                      block/slab_double.json                      block/slab_top.json                         block/slabbone_bottom.json
block/slabbone_double.json                  block/slabbone_top.json                     block/slabbricks_bottom.json                block/slabbricks_double.json                block/slabbricks_top.json
block/slabdirt_bottom.json                  block/slabdirt_double.json                  block/slabdirt_top.json                     block/slabfeeler_bottom.json                block/slabfeeler_double.json
block/slabfeeler_top.json                   block/slabflesh_bottom.json                 block/slabflesh_double.json                 block/slabflesh_top.json                    block/slabfungus_bottom.json
block/slabfungus_double.json                block/slabfungus_top.json                   block/slabmetal_bottom.json                 block/slabmetal_double.json                 block/slabmetal_top.json
block/slabmud_bottom.json                   block/slabmud_double.json                   block/slabmud_top.json                      block/slabobsidian_bottom.json              block/slabobsidian_double.json
block/slabobsidian_top.json                 block/slabred_bottom.json                   block/slabred_double.json                   block/slabred_top.json                      block/slabsackflesh_bottom.json
block/slabsackflesh_double.json             block/slabsackflesh_top.json                block/slabsflesh_bottom.json                block/slabsflesh_double.json                block/slabsflesh_top.json
block/slabspore_bottom.json                 block/slabspore_double.json                 block/slabspore_top.json                    block/slabstone_bottom.json                 block/slabstone_double.json
block/slabstone_top.json                    block/slabstonedebris_bottom.json           block/slabstonedebris_double.json           block/slabstonedebris_top.json              block/slabwood_bottom.json
block/slabwood_double.json                  block/slabwood_top.json                     block/ten.webp                              block/texture.png                           block/tresses_hair_bottom.json
block/tresses_hair_top.json                 block/wall_post_base.json                   block/wall_side_base.json                   block/weathered_bricks_slab.json            block/weathered_bricks_slab_double.json
block/weathered_bricks_slab_top.json        block/weathered_cobblestone_slab.json       block/weathered_cobblestone_slab_double.json  block/weathered_cobblestone_slab_top.json   block/web_one.json
block/web_three.json                        block/web_two.json                          block/wheathered_bricks_stairs.json         block/wheathered_bricks_stairs_inner.json   block/wheathered_bricks_stairs_outer.json
block/wheathered_cobblestone_stairs.json    block/wheathered_cobblestone_stairs_inner.json  block/wheathered_cobblestone_stairs_outer.json  item/adapted_icon.json                      item/assimilated_blossom.json
item/axe.json                               item/axe_sentient.json                      item/bloodyice.json                         item/boots.json                             item/boots_sentient.json
item/bow.json                               item/bow_sentient.json                      item/bruisewood_fence.json                  item/bruisewood_plank_slab.json             item/bruisewood_plank_stairs.json
item/bruisewood_plank_wall.json             item/brusewood_door.json                    item/brusewood_trapdoor.json                item/canister_bag.json                      item/canister_cyst.json
item/canister_lump.json                     item/canister_sac.json                      item/canisteractive.json                    item/chest.json                             item/chest_sentient.json
item/cleaver.json                           item/cleaver_sentient.json                  item/consumed_door.json                     item/consumed_fence.json                    item/consumed_plank_slab.json
item/consumed_plank_wall.json               item/consumed_planks_stairs.json            item/consumed_pot.json                      item/consumed_trapdoor.json                 item/consumed_workbench.json
item/cooked_flesh_door.json                 item/cooked_flesh_trapdoor.json             item/cosmic_structural_failure_icon.json    item/crude_icon.json                        item/dark_days_icon.json
item/dead_head_plank_slab.json              item/deadhead_fence.json                    item/deadhead_plank_stairs.json             item/dermoid_cyst.json                      item/discfive.json
item/discfour.json                          item/discone.json                           item/discsix.json                           item/disctwo.json                           item/ecstasy_icon.json
item/enemy_of_enemy_icon.json               item/epitome_infestation_warp_diffuser.json  item/flesh_door.json                        item/flesh_fence.json                       item/flesh_slab.json
item/flesh_stairs.json                      item/flesh_trapdoor.json                    item/fog_nullifier_icon.json                item/frost_weathered_stone_slab.json        item/frost_weathered_stone_stairs.json
item/gore_ada_big.json                      item/gore_ada_flat.json                     item/gore_ada_small.json                    item/gore_fer_big.json                      item/gore_fer_flat.json
item/gore_fer_small.json                    item/gore_mar_big.json                      item/gore_mar_flat.json                     item/gore_mar_small.json                    item/gore_pri_big.json
item/gore_pri_flat.json                     item/gore_pri_small.json                    item/gore_pur_big.json                      item/gore_pur_flat.json                     item/gore_pur_small.json
item/gore_sim_big.json                      item/gore_sim_flat.json                     item/gore_sim_small.json                    item/goth_door.json                         item/goth_fence.json
item/goth_plank_slab.json                   item/goth_plank_wall.json                   item/goth_planks_stairs.json                item/goth_stem.json                         item/goth_trapdoor.json
item/gothshroom1.json                       item/guerilla_icon.json                     item/harlequinn_grass.json                  item/harleskinn_fence.json                  item/harleskinn_slab.json
item/harleskinn_stairs.json                 item/hellfire_chemical_warfare_icon.json    item/helm.json                              item/helm_sentient.json                     item/hirsute_hair.json
item/hunt_season_icon.json                  item/infested_cactus.json                   item/infested_door.json                     item/infested_fence.json                    item/infested_furnace.json
item/infested_leaves.json                   item/infested_pot.json                      item/infested_trapdoor.json                 item/infested_workbench.json                item/infestedbush_arc.json
item/infestedbush_flower1.json              item/infestedbush_grass1.json               item/infestedbush_infected.json             item/infestedbush_spine.json                item/infestedbush_vine.json
item/infestedore_co.json                    item/infestedore_dia.json                   item/infestedore_eme.json                   item/infestedore_gol.json                   item/infestedore_iro.json
item/infestedore_lap.json                   item/infestedore_red.json                   item/infestedore_un.json                    item/itemmobspawner_.json                   item/itemmobspawner_abobodies.json
item/itemmobspawner_abohead.json            item/itemmobspawner_alafha.json             item/itemmobspawner_anged.json              item/itemmobspawner_ata.json                item/itemmobspawner_bano.json
item/itemmobspawner_banoadapted.json        item/itemmobspawner_buthol.json             item/itemmobspawner_canra.json              item/itemmobspawner_canraadapted.json       item/itemmobspawner_cruxa.json
item/itemmobspawner_cruxb.json              item/itemmobspawner_dod.json                item/itemmobspawner_dodsii.json             item/itemmobspawner_dodsiii.json            item/itemmobspawner_dodsiv.json
item/itemmobspawner_done.json               item/itemmobspawner_dorpa.json              item/itemmobspawner_elvia.json              item/itemmobspawner_emana.json              item/itemmobspawner_emanaadapted.json
item/itemmobspawner_esor.json               item/itemmobspawner_ferbear.json            item/itemmobspawner_fercow.json             item/itemmobspawner_ferenderman.json        item/itemmobspawner_ferhorse.json
item/itemmobspawner_ferhuman.json           item/itemmobspawner_ferpig.json             item/itemmobspawner_fersheep.json           item/itemmobspawner_fervillager.json        item/itemmobspawner_ferwolf.json
item/itemmobspawner_flog.json               item/itemmobspawner_ganro.json              item/itemmobspawner_gim.json                item/itemmobspawner_gimadapted.json         item/itemmobspawner_gothol.json
item/itemmobspawner_heblu.json              item/itemmobspawner_heed.json               item/itemmobspawner_hiblaze.json            item/itemmobspawner_higolem.json            item/itemmobspawner_hiskeleton.json
item/itemmobspawner_host.json               item/itemmobspawner_hostii.json             item/itemmobspawner_hull.json               item/itemmobspawner_hulladapted.json        item/itemmobspawner_iki.json
item/itemmobspawner_ikiadapted.json         item/itemmobspawner_infbear.json            item/itemmobspawner_infcow.json             item/itemmobspawner_infcowhead.json         item/itemmobspawner_infdragone.json
item/itemmobspawner_infdragonehead.json     item/itemmobspawner_infenderman.json        item/itemmobspawner_infendermanhead.json    item/itemmobspawner_infhorse.json           item/itemmobspawner_infhorsehead.json
item/itemmobspawner_infhuman.json           item/itemmobspawner_infhumanhead.json       item/itemmobspawner_infpig.json             item/itemmobspawner_infpighead.json         item/itemmobspawner_infplayer.json
item/itemmobspawner_infplayerhead.json      item/itemmobspawner_infsheep.json           item/itemmobspawner_infsheephead.json       item/itemmobspawner_infsquid.json           item/itemmobspawner_infvillager.json
item/itemmobspawner_infvillagerhead.json    item/itemmobspawner_infwolf.json            item/itemmobspawner_infwolfhead.json        item/itemmobspawner_inhoom.json             item/itemmobspawner_inhoos.json
item/itemmobspawner_jinjo.json              item/itemmobspawner_kirin.json              item/itemmobspawner_leem.json               item/itemmobspawner_leemsii.json            item/itemmobspawner_leemsiii.json
item/itemmobspawner_leemsiv.json            item/itemmobspawner_leer.json               item/itemmobspawner_lencia.json             item/itemmobspawner_lesh.json               item/itemmobspawner_lodo.json
item/itemmobspawner_lum.json                item/itemmobspawner_lumadapted.json         item/itemmobspawner_mar.json                item/itemmobspawner_marbear.json            item/itemmobspawner_marcow.json
item/itemmobspawner_marenderman.json        item/itemmobspawner_marhuman.json           item/itemmobspawner_marsheep.json           item/itemmobspawner_marvillager.json        item/itemmobspawner_mes.json
item/itemmobspawner_mudo.json               item/itemmobspawner_nak.json                item/itemmobspawner_nogla.json              item/itemmobspawner_noglaadapted.json       item/itemmobspawner_nuuh.json
item/itemmobspawner_omboo.json              item/itemmobspawner_orch.json               item/itemmobspawner_oronco.json             item/itemmobspawner_pheon.json              item/itemmobspawner_pod.json
item/itemmobspawner_quac.json               item/itemmobspawner_ranrac.json             item/itemmobspawner_ranracadapted.json      item/itemmobspawner_rathol.json             item/itemmobspawner_shyco.json
item/itemmobspawner_shycoadapted.json       item/itemmobspawner_soo.json                item/itemmobspawner_tenn.json               item/itemmobspawner_terla.json              item/itemmobspawner_tonro.json
item/itemmobspawner_unvo.json               item/itemmobspawner_venkrol.json            item/itemmobspawner_venkrolsii.json         item/itemmobspawner_venkrolsiii.json        item/itemmobspawner_venkrolsiv.json
item/itemmobspawner_venkrolsv.json          item/itemmobspawner_vesta.json              item/itemmobspawner_wymo.json               item/itemmobspawner_wymoadapted.json        item/itemmobspawner_zaa.json
item/itemmobspawner_zaaadapted.json         item/itemtab.json                           item/lance.json                             item/lance_sentient.json                    item/lipoma_mass.json
item/locs_block_slab.json                   item/loot_common.json                       item/loot_rare.json                         item/loot_uncommon.json                     item/lure_eight.json
item/lure_five.json                         item/lure_four.json                         item/lure_nine.json                         item/lure_one.json                          item/lure_seven.json
item/lure_six.json                          item/lure_ten.json                          item/lure_three.json                        item/lure_two.json                          item/lurecomponent10.json
item/lurecomponent7.json                    item/lurecomponent8.json                    item/lurecomponent9.json                    item/maul.json                              item/maul_sentient.json
item/nodecompass_16.json                    item/pants.json                             item/pants_sentient.json                    item/parasite_barrier.json                  item/parasitebush_bine.json
item/parasitebush_decanter.json             item/parasitebush_decanterempty.json        item/parasitebush_eye.json                  item/parasitebush_frostg.json               item/parasitebush_frostgt.json
item/parasitebush_pop.json                  item/parasitebush_tendril.json              item/parasitebush_thorn.json                item/parasitebush_thorndead.json            item/parasitebush_thorndormat.json
item/parasitebush_thorndormats.json         item/parasitebush_thorntwo.json             item/parasitebush_thorntwos.json            item/parasitebush_tooh.json                 item/parasitecanister_bag_wall.json
item/parasiteplank_deadhead_wall.json       item/parasiterubble_bone.json               item/parasiterubble_bricks_wall.json        item/parasiterubble_flesh.json              item/parasiterubble_flesh_wall.json
item/parasiterubble_metal_wall.json         item/parasiterubble_stone.json              item/parasiterubble_weathb.json             item/parasiterubble_weathb_wall.json        item/parasiterubble_weathbc.json
item/parasiterubble_weathbc_wall.json       item/parasiterubble_weathfs.json            item/parasiterubble_weathfs_wall.json       item/parasiterubbledense.json               item/parasiterubbledense_biome.json
item/parasiterubbledense_biome_wall.json    item/parasiterubbledense_colony.json        item/parasiterubbledense_colony_wall.json   item/parasiterubbledense_heart.json         item/parasitesapling_flowertall.json
item/parasitesapling_tree.json              item/parasitesapling_treethin.json          item/parasitestain_dirt.json                item/parasitestain_feeler.json              item/parasitestain_flesh.json
item/parasitestain_flesh_wall.json          item/parasitestain_mud.json                 item/parasitestain_red.json                 item/parasitestain_sackflesh.json           item/parasitestain_spore.json
item/parasitetendril.json                   item/parasitethin.json                      item/parasitethin_treebase.json             item/parasitethin_treenesw.json             item/parasitetrunk.json
item/parasitetrunk_ball.json                item/parasitetrunk_plant.json               item/parasitic_colony_core_slab.json        item/parasitic_compressed_colony_stone_slab.json  item/plank_deadhead.json
item/plank_deadheads.json                   item/poland_skin_slab.json                  item/potion_columbus_icon.json              item/potion_stolas_icon.json                item/potted_assimilated_blossom.json
item/potted_consumed_assimilated_blossom.json  item/primitive_icon.json                    item/pure_icon.json                         item/reinforced_hivestone_slab.json         item/relaycontroller.json
item/roots_icon.json                        item/rubble_bone.json                       item/rubble_bricks.json                     item/rubble_flesh.json                      item/rubble_fungus.json
item/rubble_metal.json                      item/rubble_obsidian.json                   item/rubble_slabdouble.json                 item/rubble_stone.json                      item/rubble_stonedebris.json
item/rubble_weathb.json                     item/rubble_weathbc.json                    item/rubble_weathbcs.json                   item/rubble_weathbs.json                    item/rubble_weathfs.json
item/rubble_weathfss.json                   item/rubble_wood.json                       item/rubbledense_biome.json                 item/rubbledense_colony.json                item/rubbledense_wall.json
item/sac_of_flesh_slab.json                 item/sapling_consumed.json                  item/sapling_deadhead.json                  item/sapling_flowertall.json                item/sapling_infested.json
item/sapling_tree.json                      item/sapling_treethin.json                  item/scythe.json                            item/scythe_sentient.json                   item/self_destruct_icon.json
item/slab_.json                             item/slab_bone.json                         item/slab_bricks.json                       item/slab_dirt.json                         item/slab_feeler.json
item/slab_flesh.json                        item/slab_fungus.json                       item/slab_metal.json                        item/slab_mud.json                          item/slab_obsidian.json
item/slab_red.json                          item/slab_sackflesh.json                    item/slab_sflesh.json                       item/slab_spore.json                        item/slab_stone.json
item/slab_stonedebris.json                  item/slab_wood.json                         item/stain_dirt.json                        item/stain_feeler.json                      item/stain_flesh.json
item/stain_mud.json                         item/stain_red.json                         item/stain_sackflesh.json                   item/stain_spore.json                       item/sword.json
item/sword_sentient.json                    item/tresses_hair.json                      item/trophy_boom_orb_item.json              item/trophy_void_orb_item.json              item/trunk_ball.json
item/trunk_circle.json                      item/trunk_deadhead.json                    item/trunk_deadheads.json                   item/trunk_plant.json                       item/trunk_tree.json
item/weapon_dagger.json                     item/weapon_dagger_sentient.json            item/weathered_bricks_slab.json             item/weathered_cobblestone_slab.json        item/web_one.json
item/web_three.json                         item/web_two.json                           item/wheathered_bricks_stairs.json          item/wheathered_cobblestone_stairs.json     obj/arraytowerlarge.mtl
obj/arraytowerlarge.obj
```

### 语言文件：目标 4，捐赠 37，缺 ['de_at.lang', 'de_ch.lang', 'de_de.lang', 'en_pt.lang', 'en_us.lang', 'en_ws.lang', 'es_ar.lang', 'es_cl.lang', 'es_ec.lang', 'es_es.lang', 'es_mx.lang', 'es_uy.lang', 'es_ve.lang', 'fr_ca.lang', 'fr_fr.lang', 'hr_hr.lang', 'it_it.lang', 'ja_jp.lang', 'ja_jp21.lang', 'ko_kr.lang', 'lol_us.lang', 'lv_LV.lang', 'nl_nl.lang', 'pl_pl.lang', 'pt_br.lang', 'ro_ro.lang', 'ru_ru.lang', 'sv_se.lang', 'tr_tr.lang', 'uk_ua.lang', 'zh_cn.lang', 'zh_tw.lang', 'zh_tw21.lang']

