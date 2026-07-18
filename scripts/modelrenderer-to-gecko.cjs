const fs = require("fs");
const path = require("path");

if (process.argv.length < 6) {
  console.error("Usage: node scripts/modelrenderer-to-gecko.cjs <ModelRenderer.java> <output.geo.json> <identifier> <texture-width> [texture-height]");
  process.exit(1);
}

const [inputPath, outputPath, identifier, widthText, heightText = widthText] = process.argv.slice(2);
const source = fs.readFileSync(inputPath, "utf8");
const number = (value) => {
  const expression = value.trim().replace(/\((?:float|double)\)\s*/gu, "").replace(/[fFdD](?=\s*(?:[,)]|$))/gu, "");
  if (!/^(?:(?:[0-9.\s()+\-*/]+)|(?:Math\.PI))+$/u.test(expression)) {
    throw new Error(`Unsupported model number expression: ${value}`);
  }
  return Function(`"use strict"; return (${expression});`)();
};
const round = (value) => Math.round(value * 1_000_000) / 1_000_000;
const fields = new Map();
const parents = new Map();

const value = "([^,\\r\\n]+)";
const declaration = new RegExp(
  `this\\.(\\w+)\\s*=\\s*new ModelRenderer\\((?:\\(ModelBase\\))?this,\\s*(\\d+),\\s*(\\d+)\\);\\s*\\n\\s*`
  + `this\\.\\1\\.func_78793_a\\(${value},\\s*${value},\\s*${value}\\);\\s*\\n\\s*`
  + `this\\.\\1\\.func_78790_a\\(${value},\\s*${value},\\s*${value},\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*${value}\\);`
  + `(?:\\s*\\n\\s*this\\.setRotateAngle\\(this\\.\\1,\\s*${value},\\s*${value},\\s*${value}\\);)?`,
  "gu"
);
for (const match of source.matchAll(declaration)) {
  const [, name, u, v, pivotX, pivotY, pivotZ, boxX, boxY, boxZ, sizeX, sizeY, sizeZ, inflate,
    rotationX = "0", rotationY = "0", rotationZ = "0"] = match;
  fields.set(name, {
    name,
    uv: [Number(u), Number(v)],
    pivot: [number(pivotX), number(pivotY), number(pivotZ)],
    box: [number(boxX), number(boxY), number(boxZ), Number(sizeX), Number(sizeY), Number(sizeZ), number(inflate)],
    rotation: [number(rotationX), number(rotationY), number(rotationZ)]
  });
}

for (const match of source.matchAll(/this\.(\w+)\.func_78792_a\(this\.(\w+)\);/gu)) {
  parents.set(match[2], match[1]);
}

if (!fields.size) {
  throw new Error(`No ModelRenderer declarations were parsed from ${inputPath}`);
}

const globalPivots = new Map();
function globalPivot(name, visiting = new Set()) {
  if (globalPivots.has(name)) return globalPivots.get(name);
  if (visiting.has(name)) throw new Error(`Model hierarchy cycle at ${name}`);
  const field = fields.get(name);
  if (!field) throw new Error(`Missing model field ${name}`);
  visiting.add(name);
  const parent = parents.get(name);
  const base = parent && fields.has(parent) ? globalPivot(parent, visiting) : [0, 0, 0];
  const result = field.pivot.map((value, index) => value + base[index]);
  visiting.delete(name);
  globalPivots.set(name, result);
  return result;
}

const children = new Map();
for (const [child, parent] of parents) {
  if (!fields.has(parent)) continue;
  const list = children.get(parent) ?? [];
  list.push(child);
  children.set(parent, list);
}

const ordered = [];
const emitted = new Set();
function visit(name) {
  if (emitted.has(name) || !fields.has(name)) return;
  emitted.add(name);
  ordered.push(name);
  for (const child of children.get(name) ?? []) visit(child);
}

for (const name of fields.keys()) {
  if (!parents.has(name) || !fields.has(parents.get(name))) visit(name);
}
for (const name of fields.keys()) visit(name);

const radiansToDegrees = (value) => round(value * 180 / Math.PI);
const bones = ordered.map((name) => {
  const field = fields.get(name);
  const pivot = globalPivot(name);
  const [boxX, boxY, boxZ, sizeX, sizeY, sizeZ, inflate] = field.box;
  const bone = {
    name,
    pivot: [round(pivot[0]), round(24 - pivot[1]), round(pivot[2])]
  };
  const parent = parents.get(name);
  if (parent && fields.has(parent)) bone.parent = parent;
  const rotation = field.rotation.map(radiansToDegrees);
  if (rotation.some((value) => value !== 0)) bone.rotation = rotation;
  const cube = {
    origin: [round(pivot[0] + boxX), round(24 - (pivot[1] + boxY + sizeY)), round(pivot[2] + boxZ)],
    size: [sizeX, sizeY, sizeZ],
    uv: field.uv
  };
  if (inflate !== 0) cube.inflate = inflate;
  bone.cubes = [cube];
  return bone;
});

const geometry = {
  format_version: "1.12.0",
  "minecraft:geometry": [{
    description: {
      identifier: `geometry.${identifier}`,
      texture_width: Number(widthText),
      texture_height: Number(heightText),
      visible_bounds_width: 5,
      visible_bounds_height: 5,
      visible_bounds_offset: [0, 2, 0]
    },
    bones
  }]
};

fs.mkdirSync(path.dirname(outputPath), {recursive: true});
fs.writeFileSync(outputPath, `${JSON.stringify(geometry, null, 2)}\n`);
console.log(`Converted ${fields.size} ModelRenderer parts into ${bones.length} GeckoLib bones: ${outputPath}`);
