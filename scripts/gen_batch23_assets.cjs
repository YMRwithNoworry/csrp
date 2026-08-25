// 批次2+3 资源生成器：残骸体系 + 木系建材
const fs = require('fs');
const path = require('path');
const ORIG = 'D:/code/模组反编译器/decompiled/[逃逸：寄生体] SRParasites-1.10.8/assets/srparasites/textures/blocks';
const RES = 'D:/code/MC模组/csrp/src/main/resources';
const A = RES + '/assets/csrp';
const D = RES + '/data/csrp';

// type: cube | column | cross | fence | door | trapdoor | workbench
// srcTex: 从原版拷贝的贴图 {目标名: 源名}；useTex: 复用本项目已有贴图（不拷贝）
const M = [
  // ---- 批次2 残骸 ----
  ...['bone','flesh','stone','weathb','weathbc','weathfs'].map(v => ({
    id: `parasiterubble_${v}`, type: 'cube', zh: {bone:'骨质寄生残骸',flesh:'血肉寄生残骸',stone:'石质寄生残骸',weathb:'风化石砖残骸',weathbc:'风化圆石残骸',weathfs:'风化石岩残骸'}[v],
    en: `Parasite Rubble (${v})`, srcTex: { [`parasiterubble_${v}`]: `parasiterubble_${v}.png` } })),
  ...['','_biome','_colony','_heart'].map(v => ({
    id: `parasiterubbledense${v}`, type: 'cube', zh: {'':'致密寄生残骸','_biome':'群系致密残骸','_colony':'殖民地致密残骸','_heart':'核心致密残骸'}[v] || '致密寄生残骸',
    en: `Dense Parasite Rubble${v}`, srcTex: { [`parasiterubbledense${v}`]: `parasiterubbledense${v === '' ? '_wall' : v}.png` } })),
  ...['flesh','dirt','mud','feeler'].map(v => ({
    id: `parasitestain_${v}`, type: 'cube', zh: {flesh:'血肉寄生污渍',dirt:'泥土寄生污渍',mud:'泥沼寄生污渍',feeler:'触须寄生污渍'}[v],
    en: `Parasite Stain (${v})`, srcTex: { [`parasitestain_${v}`]: `parasitestain_${v}.png` } })),
  { id: 'parasitetrunk', type: 'column', zh: '寄生树干', en: 'Parasite Trunk', srcTex: { parasitetrunk: 'parasitetrunk_tree.png', parasitetrunk_top: 'parasitetrunk_tree_side.png' } },
  { id: 'parasitetrunk_ball', type: 'column', zh: '寄生瘤节树干', en: 'Parasite Bole Trunk', srcTex: { parasitetrunk_ball: 'parasitetrunk_ball.png', parasitetrunk_ball_top: 'parasitetrunk_ball_side.png' } },
  { id: 'parasitetrunk_plant', type: 'column', zh: '寄生株茎', en: 'Parasite Stem Trunk', srcTex: { parasitetrunk_plant: 'parasitetrunk_plant.png', parasitetrunk_plant_top: 'parasitetrunk_plant_side.png' } },
  { id: 'parasitethin_treebase', type: 'cube', zh: '寄生细干基座', en: 'Parasite Thin Base', srcTex: { parasitethin_treebase: 'parasitethin_treebase.png' } },
  { id: 'parasitethin_treenesw', type: 'cube', zh: '寄生细干', en: 'Parasite Thin Trunk', srcTex: { parasitethin_treenesw: 'parasitethin_treenesw.png' } },
  ...['tree','treethin','flowertall'].map(v => ({
    id: `parasitesapling_${v}`, type: 'cross', zh: {tree:'寄生树苗',treethin:'寄生细树苗',flowertall:'寄生高花苗'}[v],
    en: `Parasite Sapling (${v})`, srcTex: { [`parasitesapling_${v}`]: `parasitesapling_${v}.png` } })),
  { id: 'goth_stem', type: 'column', zh: '戈斯茎干', en: 'Goth Stem', srcTex: { goth_stem: 'goth_stem.png', goth_stem_top: 'goth_stem_top.png' } },
  { id: 'infested_workbench', type: 'workbench', zh: '感染工作台', en: 'Infested Workbench', srcTex: { infested_workbench_top: 'infested_crafting_table_top.png', infested_workbench_side: 'infested_crafting_table_side.png', infested_workbench_front: 'infested_crafting_table_front.png' } },
  { id: 'consumed_workbench', type: 'workbench', zh: '吞蚀工作台', en: 'Consumed Workbench', srcTex: { consumed_workbench_top: 'consumed_crafting_table_top.png', consumed_workbench_side: 'consumed_crafting_table_side.png', consumed_workbench_front: 'consumed_crafting_table_front.png' } },
  // ---- 批次3 门 ----
  ...[ ['goth','goth','戈斯'],['brusewood','bruisewood','青肿木'],['consumed','consumed','吞蚀'],['infested','infested','感染'],['flesh','flesh','血肉'],['cooked_flesh','cookedflesh','熟血肉'] ].map(([id, tex, zh]) => ({
    id: `${id}_door`, type: 'door', zh: `${zh}门`, en: `${id.replace('_',' ')} door`, srcTex: { [`${id}_door_top`]: `${tex}_door_top.png`, [`${id}_door_bottom`]: `${tex}_door_bottom.png` } })),
  // ---- 活板门 ----
  ...[ ['goth','goth','戈斯'],['brusewood','brusewood','青肿木'],['consumed','consumed','吞蚀'],['infested','infested','感染'],['flesh','flesh','血肉'],['cooked_flesh','cookedflesh','熟血肉'] ].map(([id, tex, zh]) => ({
    id: `${id}_trapdoor`, type: 'trapdoor', zh: `${zh}活板门`, en: `${id.replace('_',' ')} trapdoor`, srcTex: { [`${id}_trapdoor`]: `${tex}_trapdoor.png` } })),
  // ---- 栅栏（复用木板贴图） ----
  { id: 'goth_fence', type: 'fence', zh: '戈斯木栅栏', en: 'Goth Fence', useTex: { all: 'csrp:block/goth_planks' } },
  { id: 'infested_fence', type: 'fence', zh: '感染木栅栏', en: 'Infested Fence', useTex: { all: 'csrp:block/infested_planks' } },
  { id: 'consumed_fence', type: 'fence', zh: '吞蚀木栅栏', en: 'Consumed Fence', useTex: { all: 'csrp:block/consumed_planks' } },
  { id: 'flesh_fence', type: 'fence', zh: '血肉栅栏', en: 'Flesh Fence', useTex: { all: 'csrp:block/flesh_planks' } },
  { id: 'deadhead_fence', type: 'fence', zh: '枯头木栅栏', en: 'Deadhead Fence', useTex: { all: 'csrp:block/parasiteplank_deadhead' } },
];

const W = (p, obj) => { fs.mkdirSync(path.dirname(p), { recursive: true }); fs.writeFileSync(p, JSON.stringify(obj, null, 2) + '\n'); };
const jsonLines = [];
for (const m of M) {
  const bs = `${A}/blockstates/${m.id}.json`, bm = `${A}/models/block`, im = `${A}/models/item`, lt = `${D}/loot_table/blocks/${m.id}.json`;
  // 贴图拷贝
  if (m.srcTex) for (const [dst, src] of Object.entries(m.srcTex)) fs.copyFileSync(`${ORIG}/${src}`, `${A}/textures/block/${dst}.png`);
  const loot = { type: 'minecraft:block', pools: [{ rolls: 1, bonus_rolls: 0, entries: [{ type: 'minecraft:item', name: `csrp:${m.id}` }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] };
  if (m.type === 'cube' || m.type === 'workbench') {
    W(bs, { variants: m.type === 'workbench'
      ? { facing_north: { model: `csrp:block/${m.id}` }, facing_east: { model: `csrp:block/${m.id}`, y: 90 }, facing_south: { model: `csrp:block/${m.id}`, y: 180 }, facing_west: { model: `csrp:block/${m.id}`, y: 270 } }
      : { '': { model: `csrp:block/${m.id}` } } });
    W(`${bm}/${m.id}.json`, m.type === 'workbench'
      ? { parent: 'minecraft:block/orientable', textures: { top: `csrp:block/${m.id}_top`, side: `csrp:block/${m.id}_side`, front: `csrp:block/${m.id}_front` } }
      : { parent: 'minecraft:block/cube_all', textures: { all: `csrp:block/${m.id}` } });
    W(`${im}/${m.id}.json`, { parent: `csrp:block/${m.id}` });
    W(lt, loot);
  } else if (m.type === 'column') {
    W(bs, { variants: { 'axis=x': { model: `csrp:block/${m.id}`, x: 90, y: 90 }, 'axis=y': { model: `csrp:block/${m.id}` }, 'axis=z': { model: `csrp:block/${m.id}`, x: 90 } } });
    W(`${bm}/${m.id}.json`, { parent: 'minecraft:block/cube_column', textures: { end: `csrp:block/${m.id}_top`, side: `csrp:block/${m.id}` } });
    W(`${im}/${m.id}.json`, { parent: `csrp:block/${m.id}` });
    W(lt, loot);
  } else if (m.type === 'cross') {
    W(bs, { variants: { '': { model: `csrp:block/${m.id}` } } });
    W(`${bm}/${m.id}.json`, { parent: 'minecraft:block/cross', textures: { cross: `csrp:block/${m.id}` } });
    W(`${im}/${m.id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `csrp:block/${m.id}` } });
    W(lt, loot);
  } else if (m.type === 'fence') {
    const tex = m.useTex.all;
    W(`${bm}/${m.id}_post.json`, { parent: 'minecraft:block/fence_post', textures: { texture: tex } });
    W(`${bm}/${m.id}_side.json`, { parent: 'minecraft:block/fence_side', textures: { texture: tex } });
    W(bs, { multipart: [
      { apply: { model: `csrp:block/${m.id}_post` } },
      { when: { north: 'true' }, apply: { model: `csrp:block/${m.id}_side`, uvlock: true } },
      { when: { east: 'true' }, apply: { model: `csrp:block/${m.id}_side`, y: 90, uvlock: true } },
      { when: { south: 'true' }, apply: { model: `csrp:block/${m.id}_side`, y: 180, uvlock: true } },
      { when: { west: 'true' }, apply: { model: `csrp:block/${m.id}_side`, y: 270, uvlock: true } } ] });
    W(`${im}/${m.id}.json`, { parent: `csrp:block/${m.id}_post` });
    W(lt, loot);
  } else if (m.type === 'trapdoor') {
    W(`${bm}/${m.id}_bottom.json`, { parent: 'minecraft:block/template_trapdoor_bottom', textures: { texture: `csrp:block/${m.id}` } });
    W(`${bm}/${m.id}_top.json`, { parent: 'minecraft:block/template_trapdoor_top', textures: { texture: `csrp:block/${m.id}` } });
    W(`${bm}/${m.id}_open.json`, { parent: 'minecraft:block/template_trapdoor_open', textures: { texture: `csrp:block/${m.id}` } });
    const v = {};
    for (const half of ['bottom', 'top']) for (const open of ['false', 'true']) for (const face of ['north', 'east', 'south', 'west']) {
      const rot = { north: 0, east: 90, south: 180, west: 270 }[face];
      v[`${half},open=${open},facing=${face}`] = open === 'true'
        ? { model: `csrp:block/${m.id}_open`, y: rot }
        : { model: `csrp:block/${m.id}_${half}`, y: rot };
    }
    W(bs, { variants: v });
    W(`${im}/${m.id}.json`, { parent: `csrp:block/${m.id}_bottom` });
    W(lt, loot);
  } else if (m.type === 'door') {
    for (const part of ['bottom', 'top']) for (const hinge of ['left', 'right']) {
      W(`${bm}/${m.id}_${part}_${hinge}.json`, { parent: `minecraft:block/door_${part}_${hinge}`, textures: { top: `csrp:block/${m.id}_top`, bottom: `csrp:block/${m.id}_bottom` } });
      W(`${bm}/${m.id}_${part}_${hinge}_open.json`, { parent: `minecraft:block/door_${part}_${hinge}_open`, textures: { top: `csrp:block/${m.id}_top`, bottom: `csrp:block/${m.id}_bottom` } });
    }
    const v = {};
    for (const half of ['lower', 'upper']) for (const hinge of ['left', 'right']) for (const open of ['false', 'true']) for (const powered of ['false', 'true']) for (const face of ['east', 'south', 'west', 'north']) {
      const rot = { east: 90, south: 180, west: 270, north: 0 }[face];
      v[`${half},facing=${face},open=${open},hinge=${hinge},powered=${powered}`] = {
        model: `csrp:block/${m.id}_${half === 'lower' ? 'bottom' : 'top'}_${hinge}${open === 'true' ? '_open' : ''}`, y: rot + (open === 'true' ? { left: 90, right: -90 }[hinge] : 0) };
    }
    W(bs, { variants: v });
    W(`${im}/${m.id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `csrp:block/${m.id}_top` } });
    W(lt, loot);
  }
  jsonLines.push(m);
}
// 语言文件追加
for (const [file, key] of [['zh_cn.json', 'zh'], ['en_us.json', 'en']]) {
  const p = `${A}/lang/${file}`;
  const lang = JSON.parse(fs.readFileSync(p, 'utf8'));
  for (const m of M) lang[`block.csrp.${m.id}`] = key === 'zh' ? m.zh : m.en.replace(/\b\w/g, c => c.toUpperCase());
  fs.writeFileSync(p, JSON.stringify(lang, null, 2) + '\n');
}
// 输出 Java 注册行供粘贴
const props = t => ({
  cube: 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)',
  column: 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)',
  cross: 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)',
  fence: 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)',
  door: 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()',
  trapdoor: 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)',
  workbench: 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.5F).sound(SoundType.WOOD)',
}[t]);
const cls = t => ({ cube: 'Block', column: 'Block', cross: 'Block', fence: 'FenceBlock', door: 'DoorBlock', trapdoor: 'TrapDoorBlock', workbench: 'CraftingTableBlock' }[t]);
const extra = t => ({ door: 'BlockSetType.OAK, ', trapdoor: 'BlockSetType.OAK, ', fence: '', cube: '', column: '', cross: '', workbench: '' }[t]);
console.log('==== ModBlocks 注册行 ====');
for (const m of M) console.log(`    public static final DeferredBlock<${cls(m.type)}> ${m.id.toUpperCase()} = BLOCKS.register("${m.id}", () -> new ${cls(m.type)}(${extra(m.type)}${props(m.type)}));`);
console.log('==== ModItems 注册行 ====');
for (const m of M) console.log(`    public static final DeferredItem<BlockItem> ${m.id.toUpperCase()} = ITEMS.registerSimpleBlockItem("${m.id}", ModBlocks.${m.id.toUpperCase()});`);
console.log(`共 ${M.length} 个方块资源生成完毕`);
